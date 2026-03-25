import asyncio
import logging
from app.core.celery_app import celery_app
from app.config import settings

logger = logging.getLogger(__name__)


def _run_async(coro):
    loop = asyncio.new_event_loop()
    asyncio.set_event_loop(loop)
    try:
        return loop.run_until_complete(coro)
    finally:
        loop.close()


@celery_app.task(name="app.tasks.paper_tasks.fetch_papers_for_all_users")
def fetch_papers_for_all_users():
    """Fetch papers from arXiv + PubMed for all users and populate feeds."""
    _run_async(_async_fetch_papers())


async def _async_fetch_papers():
    from sqlalchemy import select
    from app.core.database import AsyncSessionLocal
    from app.models.users import User
    from app.models.research_profiles import ResearchProfile
    from app.models.user_feeds import UserFeed
    from app.models.alerts import Alert
    from app.repositories.paper_repository import upsert_paper
    from app.services.ai.embedding import generate_paper_embedding
    from app.services.ai.relevance import score_paper, apply_boosts, classify_priority
    from app.external import arxiv_client, pubmed_client

    async with AsyncSessionLocal() as db:
        result = await db.execute(
            select(User).join(ResearchProfile).where(User.is_active == True)
        )
        users = result.scalars().all()
        logger.info("Fetching papers for %d users", len(users))

        for user in users:
            profile_result = await db.execute(
                select(ResearchProfile).where(ResearchProfile.user_id == user.id)
            )
            profile = profile_result.scalar_one_or_none()
            if not profile or not profile.keywords:
                continue

            all_papers = []
            all_papers.extend(arxiv_client.fetch_papers(profile.keywords))
            all_papers.extend(pubmed_client.fetch_papers(profile.keywords))

            added = 0
            alerts_created = 0
            for paper_data in all_papers:
                embedding = generate_paper_embedding(
                    paper_data.title, paper_data.abstract or ""
                )
                paper = await upsert_paper(db, paper_data, embedding)

                profile_vec = profile.get_embedding()
                if not profile_vec:
                    continue

                base_score = score_paper(profile_vec, embedding)
                final_score = apply_boosts(base_score, paper, profile, set())
                priority = classify_priority(final_score)
                if priority is None:
                    continue

                existing = await db.execute(
                    select(UserFeed).where(
                        UserFeed.user_id == user.id,
                        UserFeed.paper_id == paper.id,
                    )
                )
                if existing.scalar_one_or_none():
                    continue

                feed_item = UserFeed(
                    user_id=user.id,
                    paper_id=paper.id,
                    relevance_score=final_score,
                    priority=priority,
                )
                db.add(feed_item)
                added += 1

                # Create a new_paper alert for high/medium priority papers
                if priority in ("high", "medium"):
                    existing_alert = await db.execute(
                        select(Alert).where(
                            Alert.user_id == user.id,
                            Alert.paper_id == paper.id,
                            Alert.type == "new_paper",
                        )
                    )
                    if not existing_alert.scalar_one_or_none():
                        matched_topic = _match_topic(
                            paper_title=paper.title,
                            paper_abstract=paper.abstract or "",
                            user_topics=profile.topics or [],
                        )
                        topic_label = matched_topic or "your research interests"
                        alert = Alert(
                            user_id=user.id,
                            paper_id=paper.id,
                            type="new_paper",
                            title=f"New paper on {topic_label}",
                            description=(
                                f'A new paper matching your "{topic_label}" interest '
                                f"has been published: {paper.title}"
                            ),
                            similarity_score=final_score,
                        )
                        db.add(alert)
                        alerts_created += 1

                # Create alert when a followed author publishes a new paper
                followed_authors = profile.authors_following or []
                if followed_authors and paper.authors:
                    matched_author = _match_followed_author(
                        paper.authors, followed_authors
                    )
                    if matched_author:
                        existing_follow_alert = await db.execute(
                            select(Alert).where(
                                Alert.user_id == user.id,
                                Alert.paper_id == paper.id,
                                Alert.type == "followed_author",
                            )
                        )
                        if not existing_follow_alert.scalar_one_or_none():
                            alert = Alert(
                                user_id=user.id,
                                paper_id=paper.id,
                                type="followed_author",
                                title=f"New paper by {matched_author}",
                                description=(
                                    f"{matched_author}, a researcher you follow, "
                                    f"has published a new paper: {paper.title}"
                                ),
                                similarity_score=final_score,
                            )
                            db.add(alert)
                            alerts_created += 1

            await db.commit()
            logger.info(
                "User %s: added %d feed items, %d new-paper alerts",
                user.id, added, alerts_created,
            )


def _match_topic(paper_title: str, paper_abstract: str, user_topics: list[str]) -> str | None:
    """Return the first user topic that appears in the paper title or abstract."""
    text = (paper_title + " " + paper_abstract).lower()
    for topic in user_topics:
        if topic.lower() in text:
            return topic
    return user_topics[0] if user_topics else None


def _match_followed_author(
    paper_authors: list[str], followed_authors: list[str]
) -> str | None:
    """Return the first followed author that matches any paper author (case-insensitive)."""
    followed_lower = {a.lower(): a for a in followed_authors}
    for author in paper_authors:
        key = author.lower().strip()
        if key in followed_lower:
            return followed_lower[key]
    return None


@celery_app.task(name="app.tasks.paper_tasks.pre_generate_summaries")
def pre_generate_summaries():
    """Pre-generate summaries for high-priority papers without one."""
    _run_async(_async_pre_generate_summaries())


async def _async_pre_generate_summaries():
    from sqlalchemy import select
    from app.core.database import AsyncSessionLocal
    from app.models.user_feeds import UserFeed
    from app.models.papers import Paper
    from app.models.paper_summaries import PaperSummary
    from app.services.ai.summarization import generate_paper_summary
    from datetime import datetime, timezone

    async with AsyncSessionLocal() as db:
        result = await db.execute(
            select(Paper)
            .join(UserFeed)
            .outerjoin(PaperSummary)
            .where(
                UserFeed.priority == "high",
                PaperSummary.id.is_(None),
            )
            .limit(50)
        )
        papers = result.scalars().unique().all()

        for paper in papers:
            summary_data = await generate_paper_summary(
                paper.title, paper.authors or [], paper.abstract or ""
            )
            if summary_data:
                summary = PaperSummary(
                    paper_id=paper.id,
                    purpose=summary_data.get("purpose"),
                    methodology=summary_data.get("methodology"),
                    key_results=summary_data.get("key_results"),
                    limitations=summary_data.get("limitations"),
                    relevance_to_field=summary_data.get("relevance_to_field"),
                    model_version=settings.OLLAMA_SUMMARY_MODEL,
                    status="ready",
                    generated_at=datetime.now(timezone.utc),
                )
            else:
                summary = PaperSummary(
                    paper_id=paper.id,
                    status="failed",
                    failed_at=datetime.now(timezone.utc),
                )
            db.add(summary)

        await db.commit()
        logger.info("Pre-generated summaries for %d papers", len(papers))

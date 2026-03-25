import json
import logging
from fastapi import APIRouter, Depends, Query, Response
from fastapi.responses import JSONResponse
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, func
from sqlalchemy.orm import selectinload
from app.core.database import get_db
from app.core.security import get_current_user
from app.core.cache import get_cached, set_cached, invalidate_pattern
from app.models.users import User
from app.models.user_feeds import UserFeed
from app.models.papers import Paper
from app.models.research_profiles import ResearchProfile
from app.schemas.feed import FeedResponse, FeedItem, FeedPaper
from app.repositories.paper_repository import upsert_paper
from app.services.ai.embedding import generate_paper_embedding
from app.services.ai.relevance import score_paper, apply_boosts, classify_priority
from app.external import arxiv_client, pubmed_client

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/feed", tags=["feed"])

FEED_CACHE_TTL = 3600  # 1 hour


def _make_feed_item(feed: UserFeed) -> FeedItem:
    p = feed.paper
    return FeedItem(
        id=str(feed.id),
        paper_id=str(feed.paper_id),
        relevance_score=feed.relevance_score,
        priority=feed.priority,
        is_read=feed.is_read,
        is_saved=feed.is_saved,
        created_at=feed.created_at,
        paper=FeedPaper(
            id=str(p.id),
            external_id=p.external_id,
            source=p.source,
            title=p.title,
            authors=p.authors or [],
            abstract=p.abstract,
            published_date=p.published_date,
            pdf_url=p.pdf_url,
            citation_count=p.citation_count or 0,
            venue=p.venue,
        ),
    )


@router.get("/", response_model=FeedResponse)
async def get_feed(
    limit: int = Query(20, le=100),
    offset: int = Query(0, ge=0),
    filter: str = Query("all"),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    cache_key = f"feed:{current_user.id}:{filter}:{offset}:{limit}"
    cached = await get_cached(cache_key)
    if cached:
        return FeedResponse(**json.loads(cached))

    # Check if user has ANY feed items at all
    total_all = (await db.execute(
        select(func.count()).select_from(
            select(UserFeed).where(UserFeed.user_id == current_user.id).subquery()
        )
    )).scalar_one()

    # Auto-populate: if user has zero feed items, ensure profile exists and trigger refresh
    if total_all == 0 and offset == 0:
        # Ensure user has a profile — create a default one if missing
        prof_result = await db.execute(
            select(ResearchProfile).where(ResearchProfile.user_id == current_user.id)
        )
        if not prof_result.scalar_one_or_none():
            logger.info("User %s has no profile — creating default profile", current_user.id)
            default_profile = ResearchProfile(
                user_id=current_user.id,
                research_field="General Research",
                topics=["Machine Learning", "Data Science"],
                keywords=["artificial intelligence", "machine learning"],
                authors_following=[],
            )
            try:
                from app.services.ai.embedding import generate_profile_embedding
                default_profile.set_embedding(generate_profile_embedding(
                    default_profile.research_field,
                    default_profile.topics,
                    default_profile.keywords,
                ))
            except Exception:
                pass  # embedding is optional — refresh_feed handles missing embeddings
            db.add(default_profile)
            await db.commit()

        logger.info("User %s has 0 feed items — auto-triggering refresh", current_user.id)
        try:
            await refresh_feed(max_papers=20, db=db, current_user=current_user)
        except Exception as e:
            logger.warning("Auto-refresh failed for user %s: %s", current_user.id, e)

    query = (
        select(UserFeed)
        .join(Paper)
        .where(UserFeed.user_id == current_user.id)
        .options(selectinload(UserFeed.paper))
    )

    if filter == "high_priority":
        query = query.where(UserFeed.priority == "high")
    elif filter == "unread":
        query = query.where(UserFeed.is_read == False)

    count_q = select(func.count()).select_from(query.subquery())
    total = (await db.execute(count_q)).scalar_one()

    query = query.order_by(UserFeed.relevance_score.desc()).offset(offset).limit(limit)
    result = await db.execute(query)
    feeds = result.scalars().all()

    items = [_make_feed_item(f) for f in feeds]
    response = FeedResponse(items=items, total=total, has_more=(offset + limit) < total)

    await set_cached(cache_key, response.model_dump_json(), FEED_CACHE_TTL)
    return response


@router.post("/{paper_id}/dismiss", status_code=204)
async def dismiss_paper(
    paper_id: str,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    result = await db.execute(
        select(UserFeed).where(
            UserFeed.user_id == current_user.id,
            UserFeed.paper_id == paper_id,
        )
    )
    feed = result.scalar_one_or_none()
    if feed:
        await db.delete(feed)
        await db.commit()
        await invalidate_pattern(f"feed:{current_user.id}:*")
    return Response(status_code=204)


@router.post("/{paper_id}/mark-read", status_code=204)
async def mark_read(
    paper_id: str,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    result = await db.execute(
        select(UserFeed).where(
            UserFeed.user_id == current_user.id,
            UserFeed.paper_id == paper_id,
        )
    )
    feed = result.scalar_one_or_none()
    if feed:
        feed.is_read = True
        await db.commit()
        await invalidate_pattern(f"feed:{current_user.id}:*")

    # Record reading event for streak tracking (best-effort)
    try:
        from app.api.v1.endpoints.streaks import record_read as _record_read
        await _record_read(paper_id=paper_id, db=db, current_user=current_user)
    except Exception:
        pass

    return Response(status_code=204)


@router.post("/refresh", status_code=200)
async def refresh_feed(
    max_papers: int = Query(20, le=50),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """
    On-demand feed generation: fetches papers from arXiv + PubMed
    based on user's profile keywords, scores them, and populates the feed.
    Falls back to adding papers without scoring if embedding services fail.
    """
    logger.info("Feed refresh requested by user %s", current_user.id)
    profile_result = await db.execute(
        select(ResearchProfile).where(ResearchProfile.user_id == current_user.id)
    )
    profile = profile_result.scalar_one_or_none()
    if not profile:
        return JSONResponse(
            status_code=200,
            content={"added": 0, "message": "No profile found. Set up your profile first."},
        )

    # Use keywords; fall back to topics if keywords are empty
    search_keywords = profile.keywords or profile.topics or []
    if not search_keywords:
        return JSONResponse(
            status_code=200,
            content={"added": 0, "message": "No keywords or topics found. Update your profile with research interests."},
        )

    profile_vec = profile.get_embedding()
    if not profile_vec:
        logger.warning("Profile embedding missing for user %s — will add papers without scoring", current_user.id)

    # Fetch papers from external sources
    all_papers = []
    fetch_errors = []
    try:
        arxiv_papers = arxiv_client.fetch_papers(search_keywords, max_results=max_papers)
        all_papers.extend(arxiv_papers)
        logger.info("arXiv returned %d papers for keywords %s", len(arxiv_papers), search_keywords)
    except Exception as e:
        logger.warning("arXiv fetch failed: %s", e)
        fetch_errors.append(f"arXiv: {e}")
    try:
        pubmed_papers = pubmed_client.fetch_papers(search_keywords, max_results=max_papers)
        all_papers.extend(pubmed_papers)
        logger.info("PubMed returned %d papers for keywords %s", len(pubmed_papers), search_keywords)
    except Exception as e:
        logger.warning("PubMed fetch failed: %s", e)
        fetch_errors.append(f"PubMed: {e}")

    if not all_papers:
        msg = "No papers found for your keywords."
        if fetch_errors:
            msg += f" Errors: {'; '.join(fetch_errors)}"
        return JSONResponse(status_code=200, content={"added": 0, "message": msg})

    added = 0
    skipped = 0
    embedding_failures = 0
    for paper_data in all_papers:
        try:
            # Try to generate embedding and score; fall back to defaults if it fails
            embedding = None
            final_score = 0.5
            priority = "medium"

            try:
                embedding = generate_paper_embedding(paper_data.title, paper_data.abstract or "")
            except Exception as emb_err:
                embedding_failures += 1
                logger.warning("Embedding failed for '%s': %s — adding with default priority",
                               paper_data.title[:60], emb_err)

            paper = await upsert_paper(db, paper_data, embedding or [])

            try:
                if profile_vec and embedding:
                    base_score = score_paper(profile_vec, embedding)
                    final_score = apply_boosts(base_score, paper, profile, set())
                    scored_priority = classify_priority(final_score)
                    if scored_priority is not None:
                        priority = scored_priority
                    else:
                        skipped += 1
                        continue
            except Exception as score_err:
                logger.warning("Scoring failed for '%s': %s — using default priority",
                               paper_data.title[:60], score_err)

            # Skip if already in feed
            existing = await db.execute(
                select(UserFeed).where(
                    UserFeed.user_id == current_user.id,
                    UserFeed.paper_id == paper.id,
                )
            )
            if existing.scalar_one_or_none():
                continue

            feed_item = UserFeed(
                user_id=current_user.id,
                paper_id=paper.id,
                relevance_score=final_score,
                priority=priority,
            )
            db.add(feed_item)
            added += 1
            logger.info("Paper '%s' score=%.4f priority=%s", paper_data.title[:60], final_score, priority)
        except Exception as e:
            logger.warning("Failed to process paper %s: %s", paper_data.external_id, e)
            continue

    await db.commit()
    await invalidate_pattern(f"feed:{current_user.id}:*")
    logger.info("User %s: added %d, skipped %d, embedding_failures %d via refresh",
                current_user.id, added, skipped, embedding_failures)

    msg = f"Added {added} papers to your feed."
    if embedding_failures > 0:
        msg += f" ({embedding_failures} papers added without relevance scoring)"
    return {"added": added, "message": msg}

import asyncio
import logging
from app.core.celery_app import celery_app

logger = logging.getLogger(__name__)


def _run_async(coro):
    loop = asyncio.new_event_loop()
    asyncio.set_event_loop(loop)
    try:
        return loop.run_until_complete(coro)
    finally:
        loop.close()


@celery_app.task(name="app.tasks.overlap_tasks.detect_overlaps_for_all_users")
def detect_overlaps_for_all_users():
    _run_async(_async_detect_overlaps())


async def _async_detect_overlaps():
    from datetime import datetime, timedelta, timezone, date
    from sqlalchemy import select
    from app.core.database import AsyncSessionLocal
    from app.models.users import User
    from app.models.current_projects import CurrentProject
    from app.models.papers import Paper
    from app.models.alerts import Alert
    from app.repositories.vector_repository import find_similar_papers
    from app.services.ai.overlap import generate_overlap_report

    async with AsyncSessionLocal() as db:
        result = await db.execute(
            select(User).join(CurrentProject).where(User.is_active == True)
        )
        users = result.scalars().all()
        since = date.today() - timedelta(days=1)

        for user in users:
            project_result = await db.execute(
                select(CurrentProject).where(CurrentProject.user_id == user.id)
            )
            project = project_result.scalar_one_or_none()
            if not project:
                continue
            project_vec = project.get_embedding()
            if not project_vec:
                continue

            similar = await find_similar_papers(db, project_vec, limit=50, since_date=since)

            for paper, similarity in similar:
                if similarity < 0.60:
                    continue

                existing = await db.execute(
                    select(Alert).where(
                        Alert.user_id == user.id,
                        Alert.paper_id == paper.id,
                    )
                )
                if existing.scalar_one_or_none():
                    continue

                if similarity >= 0.80:
                    alert_type = "overlap_critical"
                elif similarity >= 0.70:
                    alert_type = "overlap_high"
                else:
                    alert_type = "overlap_moderate"

                pct = round(similarity * 100, 1)
                alert = Alert(
                    user_id=user.id,
                    paper_id=paper.id,
                    type=alert_type,
                    title=f"{alert_type.replace('_', ' ').title()}: {paper.title[:80]}",
                    description=f"This paper has {pct}% semantic similarity to your current project.",
                    similarity_score=similarity,
                    comparison_report=None,
                )

                if similarity >= 0.70:
                    report = await generate_overlap_report(
                        project_title=project.title,
                        project_description=project.description,
                        paper_title=paper.title,
                        paper_authors=paper.authors or [],
                        paper_abstract=paper.abstract or "",
                        similarity_score=similarity,
                    )
                    if report:
                        alert.comparison_report = report

                db.add(alert)
                logger.info(
                    "Created %s alert for user=%s paper=%s similarity=%.2f",
                    alert_type, user.id, paper.id, similarity,
                )

            await db.commit()

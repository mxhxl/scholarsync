import asyncio
import json
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


@celery_app.task(name="app.tasks.trend_tasks.recompute_trends")
def recompute_trends():
    _run_async(_async_recompute_trends())


async def _async_recompute_trends():
    from datetime import datetime, timedelta, timezone
    from sqlalchemy import select
    from app.core.database import AsyncSessionLocal
    from app.models.papers import Paper
    from app.models.trend_cache import TrendCache
    from app.services.trend_service import analyze_trends

    async with AsyncSessionLocal() as db:
        since = datetime.now(timezone.utc).date() - timedelta(days=90)
        result = await db.execute(
            select(Paper).where(Paper.published_date >= since)
        )
        papers = result.scalars().all()

        if len(papers) < 100:
            logger.warning("Only %d papers in last 90 days — skipping trend analysis", len(papers))
            return

        papers_data = [
            {
                "title": p.title,
                "abstract": p.abstract or "",
                "published_date": p.published_date,
            }
            for p in papers
        ]

        trends = analyze_trends(papers_data)
        now = datetime.now(timezone.utc)
        expires = now + timedelta(days=7)

        cache = TrendCache(
            results_json=json.dumps(trends, default=str),
            computed_at=now,
            expires_at=expires,
        )
        db.add(cache)
        await db.commit()
        logger.info("Trends recomputed: %d topics from %d papers", len(trends), len(papers))

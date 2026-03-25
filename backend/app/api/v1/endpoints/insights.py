import json
from datetime import datetime, timezone
from fastapi import APIRouter, Depends
from fastapi.responses import JSONResponse
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select
from app.core.database import get_db
from app.core.security import get_current_user
from app.models.users import User
from app.models.trend_cache import TrendCache
from app.schemas.insight import TrendsResponse, TrendResult

router = APIRouter(prefix="/insights", tags=["insights"])


@router.get("/trends")
async def get_trends(
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    now = datetime.now(timezone.utc)

    result = await db.execute(
        select(TrendCache)
        .where(TrendCache.expires_at > now)
        .order_by(TrendCache.computed_at.desc())
        .limit(1)
    )
    cache = result.scalar_one_or_none()

    if cache:
        trends_data = json.loads(cache.results_json)
        trends = [TrendResult(**t) for t in trends_data]
        return TrendsResponse(trends=trends, computed_at=cache.computed_at)

    from app.tasks.trend_tasks import recompute_trends
    recompute_trends.delay()
    return JSONResponse(
        status_code=202,
        content={"status": "computing", "retry_after": 60},
    )

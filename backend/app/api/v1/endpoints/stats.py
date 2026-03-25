from fastapi import APIRouter, Depends
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, func
from app.core.database import get_db
from app.core.security import get_current_user
from app.models.users import User
from app.models.user_feeds import UserFeed
from app.models.saved_papers import SavedPaper
from app.models.paper_summaries import PaperSummary
from app.models.alerts import Alert
from app.schemas.stats import UserStatsResponse

router = APIRouter(prefix="/stats", tags=["stats"])


@router.get("/me", response_model=UserStatsResponse)
async def get_user_stats(
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    # Papers read (from feed)
    papers_read_q = select(func.count()).select_from(
        select(UserFeed)
        .where(UserFeed.user_id == current_user.id, UserFeed.is_read == True)
        .subquery()
    )
    papers_read = (await db.execute(papers_read_q)).scalar_one()

    # Saved papers (library)
    saved_q = select(func.count()).select_from(
        select(SavedPaper)
        .where(SavedPaper.user_id == current_user.id)
        .subquery()
    )
    saved_papers = (await db.execute(saved_q)).scalar_one()

    # Summaries viewed (papers that have summaries in user's feed)
    summaries_q = select(func.count()).select_from(
        select(PaperSummary)
        .join(UserFeed, UserFeed.paper_id == PaperSummary.paper_id)
        .where(
            UserFeed.user_id == current_user.id,
            PaperSummary.status == "ready",
        )
        .subquery()
    )
    summaries = (await db.execute(summaries_q)).scalar_one()

    # Unread alerts
    alerts_q = select(func.count()).select_from(
        select(Alert)
        .where(Alert.user_id == current_user.id, Alert.is_read == False)
        .subquery()
    )
    unread_alerts = (await db.execute(alerts_q)).scalar_one()

    return UserStatsResponse(
        papers_read=papers_read,
        saved_papers=saved_papers,
        summaries=summaries,
        unread_alerts=unread_alerts,
    )

from datetime import date, timedelta
from fastapi import APIRouter, Depends
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.exc import IntegrityError
from sqlalchemy import select, func
from pydantic import BaseModel
from app.core.database import get_db
from app.core.security import get_current_user
from app.models.users import User
from app.models.reading_streaks import ReadingStreak, ReadingEvent

router = APIRouter(prefix="/streaks", tags=["streaks"])

# ── XP rules ────────────────────────────────────────────────────────────────
BASE_XP = 10       # per paper read
STREAK_BONUS = 5   # extra XP per streak day (e.g. 3-day streak = +15 bonus)


class StreakResponse(BaseModel):
    current_streak: int = 0
    longest_streak: int = 0
    total_papers_read: int = 0
    total_xp: int = 0
    last_read_date: date | None = None
    streak_start_date: date | None = None
    today_papers: int = 0
    weekly_goal_progress: int = 0  # papers read this week
    weekly_goal: int = 5
    read_dates: list[str] = []     # ISO dates for calendar heatmap


class RecordReadResponse(BaseModel):
    xp_earned: int
    streak: StreakResponse


@router.get("/me", response_model=StreakResponse)
async def get_streak(
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    streak = await _get_or_create_streak(db, current_user.id)
    today = date.today()

    # Check if streak is stale (missed a day)
    if streak.last_read_date and (today - streak.last_read_date).days > 1:
        streak.current_streak = 0
        streak.streak_start_date = None
        await db.commit()

    # Count today's reads
    today_count = (await db.execute(
        select(func.count()).where(
            ReadingEvent.user_id == current_user.id,
            ReadingEvent.read_date == today,
        )
    )).scalar_one()

    # Weekly progress (Mon-Sun)
    week_start = today - timedelta(days=today.weekday())
    weekly_count = (await db.execute(
        select(func.count()).where(
            ReadingEvent.user_id == current_user.id,
            ReadingEvent.read_date >= week_start,
        )
    )).scalar_one()

    # Recent read dates for calendar heatmap (last 60 days)
    cutoff = today - timedelta(days=60)
    dates_result = await db.execute(
        select(ReadingEvent.read_date)
        .where(
            ReadingEvent.user_id == current_user.id,
            ReadingEvent.read_date >= cutoff,
        )
        .distinct()
        .order_by(ReadingEvent.read_date.desc())
    )
    read_dates = [row[0].isoformat() for row in dates_result.all()]

    return StreakResponse(
        current_streak=streak.current_streak,
        longest_streak=streak.longest_streak,
        total_papers_read=streak.total_papers_read,
        total_xp=streak.total_xp,
        last_read_date=streak.last_read_date,
        streak_start_date=streak.streak_start_date,
        today_papers=today_count,
        weekly_goal_progress=weekly_count,
        weekly_goal=5,
        read_dates=read_dates,
    )


@router.post("/record-read/{paper_id}", response_model=RecordReadResponse)
async def record_read(
    paper_id: str,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    today = date.today()
    streak = await _get_or_create_streak(db, current_user.id)

    # Check if this paper was already read
    existing = await db.execute(
        select(ReadingEvent).where(
            ReadingEvent.user_id == current_user.id,
            ReadingEvent.paper_id == paper_id,
        )
    )
    if existing.scalar_one_or_none():
        # Already read — return current state without double-counting
        streak_resp = await get_streak(db=db, current_user=current_user)
        return RecordReadResponse(xp_earned=0, streak=streak_resp)

    # Calculate XP
    xp = BASE_XP + (STREAK_BONUS * streak.current_streak)

    # Record the event
    event = ReadingEvent(
        user_id=current_user.id,
        paper_id=paper_id,
        read_date=today,
        xp_earned=xp,
    )
    db.add(event)

    # Update streak
    if streak.last_read_date is None or (today - streak.last_read_date).days > 1:
        # New streak or broken streak
        streak.current_streak = 1
        streak.streak_start_date = today
    elif streak.last_read_date == today:
        # Already read today — streak stays same, just add XP
        pass
    else:
        # Consecutive day
        streak.current_streak += 1

    streak.last_read_date = today
    streak.total_papers_read += 1
    streak.total_xp += xp

    if streak.current_streak > streak.longest_streak:
        streak.longest_streak = streak.current_streak

    try:
        await db.commit()
    except IntegrityError:
        # Race condition: another request already inserted this event
        await db.rollback()
        streak_resp = await get_streak(db=db, current_user=current_user)
        return RecordReadResponse(xp_earned=0, streak=streak_resp)

    streak_resp = await get_streak(db=db, current_user=current_user)
    return RecordReadResponse(xp_earned=xp, streak=streak_resp)


async def _get_or_create_streak(db: AsyncSession, user_id: str) -> ReadingStreak:
    result = await db.execute(
        select(ReadingStreak).where(ReadingStreak.user_id == user_id)
    )
    streak = result.scalar_one_or_none()
    if not streak:
        streak = ReadingStreak(user_id=user_id)
        db.add(streak)
        await db.flush()
    return streak

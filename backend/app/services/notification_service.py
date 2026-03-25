import logging
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select
from app.models.users import User
from app.models.notification_preferences import NotificationPreferences
from app.models.user_feeds import UserFeed

logger = logging.getLogger(__name__)


async def send_digest_for_user(db: AsyncSession, user: User) -> None:
    """Log digest info. Stub — no real email until production."""
    result = await db.execute(
        select(UserFeed).where(
            UserFeed.user_id == user.id,
            UserFeed.is_read == False,
            UserFeed.priority == "high",
        )
    )
    unread_high = result.scalars().all()
    if unread_high:
        logger.info(
            "DIGEST STUB: user=%s email=%s unread_high_priority=%d",
            user.id,
            user.email,
            len(unread_high),
        )

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


@celery_app.task(name="app.tasks.notification_tasks.send_daily_digests")
def send_daily_digests():
    _run_async(_async_send_digests())


async def _async_send_digests():
    from sqlalchemy import select
    from app.core.database import AsyncSessionLocal
    from app.models.users import User
    from app.models.notification_preferences import NotificationPreferences
    from app.services.notification_service import send_digest_for_user

    async with AsyncSessionLocal() as db:
        result = await db.execute(
            select(User)
            .join(NotificationPreferences)
            .where(User.is_active == True, NotificationPreferences.enable_email == True)
        )
        users = result.scalars().all()
        for user in users:
            await send_digest_for_user(db, user)

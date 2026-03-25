from celery import Celery
from celery.schedules import crontab
from app.config import settings

celery_app = Celery(
    "scholarsync",
    broker=settings.RABBITMQ_URL,
    backend=settings.REDIS_URL,
    include=[
        "app.tasks.paper_tasks",
        "app.tasks.overlap_tasks",
        "app.tasks.trend_tasks",
        "app.tasks.notification_tasks",
    ],
)

celery_app.conf.update(
    task_serializer="json",
    accept_content=["json"],
    result_serializer="json",
    timezone="UTC",
    enable_utc=True,
    task_track_started=True,
    beat_schedule={
        "fetch_papers_for_all_users": {
            "task": "app.tasks.paper_tasks.fetch_papers_for_all_users",
            "schedule": crontab(minute=0, hour="*/6"),
        },
        "detect_overlaps_for_all_users": {
            "task": "app.tasks.overlap_tasks.detect_overlaps_for_all_users",
            "schedule": crontab(minute=0, hour=2),
        },
        "recompute_trends": {
            "task": "app.tasks.trend_tasks.recompute_trends",
            "schedule": crontab(minute=0, hour=1, day_of_week=0),
        },
        "send_daily_digests": {
            "task": "app.tasks.notification_tasks.send_daily_digests",
            "schedule": crontab(minute=0, hour=9),
        },
    },
)

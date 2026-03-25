from fastapi import APIRouter
from app.api.v1.endpoints import (
    auth, profile, feed, papers, library, projects, alerts, citations, insights, notifications, discover, stats,
    streaks, highlights, admin
)

v1_router = APIRouter()

v1_router.include_router(auth.router)
v1_router.include_router(profile.router)
v1_router.include_router(feed.router)
v1_router.include_router(papers.router)
v1_router.include_router(library.router)
v1_router.include_router(projects.router)
v1_router.include_router(alerts.router)
v1_router.include_router(citations.router)
v1_router.include_router(insights.router)
v1_router.include_router(notifications.router)
v1_router.include_router(discover.router)
v1_router.include_router(stats.router)
v1_router.include_router(streaks.router)
v1_router.include_router(highlights.router)
v1_router.include_router(admin.router)

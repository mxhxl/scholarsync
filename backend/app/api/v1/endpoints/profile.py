from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select
from app.core.database import get_db
from app.core.security import get_current_user
from app.models.users import User
from app.models.research_profiles import ResearchProfile
from app.models.notification_preferences import NotificationPreferences
from app.schemas.profile import (
    ProfileSetupRequest, ProfileUpdateRequest, ProfileResponse,
    NotificationPreferencesUpdate, NotificationPreferencesResponse,
)
from app.services.ai.embedding import generate_profile_embedding
from app.core.cache import invalidate_pattern
import datetime  # noqa

router = APIRouter(prefix="/profile", tags=["profile"])


def _profile_to_response(p: ResearchProfile) -> ProfileResponse:
    return ProfileResponse(
        id=str(p.id),
        user_id=str(p.user_id),
        research_field=p.research_field,
        topics=p.topics or [],
        keywords=p.keywords or [],
        authors_following=p.authors_following or [],
        created_at=p.created_at,
        updated_at=p.updated_at,
    )


def _prefs_to_response(prefs: NotificationPreferences) -> NotificationPreferencesResponse:
    digest_str = prefs.digest_time.strftime("%H:%M") if prefs.digest_time else "09:00"
    return NotificationPreferencesResponse(
        id=str(prefs.id),
        user_id=str(prefs.user_id),
        digest_time=digest_str,
        overlap_sensitivity=prefs.overlap_sensitivity,
        enable_high_priority=prefs.enable_high_priority,
        enable_overlap_alerts=prefs.enable_overlap_alerts,
        enable_email=prefs.enable_email,
    )


@router.post("/setup", response_model=ProfileResponse, status_code=201)
async def setup_profile(
    body: ProfileSetupRequest,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    result = await db.execute(
        select(ResearchProfile).where(ResearchProfile.user_id == current_user.id)
    )
    profile = result.scalar_one_or_none()

    embedding = generate_profile_embedding(body.research_field, body.topics, body.keywords)

    if profile:
        # Allow re-setup (e.g. after a partial failure)
        profile.research_field = body.research_field
        profile.topics = body.topics
        profile.keywords = body.keywords
        profile.authors_following = body.authors_following
        profile.set_embedding(embedding)
    else:
        profile = ResearchProfile(
            user_id=current_user.id,
            research_field=body.research_field,
            topics=body.topics,
            keywords=body.keywords,
            authors_following=body.authors_following,
        )
        profile.set_embedding(embedding)
        db.add(profile)

    # Ensure notification preferences exist
    prefs_result = await db.execute(
        select(NotificationPreferences).where(
            NotificationPreferences.user_id == current_user.id
        )
    )
    if not prefs_result.scalar_one_or_none():
        db.add(NotificationPreferences(user_id=current_user.id))

    await db.commit()
    await db.refresh(profile)
    await invalidate_pattern(f"feed:{current_user.id}:*")
    return _profile_to_response(profile)


@router.get("/", response_model=ProfileResponse)
async def get_profile(
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    result = await db.execute(
        select(ResearchProfile).where(ResearchProfile.user_id == current_user.id)
    )
    profile = result.scalar_one_or_none()
    if not profile:
        raise HTTPException(
            status_code=404,
            detail={"error": "not_found", "detail": "Profile not found"},
        )
    return _profile_to_response(profile)


@router.put("/", response_model=ProfileResponse)
async def update_profile(
    body: ProfileUpdateRequest,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    result = await db.execute(
        select(ResearchProfile).where(ResearchProfile.user_id == current_user.id)
    )
    profile = result.scalar_one_or_none()
    if not profile:
        raise HTTPException(
            status_code=404,
            detail={"error": "not_found", "detail": "Profile not found"},
        )
    if body.research_field is not None:
        profile.research_field = body.research_field
    if body.topics is not None:
        profile.topics = body.topics
    if body.keywords is not None:
        profile.keywords = body.keywords
    if body.authors_following is not None:
        profile.authors_following = body.authors_following

    profile.set_embedding(generate_profile_embedding(
        profile.research_field, profile.topics or [], profile.keywords or []
    ))
    await db.commit()
    await db.refresh(profile)
    await invalidate_pattern(f"feed:{current_user.id}:*")
    return _profile_to_response(profile)


@router.post("/follow-author", response_model=ProfileResponse)
async def follow_author(
    body: dict,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    author_name = body.get("author_name", "").strip()
    if not author_name:
        raise HTTPException(status_code=400, detail="author_name is required")

    result = await db.execute(
        select(ResearchProfile).where(ResearchProfile.user_id == current_user.id)
    )
    profile = result.scalar_one_or_none()
    if not profile:
        raise HTTPException(status_code=404, detail="Profile not found")

    current = list(profile.authors_following or [])
    if author_name not in current:
        current.append(author_name)
        profile.authors_following = current
        await db.commit()
        await db.refresh(profile)
    return _profile_to_response(profile)


@router.post("/unfollow-author", response_model=ProfileResponse)
async def unfollow_author(
    body: dict,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    author_name = body.get("author_name", "").strip()
    if not author_name:
        raise HTTPException(status_code=400, detail="author_name is required")

    result = await db.execute(
        select(ResearchProfile).where(ResearchProfile.user_id == current_user.id)
    )
    profile = result.scalar_one_or_none()
    if not profile:
        raise HTTPException(status_code=404, detail="Profile not found")

    current = list(profile.authors_following or [])
    if author_name in current:
        current.remove(author_name)
        profile.authors_following = current
        await db.commit()
        await db.refresh(profile)
    return _profile_to_response(profile)


@router.get("/preferences", response_model=NotificationPreferencesResponse)
async def get_preferences(
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    result = await db.execute(
        select(NotificationPreferences).where(
            NotificationPreferences.user_id == current_user.id
        )
    )
    prefs = result.scalar_one_or_none()
    if not prefs:
        prefs = NotificationPreferences(user_id=current_user.id)
        db.add(prefs)
        await db.commit()
        await db.refresh(prefs)
    return _prefs_to_response(prefs)


@router.put("/preferences", response_model=NotificationPreferencesResponse)
async def update_preferences(
    body: NotificationPreferencesUpdate,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    result = await db.execute(
        select(NotificationPreferences).where(
            NotificationPreferences.user_id == current_user.id
        )
    )
    prefs = result.scalar_one_or_none()
    if not prefs:
        prefs = NotificationPreferences(user_id=current_user.id)
        db.add(prefs)

    if body.digest_time is not None:
        h, m = map(int, body.digest_time.split(":"))
        prefs.digest_time = datetime.time(h, m)
    if body.overlap_sensitivity is not None:
        prefs.overlap_sensitivity = body.overlap_sensitivity
    if body.enable_high_priority is not None:
        prefs.enable_high_priority = body.enable_high_priority
    if body.enable_overlap_alerts is not None:
        prefs.enable_overlap_alerts = body.enable_overlap_alerts
    if body.enable_email is not None:
        prefs.enable_email = body.enable_email

    await db.commit()
    await db.refresh(prefs)
    return _prefs_to_response(prefs)

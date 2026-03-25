from datetime import datetime, timedelta, timezone
from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, func, desc
from app.core.database import get_db
from app.core.security import get_admin_user, hash_password
from app.models.users import User
from app.models.papers import Paper
from app.models.alerts import Alert
from app.models.user_feeds import UserFeed
from app.models.saved_papers import SavedPaper
from app.models.paper_summaries import PaperSummary
from app.schemas.admin import (
    AdminDashboardResponse,
    AdminCreateUserRequest,
    AdminUserItem,
    AdminUserListResponse,
    AdminUserUpdateRequest,
    AdminPaperItem,
    AdminPaperListResponse,
    AdminAlertItem,
    AdminAlertListResponse,
)

router = APIRouter(prefix="/admin", tags=["admin"])


# ── Dashboard ─────────────────────────────────────────────────────────


@router.get("/dashboard", response_model=AdminDashboardResponse)
async def admin_dashboard(
    db: AsyncSession = Depends(get_db),
    _admin: User = Depends(get_admin_user),
):
    now = datetime.now(timezone.utc)
    week_ago = now - timedelta(days=7)

    total_users = (await db.execute(select(func.count()).select_from(User))).scalar_one()
    active_users = (
        await db.execute(select(func.count()).select_from(User).where(User.is_active == True))
    ).scalar_one()
    total_papers = (await db.execute(select(func.count()).select_from(Paper))).scalar_one()
    total_alerts = (await db.execute(select(func.count()).select_from(Alert))).scalar_one()
    total_saved = (await db.execute(select(func.count()).select_from(SavedPaper))).scalar_one()
    new_users_7d = (
        await db.execute(
            select(func.count()).select_from(User).where(User.created_at >= week_ago)
        )
    ).scalar_one()
    new_papers_7d = (
        await db.execute(
            select(func.count()).select_from(Paper).where(Paper.created_at >= week_ago)
        )
    ).scalar_one()

    return AdminDashboardResponse(
        total_users=total_users,
        active_users=active_users,
        total_papers=total_papers,
        total_alerts=total_alerts,
        total_saved_papers=total_saved,
        new_users_last_7_days=new_users_7d,
        new_papers_last_7_days=new_papers_7d,
    )


# ── Users ─────────────────────────────────────────────────────────────


@router.post("/users", response_model=AdminUserItem, status_code=201)
async def admin_create_user(
    body: AdminCreateUserRequest,
    db: AsyncSession = Depends(get_db),
    _admin: User = Depends(get_admin_user),
):
    # Check duplicate email
    existing = await db.execute(select(User).where(User.email == body.email))
    if existing.scalar_one_or_none():
        raise HTTPException(status_code=409, detail="A user with this email already exists")

    if body.role not in ("user", "admin"):
        raise HTTPException(status_code=400, detail="Role must be 'user' or 'admin'")

    user = User(
        email=body.email,
        hashed_password=hash_password(body.password),
        full_name=body.full_name,
        institution=body.institution,
        role=body.role,
        is_active=True,
        is_verified=True,
    )
    db.add(user)
    await db.commit()
    await db.refresh(user)

    return AdminUserItem(
        id=str(user.id),
        email=user.email,
        full_name=user.full_name,
        institution=user.institution,
        role=user.role,
        is_active=user.is_active,
        is_verified=user.is_verified,
        created_at=user.created_at.isoformat(),
        papers_read=0,
        saved_papers=0,
    )


@router.get("/users", response_model=AdminUserListResponse)
async def admin_list_users(
    db: AsyncSession = Depends(get_db),
    _admin: User = Depends(get_admin_user),
    search: str | None = Query(None),
    role: str | None = Query(None),
    is_active: bool | None = Query(None),
    limit: int = Query(20, ge=1, le=100),
    offset: int = Query(0, ge=0),
):
    q = select(User)
    count_q = select(func.count()).select_from(User)

    if search:
        pattern = f"%{search}%"
        q = q.where(User.email.ilike(pattern) | User.full_name.ilike(pattern))
        count_q = count_q.where(User.email.ilike(pattern) | User.full_name.ilike(pattern))
    if role:
        q = q.where(User.role == role)
        count_q = count_q.where(User.role == role)
    if is_active is not None:
        q = q.where(User.is_active == is_active)
        count_q = count_q.where(User.is_active == is_active)

    total = (await db.execute(count_q)).scalar_one()
    result = await db.execute(q.order_by(desc(User.created_at)).limit(limit).offset(offset))
    users = result.scalars().all()

    items = []
    for u in users:
        # Count papers read
        pr = (
            await db.execute(
                select(func.count())
                .select_from(UserFeed)
                .where(UserFeed.user_id == u.id, UserFeed.is_read == True)
            )
        ).scalar_one()
        # Count saved papers
        sp = (
            await db.execute(
                select(func.count())
                .select_from(SavedPaper)
                .where(SavedPaper.user_id == u.id)
            )
        ).scalar_one()
        items.append(
            AdminUserItem(
                id=str(u.id),
                email=u.email,
                full_name=u.full_name,
                institution=u.institution,
                role=u.role,
                is_active=u.is_active,
                is_verified=u.is_verified,
                created_at=u.created_at.isoformat(),
                papers_read=pr,
                saved_papers=sp,
            )
        )

    return AdminUserListResponse(items=items, total=total)


@router.get("/users/{user_id}", response_model=AdminUserItem)
async def admin_get_user(
    user_id: str,
    db: AsyncSession = Depends(get_db),
    _admin: User = Depends(get_admin_user),
):
    result = await db.execute(select(User).where(User.id == user_id))
    user = result.scalar_one_or_none()
    if not user:
        raise HTTPException(status_code=404, detail="User not found")

    pr = (
        await db.execute(
            select(func.count())
            .select_from(UserFeed)
            .where(UserFeed.user_id == user.id, UserFeed.is_read == True)
        )
    ).scalar_one()
    sp = (
        await db.execute(
            select(func.count())
            .select_from(SavedPaper)
            .where(SavedPaper.user_id == user.id)
        )
    ).scalar_one()

    return AdminUserItem(
        id=str(user.id),
        email=user.email,
        full_name=user.full_name,
        institution=user.institution,
        role=user.role,
        is_active=user.is_active,
        is_verified=user.is_verified,
        created_at=user.created_at.isoformat(),
        papers_read=pr,
        saved_papers=sp,
    )


@router.patch("/users/{user_id}", response_model=AdminUserItem)
async def admin_update_user(
    user_id: str,
    body: AdminUserUpdateRequest,
    db: AsyncSession = Depends(get_db),
    admin: User = Depends(get_admin_user),
):
    result = await db.execute(select(User).where(User.id == user_id))
    user = result.scalar_one_or_none()
    if not user:
        raise HTTPException(status_code=404, detail="User not found")

    if user_id == admin.id and body.role and body.role != "admin":
        raise HTTPException(status_code=400, detail="Cannot remove your own admin role")

    if body.is_active is not None:
        user.is_active = body.is_active
    if body.is_verified is not None:
        user.is_verified = body.is_verified
    if body.role is not None:
        if body.role not in ("user", "admin"):
            raise HTTPException(status_code=400, detail="Role must be 'user' or 'admin'")
        user.role = body.role

    await db.commit()
    await db.refresh(user)

    pr = (
        await db.execute(
            select(func.count())
            .select_from(UserFeed)
            .where(UserFeed.user_id == user.id, UserFeed.is_read == True)
        )
    ).scalar_one()
    sp = (
        await db.execute(
            select(func.count())
            .select_from(SavedPaper)
            .where(SavedPaper.user_id == user.id)
        )
    ).scalar_one()

    return AdminUserItem(
        id=str(user.id),
        email=user.email,
        full_name=user.full_name,
        institution=user.institution,
        role=user.role,
        is_active=user.is_active,
        is_verified=user.is_verified,
        created_at=user.created_at.isoformat(),
        papers_read=pr,
        saved_papers=sp,
    )


@router.delete("/users/{user_id}", status_code=204)
async def admin_delete_user(
    user_id: str,
    db: AsyncSession = Depends(get_db),
    admin: User = Depends(get_admin_user),
):
    if user_id == admin.id:
        raise HTTPException(status_code=400, detail="Cannot delete your own account")

    result = await db.execute(select(User).where(User.id == user_id))
    user = result.scalar_one_or_none()
    if not user:
        raise HTTPException(status_code=404, detail="User not found")

    await db.delete(user)
    await db.commit()
    return None


# ── Papers ────────────────────────────────────────────────────────────


@router.get("/papers", response_model=AdminPaperListResponse)
async def admin_list_papers(
    db: AsyncSession = Depends(get_db),
    _admin: User = Depends(get_admin_user),
    search: str | None = Query(None),
    source: str | None = Query(None),
    limit: int = Query(20, ge=1, le=100),
    offset: int = Query(0, ge=0),
):
    q = select(Paper)
    count_q = select(func.count()).select_from(Paper)

    if search:
        pattern = f"%{search}%"
        q = q.where(Paper.title.ilike(pattern))
        count_q = count_q.where(Paper.title.ilike(pattern))
    if source:
        q = q.where(Paper.source == source)
        count_q = count_q.where(Paper.source == source)

    total = (await db.execute(count_q)).scalar_one()
    result = await db.execute(q.order_by(desc(Paper.created_at)).limit(limit).offset(offset))
    papers = result.scalars().all()

    items = []
    for p in papers:
        has_summary = (
            await db.execute(
                select(func.count())
                .select_from(PaperSummary)
                .where(PaperSummary.paper_id == p.id, PaperSummary.status == "ready")
            )
        ).scalar_one() > 0
        feed_count = (
            await db.execute(
                select(func.count()).select_from(UserFeed).where(UserFeed.paper_id == p.id)
            )
        ).scalar_one()
        saved_count = (
            await db.execute(
                select(func.count()).select_from(SavedPaper).where(SavedPaper.paper_id == p.id)
            )
        ).scalar_one()
        items.append(
            AdminPaperItem(
                id=str(p.id),
                external_id=p.external_id,
                source=p.source,
                title=p.title,
                authors=p.authors or [],
                published_date=p.published_date.isoformat() if p.published_date else None,
                citation_count=p.citation_count,
                venue=p.venue,
                has_summary=has_summary,
                feed_count=feed_count,
                saved_count=saved_count,
                created_at=p.created_at.isoformat(),
            )
        )

    return AdminPaperListResponse(items=items, total=total)


@router.delete("/papers/{paper_id}", status_code=204)
async def admin_delete_paper(
    paper_id: str,
    db: AsyncSession = Depends(get_db),
    _admin: User = Depends(get_admin_user),
):
    result = await db.execute(select(Paper).where(Paper.id == paper_id))
    paper = result.scalar_one_or_none()
    if not paper:
        raise HTTPException(status_code=404, detail="Paper not found")
    await db.delete(paper)
    await db.commit()
    return None


# ── Alerts ────────────────────────────────────────────────────────────


@router.get("/alerts", response_model=AdminAlertListResponse)
async def admin_list_alerts(
    db: AsyncSession = Depends(get_db),
    _admin: User = Depends(get_admin_user),
    type: str | None = Query(None),
    is_read: bool | None = Query(None),
    limit: int = Query(20, ge=1, le=100),
    offset: int = Query(0, ge=0),
):
    q = select(Alert, User.email, Paper.title).join(User, Alert.user_id == User.id).join(Paper, Alert.paper_id == Paper.id)
    count_q = select(func.count()).select_from(Alert)

    if type:
        q = q.where(Alert.type == type)
        count_q = count_q.where(Alert.type == type)
    if is_read is not None:
        q = q.where(Alert.is_read == is_read)
        count_q = count_q.where(Alert.is_read == is_read)

    total = (await db.execute(count_q)).scalar_one()
    result = await db.execute(q.order_by(desc(Alert.created_at)).limit(limit).offset(offset))
    rows = result.all()

    items = [
        AdminAlertItem(
            id=str(alert.id),
            user_id=str(alert.user_id),
            user_email=email,
            paper_id=str(alert.paper_id),
            paper_title=paper_title,
            type=alert.type,
            title=alert.title,
            is_read=alert.is_read,
            is_acknowledged=alert.is_acknowledged,
            created_at=alert.created_at.isoformat(),
        )
        for alert, email, paper_title in rows
    ]

    return AdminAlertListResponse(items=items, total=total)


@router.delete("/alerts/{alert_id}", status_code=204)
async def admin_delete_alert(
    alert_id: str,
    db: AsyncSession = Depends(get_db),
    _admin: User = Depends(get_admin_user),
):
    result = await db.execute(select(Alert).where(Alert.id == alert_id))
    alert = result.scalar_one_or_none()
    if not alert:
        raise HTTPException(status_code=404, detail="Alert not found")
    await db.delete(alert)
    await db.commit()
    return None

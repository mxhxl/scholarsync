from fastapi import APIRouter, Depends, HTTPException, status, Request
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select
from app.core.database import get_db
from app.core.security import (
    hash_password, verify_password, create_access_token,
    create_refresh_token, get_current_user,
)
from app.models.users import User
from app.models.login_activity import LoginActivity
from app.schemas.auth import (
    RegisterRequest, LoginRequest, RefreshRequest, ChangePasswordRequest,
    TokenResponse, RefreshResponse, UserResponse, LoginActivityResponse,
)
from app.config import settings
from jose import JWTError, jwt

router = APIRouter(prefix="/auth", tags=["auth"])


def _user_to_response(user: User) -> UserResponse:
    return UserResponse(
        id=str(user.id),
        email=user.email,
        full_name=user.full_name,
        institution=user.institution,
        role=getattr(user, "role", "user"),
    )


@router.post("/register", response_model=TokenResponse, status_code=201)
async def register(body: RegisterRequest, db: AsyncSession = Depends(get_db)):
    existing = await db.execute(select(User).where(User.email == body.email))
    if existing.scalar_one_or_none():
        raise HTTPException(
            status_code=409,
            detail={"error": "email_already_exists", "detail": "An account with this email already exists"},
        )
    user = User(
        email=body.email,
        hashed_password=hash_password(body.password),
        full_name=body.full_name,
    )
    db.add(user)
    await db.commit()
    await db.refresh(user)
    access_token = create_access_token({"sub": str(user.id)})
    refresh_token = create_refresh_token({"sub": str(user.id)})
    return TokenResponse(
        access_token=access_token,
        refresh_token=refresh_token,
        user=_user_to_response(user),
    )


@router.post("/login", response_model=TokenResponse)
async def login(
    body: LoginRequest,
    request: Request,
    db: AsyncSession = Depends(get_db),
):
    result = await db.execute(select(User).where(User.email == body.email))
    user = result.scalar_one_or_none()
    if not user or not verify_password(body.password, user.hashed_password):
        raise HTTPException(
            status_code=401,
            detail={"error": "invalid_credentials", "detail": "Email or password is incorrect"},
        )
    if not user.is_active:
        raise HTTPException(
            status_code=401,
            detail={"error": "account_inactive", "detail": "Account is deactivated"},
        )
    await _record_login_activity(db, str(user.id), request)
    access_token = create_access_token({"sub": str(user.id)})
    refresh_token = create_refresh_token({"sub": str(user.id)})
    return TokenResponse(
        access_token=access_token,
        refresh_token=refresh_token,
        user=_user_to_response(user),
    )


@router.post("/refresh", response_model=RefreshResponse)
async def refresh_token(body: RefreshRequest, db: AsyncSession = Depends(get_db)):
    try:
        payload = jwt.decode(
            body.refresh_token, settings.SECRET_KEY, algorithms=[settings.ALGORITHM]
        )
        user_id = payload.get("sub")
        token_type = payload.get("type")
        if not user_id or token_type != "refresh":
            raise HTTPException(
                status_code=401,
                detail={"error": "invalid_token", "detail": "Invalid refresh token"},
            )
    except JWTError:
        raise HTTPException(
            status_code=401,
            detail={"error": "invalid_token", "detail": "Invalid or expired refresh token"},
        )
    result = await db.execute(select(User).where(User.id == user_id))
    user = result.scalar_one_or_none()
    if not user or not user.is_active:
        raise HTTPException(
            status_code=401,
            detail={"error": "invalid_token", "detail": "User not found or inactive"},
        )
    return RefreshResponse(
        access_token=create_access_token({"sub": str(user.id)}),
        refresh_token=create_refresh_token({"sub": str(user.id)}),
    )


@router.get("/me", response_model=UserResponse)
async def get_me(current_user: User = Depends(get_current_user)):
    return _user_to_response(current_user)


@router.post("/change-password", status_code=204)
async def change_password(
    body: ChangePasswordRequest,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    if not verify_password(body.current_password, current_user.hashed_password):
        raise HTTPException(
            status_code=400,
            detail={"error": "invalid_password", "detail": "Current password is incorrect"},
        )
    current_user.hashed_password = hash_password(body.new_password)
    await db.commit()
    return None


@router.get("/login-activity", response_model=list[LoginActivityResponse])
async def get_login_activity(
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
    limit: int = 50,
):
    from sqlalchemy import desc

    result = await db.execute(
        select(LoginActivity)
        .where(LoginActivity.user_id == current_user.id)
        .order_by(desc(LoginActivity.logged_at))
        .limit(max(1, min(limit, 100)))
    )
    activities = result.scalars().all()
    return [
        LoginActivityResponse(
            id=str(a.id),
            logged_at=a.logged_at.isoformat(),
            ip_address=a.ip_address,
            device_info=a.device_info,
        )
        for a in activities
    ]


async def _record_login_activity(db: AsyncSession, user_id: str, request: Request) -> None:
    from datetime import datetime, timezone

    client = request.client
    host = client.host if client else None
    user_agent = request.headers.get("user-agent") or ""
    device = (user_agent.split("/")[0].strip() if user_agent else "Unknown")[:200]
    activity = LoginActivity(
        user_id=user_id,
        logged_at=datetime.now(timezone.utc),
        ip_address=host,
        device_info=device,
    )
    db.add(activity)
    await db.commit()

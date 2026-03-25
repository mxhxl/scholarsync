from fastapi import APIRouter, Depends, Response
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select
from app.core.database import get_db
from app.core.security import get_current_user
from app.models.users import User
from app.models.device_tokens import DeviceToken
from app.schemas.notification import DeviceTokenCreate, DeviceTokenResponse

router = APIRouter(prefix="/notifications", tags=["notifications"])


@router.post("/device-token", response_model=DeviceTokenResponse, status_code=201)
async def register_device_token(
    body: DeviceTokenCreate,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    result = await db.execute(
        select(DeviceToken).where(DeviceToken.token == body.token)
    )
    existing = result.scalar_one_or_none()
    if existing:
        existing.user_id = current_user.id
        existing.platform = body.platform
        await db.commit()
        await db.refresh(existing)
        return DeviceTokenResponse(
            id=str(existing.id),
            user_id=str(existing.user_id),
            token=existing.token,
            platform=existing.platform,
        )

    dt = DeviceToken(
        user_id=current_user.id,
        token=body.token,
        platform=body.platform,
    )
    db.add(dt)
    await db.commit()
    await db.refresh(dt)
    return DeviceTokenResponse(
        id=str(dt.id),
        user_id=str(dt.user_id),
        token=dt.token,
        platform=dt.platform,
    )


@router.delete("/device-token/{token}", status_code=204)
async def delete_device_token(
    token: str,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    result = await db.execute(
        select(DeviceToken).where(
            DeviceToken.token == token,
            DeviceToken.user_id == current_user.id,
        )
    )
    dt = result.scalar_one_or_none()
    if dt:
        await db.delete(dt)
        await db.commit()
    return Response(status_code=204)

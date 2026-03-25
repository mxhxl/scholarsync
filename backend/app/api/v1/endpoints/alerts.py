from fastapi import APIRouter, Depends, HTTPException, Query, Response
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, func
from sqlalchemy.orm import selectinload
from app.core.database import get_db
from app.core.security import get_current_user
from app.models.users import User
from app.models.alerts import Alert
from app.models.papers import Paper
from app.models.current_projects import CurrentProject
from app.schemas.alert import AlertResponse, AlertsResponse, UnreadCountResponse, AlertPaper

router = APIRouter(prefix="/alerts", tags=["alerts"])


def _alert_to_response(alert: Alert) -> AlertResponse:
    p = alert.paper
    return AlertResponse(
        id=str(alert.id),
        user_id=str(alert.user_id),
        paper_id=str(alert.paper_id),
        type=alert.type,
        title=alert.title,
        description=alert.description,
        similarity_score=alert.similarity_score,
        comparison_report=alert.comparison_report,
        is_read=alert.is_read,
        is_acknowledged=alert.is_acknowledged,
        created_at=alert.created_at,
        paper=AlertPaper(
            id=str(p.id),
            title=p.title,
            authors=p.authors or [],
            published_date=p.published_date,
            source=p.source,
            venue=p.venue,
        ),
    )


@router.get("/", response_model=AlertsResponse)
async def get_alerts(
    type: str | None = Query(None),
    is_read: bool | None = Query(None),
    limit: int = Query(20, le=100),
    offset: int = Query(0, ge=0),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    query = (
        select(Alert)
        .where(Alert.user_id == current_user.id)
        .options(selectinload(Alert.paper))
    )
    if type:
        query = query.where(Alert.type == type)
    if is_read is not None:
        query = query.where(Alert.is_read == is_read)

    count_q = select(func.count()).select_from(query.subquery())
    total = (await db.execute(count_q)).scalar_one()

    query = query.order_by(Alert.created_at.desc()).offset(offset).limit(limit)
    result = await db.execute(query)
    alerts = result.scalars().all()
    return AlertsResponse(items=[_alert_to_response(a) for a in alerts], total=total)


@router.get("/unread-count", response_model=UnreadCountResponse)
async def unread_count(
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    result = await db.execute(
        select(func.count()).where(
            Alert.user_id == current_user.id,
            Alert.is_read == False,
        )
    )
    count = result.scalar_one()
    return UnreadCountResponse(count=count)


@router.get("/{alert_id}", response_model=AlertResponse)
async def get_alert(
    alert_id: str,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    result = await db.execute(
        select(Alert)
        .where(Alert.id == alert_id, Alert.user_id == current_user.id)
        .options(selectinload(Alert.paper))
    )
    alert = result.scalar_one_or_none()
    if not alert:
        raise HTTPException(
            status_code=404,
            detail={"error": "not_found", "detail": "Alert not found"},
        )

    if alert.comparison_report is None and alert.similarity_score >= 0.70:
        project_result = await db.execute(
            select(CurrentProject).where(CurrentProject.user_id == current_user.id)
        )
        project = project_result.scalar_one_or_none()
        if project:
            from app.services.ai.overlap import generate_overlap_report
            report = await generate_overlap_report(
                project_title=project.title,
                project_description=project.description,
                paper_title=alert.paper.title,
                paper_authors=alert.paper.authors or [],
                paper_abstract=alert.paper.abstract or "",
                similarity_score=alert.similarity_score,
            )
            if report:
                alert.comparison_report = report
                await db.commit()
                await db.refresh(alert)

    return _alert_to_response(alert)


@router.post("/{alert_id}/read", status_code=204)
async def mark_alert_read(
    alert_id: str,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    result = await db.execute(
        select(Alert).where(Alert.id == alert_id, Alert.user_id == current_user.id)
    )
    alert = result.scalar_one_or_none()
    if alert:
        alert.is_read = True
        await db.commit()
    return Response(status_code=204)


@router.post("/{alert_id}/acknowledge", status_code=204)
async def acknowledge_alert(
    alert_id: str,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    result = await db.execute(
        select(Alert).where(Alert.id == alert_id, Alert.user_id == current_user.id)
    )
    alert = result.scalar_one_or_none()
    if alert:
        alert.is_acknowledged = True
        await db.commit()
    return Response(status_code=204)

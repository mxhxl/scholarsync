from fastapi import APIRouter, Depends, HTTPException, Response
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select
from pydantic import BaseModel
from datetime import datetime
from app.core.database import get_db
from app.core.security import get_current_user
from app.models.users import User
from app.models.paper_highlights import PaperHighlight

router = APIRouter(prefix="/papers", tags=["highlights"])


class HighlightCreate(BaseModel):
    highlighted_text: str
    note: str | None = None
    color: str = "yellow"
    page_number: int | None = None


class HighlightUpdate(BaseModel):
    highlighted_text: str | None = None
    note: str | None = None
    color: str | None = None


class HighlightResponse(BaseModel):
    id: str
    paper_id: str
    highlighted_text: str
    note: str | None = None
    color: str
    page_number: int | None = None
    created_at: datetime
    updated_at: datetime


class HighlightsListResponse(BaseModel):
    items: list[HighlightResponse] = []
    total: int = 0


@router.get("/{paper_id}/highlights", response_model=HighlightsListResponse)
async def get_highlights(
    paper_id: str,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    result = await db.execute(
        select(PaperHighlight)
        .where(
            PaperHighlight.user_id == current_user.id,
            PaperHighlight.paper_id == paper_id,
        )
        .order_by(PaperHighlight.created_at.desc())
    )
    highlights = result.scalars().all()
    items = [
        HighlightResponse(
            id=h.id,
            paper_id=h.paper_id,
            highlighted_text=h.highlighted_text,
            note=h.note,
            color=h.color,
            page_number=h.page_number,
            created_at=h.created_at,
            updated_at=h.updated_at,
        )
        for h in highlights
    ]
    return HighlightsListResponse(items=items, total=len(items))


@router.post("/{paper_id}/highlights", response_model=HighlightResponse, status_code=201)
async def create_highlight(
    paper_id: str,
    body: HighlightCreate,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    highlight = PaperHighlight(
        user_id=current_user.id,
        paper_id=paper_id,
        highlighted_text=body.highlighted_text,
        note=body.note,
        color=body.color,
        page_number=body.page_number,
    )
    db.add(highlight)
    await db.commit()
    await db.refresh(highlight)
    return HighlightResponse(
        id=highlight.id,
        paper_id=highlight.paper_id,
        highlighted_text=highlight.highlighted_text,
        note=highlight.note,
        color=highlight.color,
        page_number=highlight.page_number,
        created_at=highlight.created_at,
        updated_at=highlight.updated_at,
    )


@router.put("/{paper_id}/highlights/{highlight_id}", response_model=HighlightResponse)
async def update_highlight(
    paper_id: str,
    highlight_id: str,
    body: HighlightUpdate,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    result = await db.execute(
        select(PaperHighlight).where(
            PaperHighlight.id == highlight_id,
            PaperHighlight.user_id == current_user.id,
            PaperHighlight.paper_id == paper_id,
        )
    )
    highlight = result.scalar_one_or_none()
    if not highlight:
        raise HTTPException(status_code=404, detail="Highlight not found")

    if body.highlighted_text is not None:
        highlight.highlighted_text = body.highlighted_text
    if body.note is not None:
        highlight.note = body.note
    if body.color is not None:
        highlight.color = body.color

    await db.commit()
    await db.refresh(highlight)
    return HighlightResponse(
        id=highlight.id,
        paper_id=highlight.paper_id,
        highlighted_text=highlight.highlighted_text,
        note=highlight.note,
        color=highlight.color,
        page_number=highlight.page_number,
        created_at=highlight.created_at,
        updated_at=highlight.updated_at,
    )


@router.delete("/{paper_id}/highlights/{highlight_id}", status_code=204)
async def delete_highlight(
    paper_id: str,
    highlight_id: str,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    result = await db.execute(
        select(PaperHighlight).where(
            PaperHighlight.id == highlight_id,
            PaperHighlight.user_id == current_user.id,
            PaperHighlight.paper_id == paper_id,
        )
    )
    highlight = result.scalar_one_or_none()
    if highlight:
        await db.delete(highlight)
        await db.commit()
    return Response(status_code=204)

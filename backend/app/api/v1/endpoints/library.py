from fastapi import APIRouter, Depends, HTTPException, Query, Response
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, func
from sqlalchemy.orm import selectinload
from app.core.database import get_db
from app.core.security import get_current_user
from app.models.users import User
from app.models.papers import Paper
from app.models.saved_papers import SavedPaper
from app.models.folders import Folder
from app.models.user_feeds import UserFeed
from app.schemas.library import (
    SavePaperRequest, UpdateSavedPaperRequest,
    SavedPaperResponse, SavedPapersResponse, SavedPaperPaper,
    FolderCreate, FolderResponse,
)
from app.repositories.paper_repository import search_papers_fulltext

router = APIRouter(prefix="/library", tags=["library"])


def _saved_to_response(sp: SavedPaper) -> SavedPaperResponse:
    p = sp.paper
    return SavedPaperResponse(
        id=str(sp.id),
        user_id=str(sp.user_id),
        paper_id=str(sp.paper_id),
        folder_id=str(sp.folder_id) if sp.folder_id else None,
        tags=sp.tags or [],
        personal_note=sp.personal_note,
        is_read=sp.is_read,
        saved_at=sp.saved_at,
        paper=SavedPaperPaper(
            id=str(p.id),
            external_id=p.external_id,
            source=p.source,
            title=p.title,
            authors=p.authors or [],
            abstract=p.abstract,
            published_date=p.published_date,
            pdf_url=p.pdf_url,
            citation_count=p.citation_count or 0,
            venue=p.venue,
        ),
    )


@router.post("/save", response_model=SavedPaperResponse, status_code=201)
async def save_paper(
    body: SavePaperRequest,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    paper_result = await db.execute(select(Paper).where(Paper.id == body.paper_id))
    paper = paper_result.scalar_one_or_none()
    if not paper:
        raise HTTPException(
            status_code=404,
            detail={"error": "not_found", "detail": "Paper not found"},
        )

    existing = await db.execute(
        select(SavedPaper).where(
            SavedPaper.user_id == current_user.id,
            SavedPaper.paper_id == body.paper_id,
        )
    )
    if existing.scalar_one_or_none():
        raise HTTPException(
            status_code=409,
            detail={"error": "already_saved", "detail": "Paper already in library"},
        )

    sp = SavedPaper(
        user_id=current_user.id,
        paper_id=body.paper_id,
        folder_id=body.folder_id,
        tags=body.tags,
        personal_note=body.personal_note,
    )
    db.add(sp)

    feed_result = await db.execute(
        select(UserFeed).where(
            UserFeed.user_id == current_user.id,
            UserFeed.paper_id == body.paper_id,
        )
    )
    feed = feed_result.scalar_one_or_none()
    if feed:
        feed.is_saved = True

    await db.commit()
    await db.refresh(sp)

    sp_result = await db.execute(
        select(SavedPaper)
        .where(SavedPaper.id == sp.id)
        .options(selectinload(SavedPaper.paper))
    )
    sp = sp_result.scalar_one()
    return _saved_to_response(sp)


@router.get("/search", response_model=SavedPapersResponse)
async def search_library(
    q: str = Query(..., min_length=1),
    limit: int = Query(20, le=100),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    saved = await search_papers_fulltext(db, q, current_user.id, limit)
    result = await db.execute(
        select(SavedPaper)
        .where(SavedPaper.id.in_([s.id for s in saved]))
        .options(selectinload(SavedPaper.paper))
    )
    items = result.scalars().all()
    return SavedPapersResponse(items=[_saved_to_response(i) for i in items], total=len(items))


@router.get("/folders", response_model=list[FolderResponse])
async def get_folders(
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    result = await db.execute(
        select(Folder).where(Folder.user_id == current_user.id).order_by(Folder.created_at)
    )
    folders = result.scalars().all()
    return [
        FolderResponse(id=str(f.id), user_id=str(f.user_id), name=f.name, created_at=f.created_at)
        for f in folders
    ]


@router.post("/folders", response_model=FolderResponse, status_code=201)
async def create_folder(
    body: FolderCreate,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    folder = Folder(user_id=current_user.id, name=body.name)
    db.add(folder)
    await db.commit()
    await db.refresh(folder)
    return FolderResponse(
        id=str(folder.id),
        user_id=str(folder.user_id),
        name=folder.name,
        created_at=folder.created_at,
    )


@router.get("/", response_model=SavedPapersResponse)
async def get_library(
    folder_id: str | None = Query(None),
    tag: str | None = Query(None),
    limit: int = Query(20, le=100),
    offset: int = Query(0, ge=0),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    query = (
        select(SavedPaper)
        .where(SavedPaper.user_id == current_user.id)
        .options(selectinload(SavedPaper.paper))
    )
    if folder_id:
        query = query.where(SavedPaper.folder_id == folder_id)
    if tag:
        query = query.where(SavedPaper.tags.contains([tag]))

    count_q = select(func.count()).select_from(query.subquery())
    total = (await db.execute(count_q)).scalar_one()

    query = query.order_by(SavedPaper.saved_at.desc()).offset(offset).limit(limit)
    result = await db.execute(query)
    saved = result.scalars().all()
    return SavedPapersResponse(items=[_saved_to_response(s) for s in saved], total=total)


@router.put("/{paper_id}", response_model=SavedPaperResponse)
async def update_saved_paper(
    paper_id: str,
    body: UpdateSavedPaperRequest,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    result = await db.execute(
        select(SavedPaper)
        .where(SavedPaper.user_id == current_user.id, SavedPaper.paper_id == paper_id)
        .options(selectinload(SavedPaper.paper))
    )
    sp = result.scalar_one_or_none()
    if not sp:
        raise HTTPException(
            status_code=404,
            detail={"error": "not_found", "detail": "Saved paper not found"},
        )
    if body.folder_id is not None:
        sp.folder_id = body.folder_id
    if body.tags is not None:
        sp.tags = body.tags
    if body.personal_note is not None:
        sp.personal_note = body.personal_note
    if body.is_read is not None:
        sp.is_read = body.is_read
    await db.commit()
    await db.refresh(sp)
    return _saved_to_response(sp)


@router.delete("/{paper_id}", status_code=204)
async def delete_saved_paper(
    paper_id: str,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    result = await db.execute(
        select(SavedPaper).where(
            SavedPaper.user_id == current_user.id,
            SavedPaper.paper_id == paper_id,
        )
    )
    sp = result.scalar_one_or_none()
    if sp:
        await db.delete(sp)
        feed_result = await db.execute(
            select(UserFeed).where(
                UserFeed.user_id == current_user.id,
                UserFeed.paper_id == paper_id,
            )
        )
        feed = feed_result.scalar_one_or_none()
        if feed:
            feed.is_saved = False
        await db.commit()
    return Response(status_code=204)

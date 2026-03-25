import asyncio
import logging
from fastapi import APIRouter, Depends, Query, HTTPException
from pydantic import BaseModel
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select
from app.core.database import get_db
from app.core.security import get_current_user
from app.models.users import User
from app.models.papers import Paper
from app.external import arxiv_client, pubmed_client
from app.external.arxiv_client import PaperData
from app.services.ai.embedding import generate_paper_embedding
from app.repositories.paper_repository import upsert_paper

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/discover", tags=["discover"])


@router.get("/search")
async def search_papers(
    q: str = Query(..., min_length=2, description="Search query"),
    source: str = Query("all", description="Source filter: all, arxiv, pubmed"),
    max_results: int = Query(15, ge=1, le=50),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """
    Search papers across arXiv and PubMed by keyword/topic/author.
    Returns results directly from external APIs (not from local DB).
    """
    keywords = [k.strip() for k in q.split(",") if k.strip()]
    if not keywords:
        keywords = [q.strip()]

    loop = asyncio.get_event_loop()
    all_papers = []

    if source in ("all", "arxiv"):
        try:
            arxiv_papers = await loop.run_in_executor(
                None, lambda: arxiv_client.fetch_papers(keywords, max_results=max_results)
            )
            all_papers.extend(arxiv_papers)
        except Exception as e:
            logger.warning("arXiv search failed for '%s': %s", q, e)

    if source in ("all", "pubmed"):
        try:
            pubmed_papers = await loop.run_in_executor(
                None, lambda: pubmed_client.fetch_papers(keywords, max_results=max_results)
            )
            all_papers.extend(pubmed_papers)
        except Exception as e:
            logger.warning("PubMed search failed for '%s': %s", q, e)

    results = []
    for p in all_papers:
        results.append({
            "external_id": p.external_id,
            "source": p.source,
            "title": p.title,
            "authors": p.authors,
            "abstract": p.abstract,
            "published_date": p.published_date.isoformat() if p.published_date else None,
            "pdf_url": p.pdf_url,
            "citation_count": p.citation_count,
            "venue": p.venue,
        })

    return {"query": q, "source": source, "total": len(results), "results": results}


class SavePaperRequest(BaseModel):
    external_id: str
    source: str
    title: str
    authors: list[str] = []
    abstract: str | None = None
    published_date: str | None = None
    pdf_url: str | None = None
    citation_count: int = 0
    venue: str | None = None


@router.post("/save")
async def save_discovered_paper(
    body: SavePaperRequest,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """
    Save a discovered paper to the DB so it gets a UUID.
    Returns the paper's DB id for navigation to PaperDetails.
    """
    # Check if already in DB
    result = await db.execute(
        select(Paper).where(Paper.external_id == body.external_id)
    )
    existing = result.scalar_one_or_none()
    if existing:
        return {"id": str(existing.id), "title": existing.title}

    # Parse date
    from datetime import date as dt_date
    pub_date = None
    if body.published_date:
        try:
            pub_date = dt_date.fromisoformat(body.published_date)
        except ValueError:
            pass

    paper_data = PaperData(
        external_id=body.external_id,
        source=body.source,
        title=body.title,
        authors=body.authors,
        abstract=body.abstract or "",
        published_date=pub_date,
        pdf_url=body.pdf_url,
        citation_count=body.citation_count,
        venue=body.venue,
    )

    loop = asyncio.get_event_loop()
    embedding = await loop.run_in_executor(
        None, lambda: generate_paper_embedding(body.title, body.abstract or "")
    )

    paper = await upsert_paper(db, paper_data, embedding)
    return {"id": str(paper.id), "title": paper.title}

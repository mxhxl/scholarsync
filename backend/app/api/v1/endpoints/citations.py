from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select
from app.core.database import get_db
from app.core.security import get_current_user
from app.models.users import User
from app.models.papers import Paper
from app.models.citations import Citation
from app.schemas.citation import CitationsResponse, CitationPaper, MustCiteResponse, CitationPaperWithScore
from app.services.citation_service import get_must_cite_papers
from app.external.semantic_scholar_client import fetch_citations

router = APIRouter(prefix="/citations", tags=["citations"])


def _paper_to_citation(p: Paper) -> CitationPaper:
    return CitationPaper(
        id=str(p.id),
        external_id=p.external_id,
        source=p.source,
        title=p.title,
        authors=p.authors or [],
        abstract=p.abstract,
        published_date=p.published_date,
        citation_count=p.citation_count or 0,
        venue=p.venue,
    )


@router.get("/must-cite", response_model=MustCiteResponse)
async def must_cite(
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    candidates = await get_must_cite_papers(db, current_user.id)
    papers = []
    for paper_id, score in candidates:
        result = await db.execute(select(Paper).where(Paper.id == paper_id))
        paper = result.scalar_one_or_none()
        if paper:
            papers.append(CitationPaperWithScore(
                id=str(paper.id),
                external_id=paper.external_id,
                source=paper.source,
                title=paper.title,
                authors=paper.authors or [],
                abstract=paper.abstract,
                published_date=paper.published_date,
                citation_count=paper.citation_count or 0,
                venue=paper.venue,
                pagerank_score=score,
            ))
    return MustCiteResponse(papers=papers)


@router.get("/{paper_id}", response_model=CitationsResponse)
async def get_citations(
    paper_id: str,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    paper_result = await db.execute(select(Paper).where(Paper.id == paper_id))
    paper = paper_result.scalar_one_or_none()
    if not paper:
        raise HTTPException(
            status_code=404,
            detail={"error": "not_found", "detail": "Paper not found"},
        )

    backward_result = await db.execute(
        select(Citation).where(Citation.citing_paper_id == paper_id)
    )
    backward_citations_ids = [c.cited_paper_id for c in backward_result.scalars().all()]

    forward_result = await db.execute(
        select(Citation).where(Citation.cited_paper_id == paper_id)
    )
    forward_citations_ids = [c.citing_paper_id for c in forward_result.scalars().all()]

    if not backward_citations_ids and not forward_citations_ids:
        data = await fetch_citations(paper.external_id)
        backward_citations_ids = []
        forward_citations_ids = []

    backward_papers = []
    for cid in backward_citations_ids[:20]:
        r = await db.execute(select(Paper).where(Paper.id == cid))
        p = r.scalar_one_or_none()
        if p:
            backward_papers.append(_paper_to_citation(p))

    forward_papers = []
    for cid in forward_citations_ids[:20]:
        r = await db.execute(select(Paper).where(Paper.id == cid))
        p = r.scalar_one_or_none()
        if p:
            forward_papers.append(_paper_to_citation(p))

    return CitationsResponse(
        paper=_paper_to_citation(paper),
        backward_citations=backward_papers,
        forward_citations=forward_papers,
    )

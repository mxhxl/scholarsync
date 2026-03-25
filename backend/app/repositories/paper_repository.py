import logging
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, text
from sqlalchemy.dialects.postgresql import insert as pg_insert
from app.models.papers import Paper
from app.external.arxiv_client import PaperData

logger = logging.getLogger(__name__)


async def upsert_paper(db: AsyncSession, paper_data: PaperData, embedding: list[float]) -> Paper:
    """Insert or update a paper by external_id."""
    result = await db.execute(
        select(Paper).where(Paper.external_id == paper_data.external_id)
    )
    existing = result.scalar_one_or_none()
    if existing:
        existing.title = paper_data.title
        existing.authors = paper_data.authors
        existing.abstract = paper_data.abstract
        existing.citation_count = paper_data.citation_count
        if embedding:
            existing.set_embedding(embedding)
        await db.commit()
        await db.refresh(existing)
        return existing

    paper = Paper(
        external_id=paper_data.external_id,
        source=paper_data.source,
        title=paper_data.title,
        authors=paper_data.authors,
        abstract=paper_data.abstract,
        published_date=paper_data.published_date,
        pdf_url=paper_data.pdf_url,
        citation_count=paper_data.citation_count,
        venue=paper_data.venue,
    )
    paper.set_embedding(embedding)
    db.add(paper)
    await db.commit()
    await db.refresh(paper)
    return paper


async def search_papers_fulltext(db: AsyncSession, query: str, user_id: str, limit: int = 20):
    """PostgreSQL full-text search on saved papers."""
    from app.models.saved_papers import SavedPaper
    result = await db.execute(
        select(SavedPaper).join(Paper).where(
            SavedPaper.user_id == user_id,
            text(
                "to_tsvector('english', papers.title || ' ' || COALESCE(papers.abstract, '')) "
                "@@ plainto_tsquery('english', :q)"
            ).bindparams(q=query)
        ).limit(limit)
    )
    return result.scalars().all()

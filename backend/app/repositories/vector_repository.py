import json
import logging
import numpy as np
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select
from app.models.papers import Paper

logger = logging.getLogger(__name__)


def _cosine_similarity(a: list[float], b: list[float]) -> float:
    va, vb = np.array(a), np.array(b)
    denom = np.linalg.norm(va) * np.linalg.norm(vb)
    if denom == 0:
        return 0.0
    return float(np.dot(va, vb) / denom)


async def find_similar_papers(
    db: AsyncSession,
    embedding: list[float],
    limit: int = 50,
    since_date=None,
) -> list[tuple]:
    """
    Find papers similar to the given embedding using Python cosine similarity.
    Returns list of (Paper, similarity_score) tuples sorted by score desc.
    Falls back gracefully when pgvector is not installed.
    """
    query = select(Paper).where(Paper.embedding.isnot(None))
    if since_date:
        query = query.where(Paper.published_date >= since_date)

    result = await db.execute(query)
    papers = result.scalars().all()

    scored = []
    for paper in papers:
        try:
            paper_vec = paper.get_embedding()
            if paper_vec is None:
                continue
            score = _cosine_similarity(embedding, paper_vec)
            scored.append((paper, score))
        except Exception as e:
            logger.warning("Skipping paper %s during similarity: %s", paper.id, e)

    scored.sort(key=lambda x: x[1], reverse=True)
    return scored[:limit]

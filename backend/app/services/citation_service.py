import logging
import networkx as nx
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select
from app.models.papers import Paper
from app.models.citations import Citation
from app.models.saved_papers import SavedPaper

logger = logging.getLogger(__name__)


async def build_citation_graph(db: AsyncSession, paper_ids: list[str]) -> nx.DiGraph:
    """Build a directed citation graph for the given paper IDs."""
    G = nx.DiGraph()
    G.add_nodes_from(paper_ids)

    result = await db.execute(
        select(Citation).where(
            Citation.citing_paper_id.in_(paper_ids) | Citation.cited_paper_id.in_(paper_ids)
        )
    )
    citations = result.scalars().all()
    for c in citations:
        G.add_edge(c.citing_paper_id, c.cited_paper_id)
    return G


async def get_must_cite_papers(db: AsyncSession, user_id: str, top_n: int = 10) -> list[tuple]:
    """
    Get user's saved papers, build citation graph, run PageRank,
    return top papers by PageRank not already in library.
    """
    result = await db.execute(
        select(SavedPaper.paper_id).where(SavedPaper.user_id == user_id)
    )
    saved_ids = [row[0] for row in result.fetchall()]
    if not saved_ids:
        return []

    G = await build_citation_graph(db, saved_ids)

    if len(G.nodes) < 2:
        return []

    pagerank = nx.pagerank(G, alpha=0.85)

    saved_set = set(saved_ids)
    candidates = [
        (node, score)
        for node, score in pagerank.items()
        if node not in saved_set
    ]
    candidates.sort(key=lambda x: x[1], reverse=True)
    return candidates[:top_n]

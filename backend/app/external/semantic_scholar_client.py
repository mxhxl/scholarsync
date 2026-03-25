import asyncio
import logging
import httpx
from app.config import settings
from app.external.arxiv_client import PaperData

logger = logging.getLogger(__name__)

S2_BASE = "https://api.semanticscholar.org/graph/v1"
HEADERS = {}
if settings.SEMANTIC_SCHOLAR_API_KEY:
    HEADERS["x-api-key"] = settings.SEMANTIC_SCHOLAR_API_KEY


async def fetch_citations(paper_id: str) -> dict:
    """Returns {backward: [paper_ids], forward: [paper_ids]}"""
    async with httpx.AsyncClient(headers=HEADERS, timeout=30) as client:
        try:
            resp = await client.get(
                f"{S2_BASE}/paper/{paper_id}/citations",
                params={"fields": "externalIds,title", "limit": 100},
            )
            resp.raise_for_status()
            citations_data = resp.json().get("data", [])
            backward = [
                c["citedPaper"].get("paperId", "")
                for c in citations_data
                if c.get("citedPaper")
            ]
        except Exception as e:
            logger.error("S2 citations error: %s", e)
            backward = []

        await asyncio.sleep(0.05)

        try:
            resp = await client.get(
                f"{S2_BASE}/paper/{paper_id}/references",
                params={"fields": "externalIds,title", "limit": 100},
            )
            resp.raise_for_status()
            refs_data = resp.json().get("data", [])
            forward = [
                r["citedPaper"].get("paperId", "")
                for r in refs_data
                if r.get("citedPaper")
            ]
        except Exception as e:
            logger.error("S2 references error: %s", e)
            forward = []

    return {"backward": backward, "forward": forward}


async def fetch_paper_metadata(external_ids: list[str]) -> list[PaperData]:
    """Fetch paper metadata from Semantic Scholar in batches."""
    results = []
    batch_size = 10
    async with httpx.AsyncClient(headers=HEADERS, timeout=30) as client:
        for i in range(0, len(external_ids), batch_size):
            batch = external_ids[i:i + batch_size]
            try:
                resp = await client.post(
                    f"{S2_BASE}/paper/batch",
                    params={"fields": "externalIds,title,authors,abstract,year,citationCount,venue"},
                    json={"ids": batch},
                )
                resp.raise_for_status()
                for item in resp.json():
                    if item is None:
                        continue
                    from datetime import date as date_type
                    year = item.get("year")
                    pub_date = date_type(year, 1, 1) if year else None
                    results.append(PaperData(
                        external_id=f"s2:{item.get('paperId', '')}",
                        source="semantic_scholar",
                        title=item.get("title", ""),
                        authors=[a.get("name", "") for a in (item.get("authors") or [])],
                        abstract=item.get("abstract", "") or "",
                        published_date=pub_date,
                        pdf_url=None,
                        citation_count=item.get("citationCount", 0) or 0,
                        venue=item.get("venue"),
                    ))
            except Exception as e:
                logger.error("S2 metadata batch error: %s", e)
            await asyncio.sleep(0.05)

    return results

import logging
import arxiv
from dataclasses import dataclass
from datetime import date
from app.config import settings

logger = logging.getLogger(__name__)


@dataclass
class PaperData:
    external_id: str
    source: str
    title: str
    authors: list[str]
    abstract: str
    published_date: date | None
    pdf_url: str | None
    citation_count: int
    venue: str | None


def fetch_papers(keywords: list[str], max_results: int = None) -> list[PaperData]:
    if max_results is None:
        max_results = settings.ARXIV_MAX_RESULTS

    query = " OR ".join(f'all:"{kw}"' for kw in keywords)
    client = arxiv.Client()
    search = arxiv.Search(
        query=query,
        max_results=max_results,
        sort_by=arxiv.SortCriterion.SubmittedDate,
        sort_order=arxiv.SortOrder.Descending,
    )

    papers = []
    try:
        for result in client.results(search):
            papers.append(PaperData(
                external_id=f"arxiv:{result.entry_id.split('/')[-1]}",
                source="arxiv",
                title=result.title,
                authors=[str(a) for a in result.authors],
                abstract=result.summary or "",
                published_date=result.published.date() if result.published else None,
                pdf_url=result.pdf_url,
                citation_count=0,
                venue=result.journal_ref,
            ))
    except Exception as e:
        logger.error("arXiv fetch error: %s", e)

    return papers

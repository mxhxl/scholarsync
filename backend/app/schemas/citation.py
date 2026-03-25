from pydantic import BaseModel
from datetime import date, datetime


class CitationPaper(BaseModel):
    id: str
    external_id: str
    source: str
    title: str
    authors: list[str] = []
    abstract: str | None = None
    published_date: date | None = None
    citation_count: int = 0
    venue: str | None = None


class CitationPaperWithScore(BaseModel):
    id: str
    external_id: str
    source: str
    title: str
    authors: list[str] = []
    abstract: str | None = None
    published_date: date | None = None
    citation_count: int = 0
    venue: str | None = None
    pagerank_score: float = 0.0


class CitationsResponse(BaseModel):
    paper: CitationPaper
    backward_citations: list[CitationPaper] = []
    forward_citations: list[CitationPaper] = []


class MustCiteResponse(BaseModel):
    papers: list[CitationPaperWithScore] = []

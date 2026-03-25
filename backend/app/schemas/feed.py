from pydantic import BaseModel
from datetime import date, datetime


class FeedPaper(BaseModel):
    id: str
    external_id: str
    source: str
    title: str
    authors: list[str] = []
    abstract: str | None = None
    published_date: date | None = None
    pdf_url: str | None = None
    citation_count: int = 0
    venue: str | None = None


class FeedItem(BaseModel):
    id: str
    paper_id: str
    relevance_score: float
    priority: str
    is_read: bool
    is_saved: bool
    created_at: datetime
    paper: FeedPaper


class FeedResponse(BaseModel):
    items: list[FeedItem] = []
    total: int
    has_more: bool

from pydantic import BaseModel
from datetime import date, datetime


class AlertPaper(BaseModel):
    id: str
    title: str
    authors: list[str] = []
    published_date: date | None = None
    source: str
    venue: str | None = None


class AlertResponse(BaseModel):
    id: str
    user_id: str
    paper_id: str
    type: str
    title: str
    description: str
    similarity_score: float
    comparison_report: dict | None = None
    is_read: bool
    is_acknowledged: bool
    created_at: datetime
    paper: AlertPaper


class AlertsResponse(BaseModel):
    items: list[AlertResponse] = []
    total: int


class UnreadCountResponse(BaseModel):
    count: int

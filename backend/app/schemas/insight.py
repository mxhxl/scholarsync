from pydantic import BaseModel
from datetime import datetime


class TrendResult(BaseModel):
    topic_id: int
    keywords: list[str] = []
    label: str
    papers_count: int
    growth_rate: float
    trend: str  # rising / declining / stable
    monthly_counts: list[int] = []


class TrendsResponse(BaseModel):
    trends: list[TrendResult] = []
    computed_at: datetime


class ProcessingResponse(BaseModel):
    status: str
    retry_after: int = 60

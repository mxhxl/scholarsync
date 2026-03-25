from pydantic import BaseModel, ConfigDict
from datetime import date, datetime


class PaperResponse(BaseModel):
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
    created_at: datetime


class SummaryResponse(BaseModel):
    model_config = ConfigDict(protected_namespaces=())
    paper_id: str
    purpose: str | None = None
    methodology: str | None = None
    key_results: str | None = None
    limitations: str | None = None
    relevance_to_field: str | None = None
    research_gaps: str | None = None
    status: str  # ready / generating / failed
    model_version: str | None = None
    generated_at: datetime | None = None


class SuggestionsResponse(BaseModel):
    paper_id: str
    research_directions: list[str] = []
    practical_applications: list[str] = []
    recommended_reading: list[str] = []
    key_takeaways: list[str] = []
    status: str  # ready / failed


class LitReviewEntry(BaseModel):
    ref_no: int = 0
    authors: str = ""
    year: str = ""
    title: str = ""
    methodology: str = ""
    key_findings: str = ""
    limitations: str = ""


class LiteratureReviewResponse(BaseModel):
    paper_id: str
    entries: list[LitReviewEntry] = []
    status: str  # ready / failed


class ProcessingResponse(BaseModel):
    status: str
    retry_after: int = 60

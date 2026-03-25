from pydantic import BaseModel
from datetime import date, datetime


class SavePaperRequest(BaseModel):
    paper_id: str
    folder_id: str | None = None
    tags: list[str] = []
    personal_note: str | None = None


class UpdateSavedPaperRequest(BaseModel):
    folder_id: str | None = None
    tags: list[str] | None = None
    personal_note: str | None = None
    is_read: bool | None = None


class SavedPaperPaper(BaseModel):
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


class SavedPaperResponse(BaseModel):
    id: str
    user_id: str
    paper_id: str
    folder_id: str | None = None
    tags: list[str] = []
    personal_note: str | None = None
    is_read: bool
    saved_at: datetime
    paper: SavedPaperPaper


class SavedPapersResponse(BaseModel):
    items: list[SavedPaperResponse] = []
    total: int


class FolderCreate(BaseModel):
    name: str


class FolderResponse(BaseModel):
    id: str
    user_id: str
    name: str
    created_at: datetime

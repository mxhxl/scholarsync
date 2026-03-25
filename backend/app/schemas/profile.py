from pydantic import BaseModel
from datetime import datetime


class ProfileSetupRequest(BaseModel):
    research_field: str
    topics: list[str] = []
    keywords: list[str] = []
    authors_following: list[str] = []


class ProfileUpdateRequest(BaseModel):
    research_field: str | None = None
    topics: list[str] | None = None
    keywords: list[str] | None = None
    authors_following: list[str] | None = None


class ProfileResponse(BaseModel):
    id: str
    user_id: str
    research_field: str
    topics: list[str] = []
    keywords: list[str] = []
    authors_following: list[str] = []
    created_at: datetime
    updated_at: datetime


class NotificationPreferencesUpdate(BaseModel):
    digest_time: str | None = None  # "HH:MM"
    overlap_sensitivity: str | None = None
    enable_high_priority: bool | None = None
    enable_overlap_alerts: bool | None = None
    enable_email: bool | None = None


class NotificationPreferencesResponse(BaseModel):
    id: str
    user_id: str
    digest_time: str
    overlap_sensitivity: str
    enable_high_priority: bool
    enable_overlap_alerts: bool
    enable_email: bool

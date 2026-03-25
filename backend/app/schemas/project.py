from pydantic import BaseModel
from datetime import datetime


class ProjectCreate(BaseModel):
    title: str
    description: str


class ProjectResponse(BaseModel):
    id: str
    user_id: str
    title: str
    description: str
    created_at: datetime
    updated_at: datetime

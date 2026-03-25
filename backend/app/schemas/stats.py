from pydantic import BaseModel


class UserStatsResponse(BaseModel):
    papers_read: int
    saved_papers: int
    summaries: int
    unread_alerts: int

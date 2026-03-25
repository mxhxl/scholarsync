from pydantic import BaseModel, EmailStr


# ── Dashboard ─────────────────────────────────────────────────────────

class AdminDashboardResponse(BaseModel):
    total_users: int
    active_users: int
    total_papers: int
    total_alerts: int
    total_saved_papers: int
    new_users_last_7_days: int
    new_papers_last_7_days: int


# ── User management ──────────────────────────────────────────────────

class AdminUserItem(BaseModel):
    id: str
    email: str
    full_name: str
    institution: str | None = None
    role: str
    is_active: bool
    is_verified: bool
    created_at: str
    papers_read: int = 0
    saved_papers: int = 0


class AdminUserListResponse(BaseModel):
    items: list[AdminUserItem]
    total: int


class AdminCreateUserRequest(BaseModel):
    email: EmailStr
    password: str
    full_name: str
    institution: str | None = None
    role: str = "user"  # "user" | "admin"


class AdminUserUpdateRequest(BaseModel):
    is_active: bool | None = None
    is_verified: bool | None = None
    role: str | None = None


# ── Paper management ─────────────────────────────────────────────────

class AdminPaperItem(BaseModel):
    id: str
    external_id: str
    source: str
    title: str
    authors: list[str]
    published_date: str | None = None
    citation_count: int
    venue: str | None = None
    has_summary: bool = False
    feed_count: int = 0
    saved_count: int = 0
    created_at: str


class AdminPaperListResponse(BaseModel):
    items: list[AdminPaperItem]
    total: int


# ── Alert management ─────────────────────────────────────────────────

class AdminAlertItem(BaseModel):
    id: str
    user_id: str
    user_email: str
    paper_id: str
    paper_title: str
    type: str
    title: str
    is_read: bool
    is_acknowledged: bool
    created_at: str


class AdminAlertListResponse(BaseModel):
    items: list[AdminAlertItem]
    total: int

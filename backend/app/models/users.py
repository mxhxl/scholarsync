import uuid
from datetime import datetime, timezone
from sqlalchemy import String, Boolean, DateTime
from sqlalchemy.orm import Mapped, mapped_column, relationship
from app.core.database import Base


class User(Base):
    __tablename__ = "users"

    id: Mapped[str] = mapped_column(String, primary_key=True, default=lambda: str(uuid.uuid4()))
    email: Mapped[str] = mapped_column(String, unique=True, index=True, nullable=False)
    hashed_password: Mapped[str] = mapped_column(String, nullable=False)
    full_name: Mapped[str] = mapped_column(String, nullable=False)
    institution: Mapped[str | None] = mapped_column(String, nullable=True)
    is_active: Mapped[bool] = mapped_column(Boolean, default=True)
    is_verified: Mapped[bool] = mapped_column(Boolean, default=False)
    role: Mapped[str] = mapped_column(String, default="user", server_default="user", nullable=True)  # "user" | "admin"
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), default=lambda: datetime.now(timezone.utc)
    )
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        default=lambda: datetime.now(timezone.utc),
        onupdate=lambda: datetime.now(timezone.utc),
    )

    research_profile = relationship("ResearchProfile", back_populates="user", uselist=False)
    current_project = relationship("CurrentProject", back_populates="user", uselist=False)
    feeds = relationship("UserFeed", back_populates="user")
    saved_papers = relationship("SavedPaper", back_populates="user")
    folders = relationship("Folder", back_populates="user")
    alerts = relationship("Alert", back_populates="user")
    device_tokens = relationship("DeviceToken", back_populates="user")
    login_activities = relationship("LoginActivity", back_populates="user")
    notification_preferences = relationship(
        "NotificationPreferences", back_populates="user", uselist=False
    )

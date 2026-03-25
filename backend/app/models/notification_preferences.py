import uuid
from sqlalchemy import String, Boolean, Time, ForeignKey
from sqlalchemy.orm import Mapped, mapped_column, relationship
from app.core.database import Base
import datetime


class NotificationPreferences(Base):
    __tablename__ = "notification_preferences"

    id: Mapped[str] = mapped_column(String, primary_key=True, default=lambda: str(uuid.uuid4()))
    user_id: Mapped[str] = mapped_column(
        String, ForeignKey("users.id", ondelete="CASCADE"), unique=True, nullable=False
    )
    digest_time: Mapped[datetime.time] = mapped_column(
        Time, default=datetime.time(9, 0)
    )
    overlap_sensitivity: Mapped[str] = mapped_column(String, default="medium")
    enable_high_priority: Mapped[bool] = mapped_column(Boolean, default=True)
    enable_overlap_alerts: Mapped[bool] = mapped_column(Boolean, default=True)
    enable_email: Mapped[bool] = mapped_column(Boolean, default=True)

    user = relationship("User", back_populates="notification_preferences")

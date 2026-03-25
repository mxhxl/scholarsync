import uuid
from datetime import datetime, date, timezone
from sqlalchemy import String, Integer, Date, DateTime, ForeignKey, UniqueConstraint, Index
from sqlalchemy.orm import Mapped, mapped_column, relationship
from app.core.database import Base


class ReadingStreak(Base):
    """Tracks a user's reading streak (Duolingo-style)."""
    __tablename__ = "reading_streaks"

    id: Mapped[str] = mapped_column(String, primary_key=True, default=lambda: str(uuid.uuid4()))
    user_id: Mapped[str] = mapped_column(
        String, ForeignKey("users.id", ondelete="CASCADE"), unique=True, nullable=False
    )
    current_streak: Mapped[int] = mapped_column(Integer, default=0)
    longest_streak: Mapped[int] = mapped_column(Integer, default=0)
    total_papers_read: Mapped[int] = mapped_column(Integer, default=0)
    total_xp: Mapped[int] = mapped_column(Integer, default=0)
    last_read_date: Mapped[date | None] = mapped_column(Date, nullable=True)
    streak_start_date: Mapped[date | None] = mapped_column(Date, nullable=True)
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), default=lambda: datetime.now(timezone.utc)
    )

    user = relationship("User", backref="reading_streak")


class ReadingEvent(Base):
    """Individual paper-read events for calendar/history tracking."""
    __tablename__ = "reading_events"

    id: Mapped[str] = mapped_column(String, primary_key=True, default=lambda: str(uuid.uuid4()))
    user_id: Mapped[str] = mapped_column(
        String, ForeignKey("users.id", ondelete="CASCADE"), nullable=False
    )
    paper_id: Mapped[str] = mapped_column(
        String, ForeignKey("papers.id", ondelete="CASCADE"), nullable=False
    )
    read_date: Mapped[date] = mapped_column(Date, nullable=False)
    xp_earned: Mapped[int] = mapped_column(Integer, default=10)
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), default=lambda: datetime.now(timezone.utc)
    )

    user = relationship("User", backref="reading_events")
    paper = relationship("Paper", backref="reading_events")

    __table_args__ = (
        UniqueConstraint("user_id", "paper_id", name="uq_reading_event_user_paper"),
        Index("ix_reading_events_user_date", "user_id", "read_date"),
    )

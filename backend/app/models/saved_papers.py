import uuid
from datetime import datetime, timezone
from sqlalchemy import String, Text, Boolean, DateTime, ForeignKey, UniqueConstraint, Index
from sqlalchemy.dialects.postgresql import ARRAY
from sqlalchemy.orm import Mapped, mapped_column, relationship
from app.core.database import Base


class SavedPaper(Base):
    __tablename__ = "saved_papers"

    id: Mapped[str] = mapped_column(String, primary_key=True, default=lambda: str(uuid.uuid4()))
    user_id: Mapped[str] = mapped_column(
        String, ForeignKey("users.id", ondelete="CASCADE"), nullable=False
    )
    paper_id: Mapped[str] = mapped_column(
        String, ForeignKey("papers.id", ondelete="CASCADE"), nullable=False
    )
    folder_id: Mapped[str | None] = mapped_column(
        String, ForeignKey("folders.id", ondelete="SET NULL"), nullable=True
    )
    tags: Mapped[list] = mapped_column(ARRAY(String), default=list)
    personal_note: Mapped[str | None] = mapped_column(Text, nullable=True)
    is_read: Mapped[bool] = mapped_column(Boolean, default=False)
    saved_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), default=lambda: datetime.now(timezone.utc)
    )

    user = relationship("User", back_populates="saved_papers")
    paper = relationship("Paper", back_populates="saved_by")
    folder = relationship("Folder", back_populates="saved_papers")

    __table_args__ = (
        UniqueConstraint("user_id", "paper_id", name="uq_saved_papers_user_paper"),
        Index("ix_saved_papers_user_folder", "user_id", "folder_id"),
    )

import uuid
import json
from datetime import datetime, date, timezone
from sqlalchemy import String, Integer, Text, Date, DateTime, Index
from sqlalchemy.dialects.postgresql import ARRAY
from sqlalchemy.orm import Mapped, mapped_column, relationship
from app.core.database import Base


class Paper(Base):
    __tablename__ = "papers"

    id: Mapped[str] = mapped_column(String, primary_key=True, default=lambda: str(uuid.uuid4()))
    external_id: Mapped[str] = mapped_column(String, unique=True, nullable=False, index=True)
    source: Mapped[str] = mapped_column(String, nullable=False)
    title: Mapped[str] = mapped_column(String, nullable=False)
    authors: Mapped[list] = mapped_column(ARRAY(String), default=list)
    abstract: Mapped[str | None] = mapped_column(Text, nullable=True)
    published_date: Mapped[date | None] = mapped_column(Date, nullable=True, index=True)
    pdf_url: Mapped[str | None] = mapped_column(String, nullable=True)
    citation_count: Mapped[int] = mapped_column(Integer, default=0)
    venue: Mapped[str | None] = mapped_column(String, nullable=True)
    # Stored as JSON text — swap for Vector(768) once pgvector is installed
    embedding: Mapped[str | None] = mapped_column(Text, nullable=True)
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), default=lambda: datetime.now(timezone.utc)
    )

    summary = relationship("PaperSummary", back_populates="paper", uselist=False)
    feeds = relationship("UserFeed", back_populates="paper")
    saved_by = relationship("SavedPaper", back_populates="paper")
    alerts = relationship("Alert", back_populates="paper")
    citing = relationship(
        "Citation", foreign_keys="Citation.citing_paper_id", back_populates="citing_paper"
    )
    cited_by = relationship(
        "Citation", foreign_keys="Citation.cited_paper_id", back_populates="cited_paper"
    )

    __table_args__ = (
        Index("ix_papers_published_date_desc", "published_date"),
    )

    def get_embedding(self) -> list[float] | None:
        if self.embedding is None:
            return None
        return json.loads(self.embedding)

    def set_embedding(self, vec: list[float]) -> None:
        self.embedding = json.dumps(vec)

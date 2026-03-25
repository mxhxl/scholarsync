import uuid
from datetime import datetime, timezone
from sqlalchemy import String, DateTime, ForeignKey, UniqueConstraint
from sqlalchemy.orm import Mapped, mapped_column, relationship
from app.core.database import Base


class Citation(Base):
    __tablename__ = "citations"

    id: Mapped[str] = mapped_column(String, primary_key=True, default=lambda: str(uuid.uuid4()))
    citing_paper_id: Mapped[str] = mapped_column(
        String, ForeignKey("papers.id", ondelete="CASCADE"), nullable=False
    )
    cited_paper_id: Mapped[str] = mapped_column(
        String, ForeignKey("papers.id", ondelete="CASCADE"), nullable=False
    )
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), default=lambda: datetime.now(timezone.utc)
    )

    citing_paper = relationship("Paper", foreign_keys=[citing_paper_id], back_populates="citing")
    cited_paper = relationship("Paper", foreign_keys=[cited_paper_id], back_populates="cited_by")

    __table_args__ = (
        UniqueConstraint("citing_paper_id", "cited_paper_id", name="uq_citations_citing_cited"),
    )

import uuid
import json
from datetime import datetime, timezone
from sqlalchemy import String, DateTime, ForeignKey, Text
from sqlalchemy.dialects.postgresql import ARRAY
from sqlalchemy.orm import Mapped, mapped_column, relationship
from app.core.database import Base


class ResearchProfile(Base):
    __tablename__ = "research_profiles"

    id: Mapped[str] = mapped_column(String, primary_key=True, default=lambda: str(uuid.uuid4()))
    user_id: Mapped[str] = mapped_column(
        String, ForeignKey("users.id", ondelete="CASCADE"), unique=True, nullable=False
    )
    research_field: Mapped[str] = mapped_column(String, nullable=False)
    topics: Mapped[list] = mapped_column(ARRAY(String), default=list)
    keywords: Mapped[list] = mapped_column(ARRAY(String), default=list)
    authors_following: Mapped[list] = mapped_column(ARRAY(String), default=list)
    # Stored as JSON text — swap for Vector(768) once pgvector is installed
    embedding: Mapped[str | None] = mapped_column(Text, nullable=True)
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), default=lambda: datetime.now(timezone.utc)
    )
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        default=lambda: datetime.now(timezone.utc),
        onupdate=lambda: datetime.now(timezone.utc),
    )

    user = relationship("User", back_populates="research_profile")

    def get_embedding(self) -> list[float] | None:
        if self.embedding is None:
            return None
        return json.loads(self.embedding)

    def set_embedding(self, vec: list[float]) -> None:
        self.embedding = json.dumps(vec)

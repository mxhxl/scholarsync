"""Add reading_streaks and reading_events tables

Revision ID: 003
Revises: 002
Create Date: 2026-03-15

"""
from typing import Sequence, Union
from alembic import op
import sqlalchemy as sa

revision: str = "003"
down_revision: Union[str, None] = "002"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.create_table(
        "reading_streaks",
        sa.Column("id", sa.String(), nullable=False),
        sa.Column("user_id", sa.String(), sa.ForeignKey("users.id", ondelete="CASCADE"), nullable=False),
        sa.Column("current_streak", sa.Integer(), nullable=False, server_default="0"),
        sa.Column("longest_streak", sa.Integer(), nullable=False, server_default="0"),
        sa.Column("total_papers_read", sa.Integer(), nullable=False, server_default="0"),
        sa.Column("total_xp", sa.Integer(), nullable=False, server_default="0"),
        sa.Column("last_read_date", sa.Date(), nullable=True),
        sa.Column("streak_start_date", sa.Date(), nullable=True),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False, server_default=sa.func.now()),
        sa.PrimaryKeyConstraint("id"),
        sa.UniqueConstraint("user_id", name="uq_reading_streaks_user"),
    )

    op.create_table(
        "reading_events",
        sa.Column("id", sa.String(), nullable=False),
        sa.Column("user_id", sa.String(), sa.ForeignKey("users.id", ondelete="CASCADE"), nullable=False),
        sa.Column("paper_id", sa.String(), sa.ForeignKey("papers.id", ondelete="CASCADE"), nullable=False),
        sa.Column("read_date", sa.Date(), nullable=False),
        sa.Column("xp_earned", sa.Integer(), nullable=False, server_default="10"),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False, server_default=sa.func.now()),
        sa.PrimaryKeyConstraint("id"),
        sa.UniqueConstraint("user_id", "paper_id", name="uq_reading_event_user_paper"),
    )
    op.create_index("ix_reading_events_user_date", "reading_events", ["user_id", "read_date"])


def downgrade() -> None:
    op.drop_index("ix_reading_events_user_date", table_name="reading_events")
    op.drop_table("reading_events")
    op.drop_table("reading_streaks")

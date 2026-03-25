"""Initial schema

Revision ID: 001
Revises:
Create Date: 2026-03-07

"""
from typing import Sequence, Union
from alembic import op
import sqlalchemy as sa
from sqlalchemy.dialects import postgresql

revision: str = "001"
down_revision: Union[str, None] = None
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    # users
    op.create_table(
        "users",
        sa.Column("id", sa.String(), nullable=False),
        sa.Column("email", sa.String(), nullable=False),
        sa.Column("hashed_password", sa.String(), nullable=False),
        sa.Column("full_name", sa.String(), nullable=False),
        sa.Column("institution", sa.String(), nullable=True),
        sa.Column("is_active", sa.Boolean(), nullable=False, server_default="true"),
        sa.Column("is_verified", sa.Boolean(), nullable=False, server_default="false"),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False, server_default=sa.text("NOW()")),
        sa.Column("updated_at", sa.DateTime(timezone=True), nullable=False, server_default=sa.text("NOW()")),
        sa.PrimaryKeyConstraint("id"),
    )
    op.create_index("ix_users_email", "users", ["email"], unique=True)

    # research_profiles (embedding stored as JSON text)
    op.create_table(
        "research_profiles",
        sa.Column("id", sa.String(), nullable=False),
        sa.Column("user_id", sa.String(), sa.ForeignKey("users.id", ondelete="CASCADE"), nullable=False),
        sa.Column("research_field", sa.String(), nullable=False),
        sa.Column("topics", postgresql.ARRAY(sa.String()), nullable=True),
        sa.Column("keywords", postgresql.ARRAY(sa.String()), nullable=True),
        sa.Column("authors_following", postgresql.ARRAY(sa.String()), nullable=True),
        sa.Column("embedding", sa.Text(), nullable=True),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False, server_default=sa.text("NOW()")),
        sa.Column("updated_at", sa.DateTime(timezone=True), nullable=False, server_default=sa.text("NOW()")),
        sa.PrimaryKeyConstraint("id"),
        sa.UniqueConstraint("user_id"),
    )

    # papers (embedding stored as JSON text)
    op.create_table(
        "papers",
        sa.Column("id", sa.String(), nullable=False),
        sa.Column("external_id", sa.String(), nullable=False),
        sa.Column("source", sa.String(), nullable=False),
        sa.Column("title", sa.String(), nullable=False),
        sa.Column("authors", postgresql.ARRAY(sa.String()), nullable=True),
        sa.Column("abstract", sa.Text(), nullable=True),
        sa.Column("published_date", sa.Date(), nullable=True),
        sa.Column("pdf_url", sa.String(), nullable=True),
        sa.Column("citation_count", sa.Integer(), nullable=False, server_default="0"),
        sa.Column("venue", sa.String(), nullable=True),
        sa.Column("embedding", sa.Text(), nullable=True),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False, server_default=sa.text("NOW()")),
        sa.PrimaryKeyConstraint("id"),
    )
    op.create_index("ix_papers_external_id", "papers", ["external_id"], unique=True)
    op.create_index("ix_papers_published_date", "papers", ["published_date"])
    op.execute(
        "CREATE INDEX ix_papers_fulltext ON papers "
        "USING gin(to_tsvector('english', title || ' ' || COALESCE(abstract, '')))"
    )

    # paper_summaries
    op.create_table(
        "paper_summaries",
        sa.Column("id", sa.String(), nullable=False),
        sa.Column("paper_id", sa.String(), sa.ForeignKey("papers.id", ondelete="CASCADE"), nullable=False),
        sa.Column("purpose", sa.Text(), nullable=True),
        sa.Column("methodology", sa.Text(), nullable=True),
        sa.Column("key_results", sa.Text(), nullable=True),
        sa.Column("limitations", sa.Text(), nullable=True),
        sa.Column("relevance_to_field", sa.Text(), nullable=True),
        sa.Column("model_version", sa.String(), nullable=True),
        sa.Column("status", sa.String(), nullable=False, server_default="generating"),
        sa.Column("failed_at", sa.DateTime(timezone=True), nullable=True),
        sa.Column("retry_count", sa.Integer(), nullable=False, server_default="0"),
        sa.Column("generated_at", sa.DateTime(timezone=True), nullable=False, server_default=sa.text("NOW()")),
        sa.PrimaryKeyConstraint("id"),
        sa.UniqueConstraint("paper_id"),
    )

    # folders
    op.create_table(
        "folders",
        sa.Column("id", sa.String(), nullable=False),
        sa.Column("user_id", sa.String(), sa.ForeignKey("users.id", ondelete="CASCADE"), nullable=False),
        sa.Column("name", sa.String(), nullable=False),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False, server_default=sa.text("NOW()")),
        sa.PrimaryKeyConstraint("id"),
    )

    # saved_papers
    op.create_table(
        "saved_papers",
        sa.Column("id", sa.String(), nullable=False),
        sa.Column("user_id", sa.String(), sa.ForeignKey("users.id", ondelete="CASCADE"), nullable=False),
        sa.Column("paper_id", sa.String(), sa.ForeignKey("papers.id", ondelete="CASCADE"), nullable=False),
        sa.Column("folder_id", sa.String(), sa.ForeignKey("folders.id", ondelete="SET NULL"), nullable=True),
        sa.Column("tags", postgresql.ARRAY(sa.String()), nullable=True),
        sa.Column("personal_note", sa.Text(), nullable=True),
        sa.Column("is_read", sa.Boolean(), nullable=False, server_default="false"),
        sa.Column("saved_at", sa.DateTime(timezone=True), nullable=False, server_default=sa.text("NOW()")),
        sa.PrimaryKeyConstraint("id"),
        sa.UniqueConstraint("user_id", "paper_id", name="uq_saved_papers_user_paper"),
    )
    op.create_index("ix_saved_papers_user_folder", "saved_papers", ["user_id", "folder_id"])

    # user_feeds
    op.create_table(
        "user_feeds",
        sa.Column("id", sa.String(), nullable=False),
        sa.Column("user_id", sa.String(), sa.ForeignKey("users.id", ondelete="CASCADE"), nullable=False),
        sa.Column("paper_id", sa.String(), sa.ForeignKey("papers.id", ondelete="CASCADE"), nullable=False),
        sa.Column("relevance_score", sa.Float(), nullable=False, server_default="0.0"),
        sa.Column("priority", sa.String(), nullable=False, server_default="medium"),
        sa.Column("is_read", sa.Boolean(), nullable=False, server_default="false"),
        sa.Column("is_saved", sa.Boolean(), nullable=False, server_default="false"),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False, server_default=sa.text("NOW()")),
        sa.PrimaryKeyConstraint("id"),
        sa.UniqueConstraint("user_id", "paper_id", name="uq_user_feeds_user_paper"),
    )
    op.create_index("ix_user_feeds_user_created", "user_feeds", ["user_id", "created_at"])

    # current_projects (embedding stored as JSON text)
    op.create_table(
        "current_projects",
        sa.Column("id", sa.String(), nullable=False),
        sa.Column("user_id", sa.String(), sa.ForeignKey("users.id", ondelete="CASCADE"), nullable=False),
        sa.Column("title", sa.String(), nullable=False),
        sa.Column("description", sa.Text(), nullable=False),
        sa.Column("embedding", sa.Text(), nullable=True),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False, server_default=sa.text("NOW()")),
        sa.Column("updated_at", sa.DateTime(timezone=True), nullable=False, server_default=sa.text("NOW()")),
        sa.PrimaryKeyConstraint("id"),
        sa.UniqueConstraint("user_id"),
    )

    # alerts
    op.create_table(
        "alerts",
        sa.Column("id", sa.String(), nullable=False),
        sa.Column("user_id", sa.String(), sa.ForeignKey("users.id", ondelete="CASCADE"), nullable=False),
        sa.Column("paper_id", sa.String(), sa.ForeignKey("papers.id", ondelete="CASCADE"), nullable=False),
        sa.Column("type", sa.String(), nullable=False),
        sa.Column("title", sa.String(), nullable=False),
        sa.Column("description", sa.Text(), nullable=False),
        sa.Column("similarity_score", sa.Float(), nullable=False),
        sa.Column("comparison_report", postgresql.JSONB(), nullable=True),
        sa.Column("is_read", sa.Boolean(), nullable=False, server_default="false"),
        sa.Column("is_acknowledged", sa.Boolean(), nullable=False, server_default="false"),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False, server_default=sa.text("NOW()")),
        sa.PrimaryKeyConstraint("id"),
        sa.UniqueConstraint("user_id", "paper_id", "type", name="uq_alerts_user_paper_type"),
    )
    op.create_index("ix_alerts_user_read_created", "alerts", ["user_id", "is_read", "created_at"])

    # citations
    op.create_table(
        "citations",
        sa.Column("id", sa.String(), nullable=False),
        sa.Column("citing_paper_id", sa.String(), sa.ForeignKey("papers.id", ondelete="CASCADE"), nullable=False),
        sa.Column("cited_paper_id", sa.String(), sa.ForeignKey("papers.id", ondelete="CASCADE"), nullable=False),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False, server_default=sa.text("NOW()")),
        sa.PrimaryKeyConstraint("id"),
        sa.UniqueConstraint("citing_paper_id", "cited_paper_id", name="uq_citations_citing_cited"),
    )

    # device_tokens
    op.create_table(
        "device_tokens",
        sa.Column("id", sa.String(), nullable=False),
        sa.Column("user_id", sa.String(), sa.ForeignKey("users.id", ondelete="CASCADE"), nullable=False),
        sa.Column("token", sa.String(), nullable=False),
        sa.Column("platform", sa.String(), nullable=False),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False, server_default=sa.text("NOW()")),
        sa.PrimaryKeyConstraint("id"),
        sa.UniqueConstraint("token"),
    )

    # notification_preferences
    op.create_table(
        "notification_preferences",
        sa.Column("id", sa.String(), nullable=False),
        sa.Column("user_id", sa.String(), sa.ForeignKey("users.id", ondelete="CASCADE"), nullable=False),
        sa.Column("digest_time", sa.Time(), nullable=False, server_default="09:00"),
        sa.Column("overlap_sensitivity", sa.String(), nullable=False, server_default="medium"),
        sa.Column("enable_high_priority", sa.Boolean(), nullable=False, server_default="true"),
        sa.Column("enable_overlap_alerts", sa.Boolean(), nullable=False, server_default="true"),
        sa.Column("enable_email", sa.Boolean(), nullable=False, server_default="true"),
        sa.PrimaryKeyConstraint("id"),
        sa.UniqueConstraint("user_id"),
    )

    # trend_cache
    op.create_table(
        "trend_cache",
        sa.Column("id", sa.String(), nullable=False),
        sa.Column("results_json", sa.Text(), nullable=False),
        sa.Column("computed_at", sa.DateTime(timezone=True), nullable=False, server_default=sa.text("NOW()")),
        sa.Column("expires_at", sa.DateTime(timezone=True), nullable=False),
        sa.PrimaryKeyConstraint("id"),
    )


def downgrade() -> None:
    op.drop_table("trend_cache")
    op.drop_table("notification_preferences")
    op.drop_table("device_tokens")
    op.drop_table("citations")
    op.drop_table("alerts")
    op.drop_table("current_projects")
    op.drop_table("user_feeds")
    op.drop_table("saved_papers")
    op.drop_table("folders")
    op.drop_table("paper_summaries")
    op.drop_table("papers")
    op.drop_table("research_profiles")
    op.drop_table("users")

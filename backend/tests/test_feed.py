import pytest
from httpx import AsyncClient
from sqlalchemy.ext.asyncio import AsyncSession
from app.models.papers import Paper
from app.models.user_feeds import UserFeed
from app.models.users import User
from sqlalchemy import select
import uuid
from datetime import datetime, timezone

pytestmark = pytest.mark.asyncio


async def _insert_paper(db: AsyncSession) -> Paper:
    paper = Paper(
        id=str(uuid.uuid4()),
        external_id=f"arxiv:{uuid.uuid4()}",
        source="arxiv",
        title="Test Paper on ML",
        authors=["Author One"],
        abstract="This paper tests machine learning concepts.",
        citation_count=0,
    )
    db.add(paper)
    await db.commit()
    await db.refresh(paper)
    return paper


async def _get_user_id(db: AsyncSession, email: str) -> str:
    result = await db.execute(select(User).where(User.email == email))
    user = result.scalar_one()
    return str(user.id)


async def test_get_feed_empty(client: AsyncClient, auth_headers: dict):
    resp = await client.get("/v1/feed/", headers=auth_headers)
    assert resp.status_code == 200
    data = resp.json()
    assert data["items"] == []
    assert data["total"] == 0


async def test_get_feed_with_items(
    client: AsyncClient, auth_headers: dict, db_session: AsyncSession
):
    user_id = await _get_user_id(db_session, "test@scholarsync.test")
    paper = await _insert_paper(db_session)
    feed_item = UserFeed(
        user_id=user_id,
        paper_id=paper.id,
        relevance_score=0.85,
        priority="high",
        created_at=datetime.now(timezone.utc),
    )
    db_session.add(feed_item)
    await db_session.commit()

    resp = await client.get("/v1/feed/", headers=auth_headers)
    assert resp.status_code == 200
    data = resp.json()
    assert data["total"] >= 1
    assert any(item["priority"] == "high" for item in data["items"])


async def test_feed_filter_high_priority(
    client: AsyncClient, auth_headers: dict, db_session: AsyncSession
):
    user_id = await _get_user_id(db_session, "test@scholarsync.test")
    paper = await _insert_paper(db_session)
    feed_item = UserFeed(
        user_id=user_id,
        paper_id=paper.id,
        relevance_score=0.65,
        priority="medium",
        created_at=datetime.now(timezone.utc),
    )
    db_session.add(feed_item)
    await db_session.commit()

    resp = await client.get("/v1/feed/?filter=high_priority", headers=auth_headers)
    assert resp.status_code == 200
    for item in resp.json()["items"]:
        assert item["priority"] == "high"


async def test_dismiss_paper(
    client: AsyncClient, auth_headers: dict, db_session: AsyncSession
):
    user_id = await _get_user_id(db_session, "test@scholarsync.test")
    paper = await _insert_paper(db_session)
    feed_item = UserFeed(
        user_id=user_id,
        paper_id=paper.id,
        relevance_score=0.75,
        priority="high",
        created_at=datetime.now(timezone.utc),
    )
    db_session.add(feed_item)
    await db_session.commit()

    resp = await client.post(f"/v1/feed/{paper.id}/dismiss", headers=auth_headers)
    assert resp.status_code == 204


async def test_mark_read(
    client: AsyncClient, auth_headers: dict, db_session: AsyncSession
):
    user_id = await _get_user_id(db_session, "test@scholarsync.test")
    paper = await _insert_paper(db_session)
    feed_item = UserFeed(
        user_id=user_id,
        paper_id=paper.id,
        relevance_score=0.80,
        priority="high",
        created_at=datetime.now(timezone.utc),
    )
    db_session.add(feed_item)
    await db_session.commit()

    resp = await client.post(f"/v1/feed/{paper.id}/mark-read", headers=auth_headers)
    assert resp.status_code == 204

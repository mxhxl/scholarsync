import pytest
from unittest.mock import patch
import numpy as np
from httpx import AsyncClient
from sqlalchemy.ext.asyncio import AsyncSession
from app.models.papers import Paper
from app.models.alerts import Alert
from app.models.users import User
from sqlalchemy import select
import uuid
from datetime import datetime, timezone

pytestmark = pytest.mark.asyncio


async def _get_user_id(db: AsyncSession, email: str) -> str:
    result = await db.execute(select(User).where(User.email == email))
    user = result.scalar_one()
    return str(user.id)


async def _insert_paper(db: AsyncSession) -> Paper:
    paper = Paper(
        id=str(uuid.uuid4()),
        external_id=f"arxiv:{uuid.uuid4()}",
        source="arxiv",
        title="Related Research Paper",
        authors=["Researcher X"],
        abstract="This paper covers topics very close to the researcher's work.",
        citation_count=5,
    )
    db.add(paper)
    await db.commit()
    await db.refresh(paper)
    return paper


async def test_create_project(client: AsyncClient, auth_headers: dict):
    with patch("app.services.ai.embedding.get_scibert_model") as m:
        inst = m.return_value
        inst.encode.return_value = np.random.rand(768).astype("float32")
        resp = await client.post(
            "/v1/projects/current",
            headers=auth_headers,
            json={
                "title": "PhD Thesis on NLP",
                "description": "Exploring transformer models for low-resource language processing.",
            },
        )
    assert resp.status_code == 201
    data = resp.json()
    assert data["title"] == "PhD Thesis on NLP"
    assert "id" in data


async def test_unread_count_empty(client: AsyncClient, auth_headers: dict):
    resp = await client.get("/v1/alerts/unread-count", headers=auth_headers)
    assert resp.status_code == 200
    assert resp.json()["count"] == 0


async def test_unread_count_with_alerts(
    client: AsyncClient, auth_headers: dict, db_session: AsyncSession
):
    user_id = await _get_user_id(db_session, "test@scholarsync.test")
    paper = await _insert_paper(db_session)
    alert = Alert(
        user_id=user_id,
        paper_id=paper.id,
        type="overlap_high",
        title="High Overlap Detected",
        description="75% similarity to your project.",
        similarity_score=0.75,
        is_read=False,
        created_at=datetime.now(timezone.utc),
    )
    db_session.add(alert)
    await db_session.commit()

    resp = await client.get("/v1/alerts/unread-count", headers=auth_headers)
    assert resp.status_code == 200
    assert resp.json()["count"] >= 1


async def test_get_alerts(
    client: AsyncClient, auth_headers: dict, db_session: AsyncSession
):
    user_id = await _get_user_id(db_session, "test@scholarsync.test")
    paper = await _insert_paper(db_session)
    alert = Alert(
        user_id=user_id,
        paper_id=paper.id,
        type="overlap_moderate",
        title="Moderate Overlap",
        description="65% similarity.",
        similarity_score=0.65,
        created_at=datetime.now(timezone.utc),
    )
    db_session.add(alert)
    await db_session.commit()

    resp = await client.get("/v1/alerts/", headers=auth_headers)
    assert resp.status_code == 200
    data = resp.json()
    assert data["total"] >= 1


async def test_mark_alert_read(
    client: AsyncClient, auth_headers: dict, db_session: AsyncSession
):
    user_id = await _get_user_id(db_session, "test@scholarsync.test")
    paper = await _insert_paper(db_session)
    alert = Alert(
        user_id=user_id,
        paper_id=paper.id,
        type="must_cite",
        title="Must Cite",
        description="Important citation.",
        similarity_score=0.70,
        created_at=datetime.now(timezone.utc),
    )
    db_session.add(alert)
    await db_session.commit()

    resp = await client.post(f"/v1/alerts/{alert.id}/read", headers=auth_headers)
    assert resp.status_code == 204


async def test_acknowledge_alert(
    client: AsyncClient, auth_headers: dict, db_session: AsyncSession
):
    user_id = await _get_user_id(db_session, "test@scholarsync.test")
    paper = await _insert_paper(db_session)
    alert = Alert(
        user_id=user_id,
        paper_id=paper.id,
        type="overlap_critical",
        title="Critical Overlap",
        description="80% similarity.",
        similarity_score=0.82,
        created_at=datetime.now(timezone.utc),
    )
    db_session.add(alert)
    await db_session.commit()

    resp = await client.post(f"/v1/alerts/{alert.id}/acknowledge", headers=auth_headers)
    assert resp.status_code == 204

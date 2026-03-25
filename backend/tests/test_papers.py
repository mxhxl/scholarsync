import pytest
from unittest.mock import patch, AsyncMock
from httpx import AsyncClient
from sqlalchemy.ext.asyncio import AsyncSession
from app.models.papers import Paper
import uuid

pytestmark = pytest.mark.asyncio


async def _insert_paper(db: AsyncSession) -> Paper:
    paper = Paper(
        id=str(uuid.uuid4()),
        external_id=f"arxiv:{uuid.uuid4()}",
        source="arxiv",
        title="Deep Learning for NLP",
        authors=["Alice Smith", "Bob Jones"],
        abstract="We propose a novel transformer architecture for NLP tasks achieving 95% accuracy.",
        citation_count=150,
    )
    db.add(paper)
    await db.commit()
    await db.refresh(paper)
    return paper


async def test_get_paper(client: AsyncClient, auth_headers: dict, db_session: AsyncSession):
    paper = await _insert_paper(db_session)
    resp = await client.get(f"/v1/papers/{paper.id}", headers=auth_headers)
    assert resp.status_code == 200
    data = resp.json()
    assert data["id"] == str(paper.id)
    assert data["title"] == "Deep Learning for NLP"
    assert isinstance(data["authors"], list)


async def test_get_paper_not_found(client: AsyncClient, auth_headers: dict):
    resp = await client.get("/v1/papers/nonexistent-id", headers=auth_headers)
    assert resp.status_code == 404
    assert resp.json()["error"] == "not_found"


async def test_get_summary_generates_and_caches(
    client: AsyncClient, auth_headers: dict, db_session: AsyncSession
):
    paper = await _insert_paper(db_session)

    with patch("app.api.v1.endpoints.papers.generate_paper_summary") as mock_summarize:
        mock_summarize.return_value = {
            "purpose": "Test purpose",
            "methodology": "Test methodology",
            "key_results": "95% accuracy on benchmark",
            "limitations": "Limited to English text",
            "relevance_to_field": "Advances NLP research",
        }

        resp = await client.get(f"/v1/papers/{paper.id}/summary", headers=auth_headers)
        assert resp.status_code == 200
        data = resp.json()
        assert data["status"] == "ready"
        assert data["purpose"] == "Test purpose"
        assert data["key_results"] == "95% accuracy on benchmark"

        # Second call should return cached result (mock not called again)
        mock_summarize.reset_mock()
        resp2 = await client.get(f"/v1/papers/{paper.id}/summary", headers=auth_headers)
        assert resp2.status_code == 200
        mock_summarize.assert_not_called()


async def test_get_summary_paper_not_found(client: AsyncClient, auth_headers: dict):
    resp = await client.get("/v1/papers/bad-id/summary", headers=auth_headers)
    assert resp.status_code == 404

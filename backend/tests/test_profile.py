import pytest
from unittest.mock import patch
import numpy as np
from httpx import AsyncClient

pytestmark = pytest.mark.asyncio


@pytest.fixture
def mock_embed():
    with patch("app.services.ai.embedding.get_scibert_model") as m:
        inst = m.return_value
        inst.encode.return_value = np.random.rand(768).astype("float32")
        yield m


async def test_profile_setup(client: AsyncClient, auth_headers: dict, mock_embed):
    resp = await client.post(
        "/v1/profile/setup",
        headers=auth_headers,
        json={
            "research_field": "Machine Learning",
            "topics": ["NLP", "transformers"],
            "keywords": ["BERT", "attention"],
            "authors_following": ["Yann LeCun"],
        },
    )
    assert resp.status_code == 201
    data = resp.json()
    assert data["research_field"] == "Machine Learning"
    assert data["topics"] == ["NLP", "transformers"]
    assert data["keywords"] == ["BERT", "attention"]
    assert "id" in data


async def test_get_profile(client: AsyncClient, auth_headers: dict, mock_embed):
    await client.post(
        "/v1/profile/setup",
        headers=auth_headers,
        json={"research_field": "Bioinformatics", "topics": ["genomics"], "keywords": ["DNA"]},
    )
    resp = await client.get("/v1/profile/", headers=auth_headers)
    assert resp.status_code == 200
    data = resp.json()
    assert data["research_field"] == "Bioinformatics"


async def test_update_profile(client: AsyncClient, auth_headers: dict, mock_embed):
    await client.post(
        "/v1/profile/setup",
        headers=auth_headers,
        json={"research_field": "Physics", "topics": ["quantum"], "keywords": ["entanglement"]},
    )
    resp = await client.put(
        "/v1/profile/",
        headers=auth_headers,
        json={"research_field": "Quantum Computing", "topics": ["qubits", "gates"]},
    )
    assert resp.status_code == 200
    assert resp.json()["research_field"] == "Quantum Computing"
    assert "qubits" in resp.json()["topics"]


async def test_profile_not_found(client: AsyncClient, auth_headers: dict):
    resp = await client.get("/v1/profile/", headers=auth_headers)
    assert resp.status_code == 404


async def test_get_preferences(client: AsyncClient, auth_headers: dict, mock_embed):
    await client.post(
        "/v1/profile/setup",
        headers=auth_headers,
        json={"research_field": "CS", "topics": [], "keywords": []},
    )
    resp = await client.get("/v1/profile/preferences", headers=auth_headers)
    assert resp.status_code == 200
    data = resp.json()
    assert "digest_time" in data
    assert "overlap_sensitivity" in data


async def test_update_preferences(client: AsyncClient, auth_headers: dict, mock_embed):
    await client.post(
        "/v1/profile/setup",
        headers=auth_headers,
        json={"research_field": "CS", "topics": [], "keywords": []},
    )
    resp = await client.put(
        "/v1/profile/preferences",
        headers=auth_headers,
        json={"overlap_sensitivity": "high", "enable_email": False},
    )
    assert resp.status_code == 200
    data = resp.json()
    assert data["overlap_sensitivity"] == "high"
    assert data["enable_email"] == False

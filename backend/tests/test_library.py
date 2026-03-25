import pytest
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
        title=f"Research Paper {uuid.uuid4().hex[:6]}",
        authors=["Dr. Author"],
        abstract="A study about something interesting in science.",
        citation_count=10,
    )
    db.add(paper)
    await db.commit()
    await db.refresh(paper)
    return paper


async def test_save_paper(client: AsyncClient, auth_headers: dict, db_session: AsyncSession):
    paper = await _insert_paper(db_session)
    resp = await client.post(
        "/v1/library/save",
        headers=auth_headers,
        json={"paper_id": paper.id, "tags": ["ml", "research"]},
    )
    assert resp.status_code == 201
    data = resp.json()
    assert data["paper_id"] == str(paper.id)
    assert "ml" in data["tags"]


async def test_save_paper_not_found(client: AsyncClient, auth_headers: dict):
    resp = await client.post(
        "/v1/library/save",
        headers=auth_headers,
        json={"paper_id": "nonexistent"},
    )
    assert resp.status_code == 404


async def test_get_library(client: AsyncClient, auth_headers: dict, db_session: AsyncSession):
    paper = await _insert_paper(db_session)
    await client.post(
        "/v1/library/save", headers=auth_headers, json={"paper_id": paper.id}
    )
    resp = await client.get("/v1/library/", headers=auth_headers)
    assert resp.status_code == 200
    data = resp.json()
    assert data["total"] >= 1
    assert any(item["paper_id"] == str(paper.id) for item in data["items"])


async def test_update_saved_paper(
    client: AsyncClient, auth_headers: dict, db_session: AsyncSession
):
    paper = await _insert_paper(db_session)
    await client.post(
        "/v1/library/save", headers=auth_headers, json={"paper_id": paper.id}
    )
    resp = await client.put(
        f"/v1/library/{paper.id}",
        headers=auth_headers,
        json={"tags": ["updated", "tag"], "personal_note": "Great paper!"},
    )
    assert resp.status_code == 200
    data = resp.json()
    assert "updated" in data["tags"]
    assert data["personal_note"] == "Great paper!"


async def test_delete_saved_paper(
    client: AsyncClient, auth_headers: dict, db_session: AsyncSession
):
    paper = await _insert_paper(db_session)
    await client.post(
        "/v1/library/save", headers=auth_headers, json={"paper_id": paper.id}
    )
    resp = await client.delete(f"/v1/library/{paper.id}", headers=auth_headers)
    assert resp.status_code == 204

    get_resp = await client.get("/v1/library/", headers=auth_headers)
    assert not any(item["paper_id"] == str(paper.id) for item in get_resp.json()["items"])


async def test_create_folder(client: AsyncClient, auth_headers: dict):
    resp = await client.post(
        "/v1/library/folders",
        headers=auth_headers,
        json={"name": "My Research Folder"},
    )
    assert resp.status_code == 201
    data = resp.json()
    assert data["name"] == "My Research Folder"


async def test_get_folders(client: AsyncClient, auth_headers: dict):
    await client.post(
        "/v1/library/folders", headers=auth_headers, json={"name": "Folder A"}
    )
    await client.post(
        "/v1/library/folders", headers=auth_headers, json={"name": "Folder B"}
    )
    resp = await client.get("/v1/library/folders", headers=auth_headers)
    assert resp.status_code == 200
    assert len(resp.json()) >= 2


async def test_save_to_folder(
    client: AsyncClient, auth_headers: dict, db_session: AsyncSession
):
    paper = await _insert_paper(db_session)
    folder_resp = await client.post(
        "/v1/library/folders", headers=auth_headers, json={"name": "Special Folder"}
    )
    folder_id = folder_resp.json()["id"]
    save_resp = await client.post(
        "/v1/library/save",
        headers=auth_headers,
        json={"paper_id": paper.id, "folder_id": folder_id},
    )
    assert save_resp.status_code == 201
    assert save_resp.json()["folder_id"] == folder_id

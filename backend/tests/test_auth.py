import pytest
from httpx import AsyncClient

pytestmark = pytest.mark.asyncio


async def test_register_success(client: AsyncClient):
    resp = await client.post(
        "/v1/auth/register",
        json={"email": "newuser@test.com", "password": "Password123", "full_name": "New User"},
    )
    assert resp.status_code == 201
    data = resp.json()
    assert "access_token" in data
    assert "refresh_token" in data
    assert data["token_type"] == "bearer"
    assert data["user"]["email"] == "newuser@test.com"
    assert data["user"]["full_name"] == "New User"
    assert "id" in data["user"]


async def test_register_duplicate_email(client: AsyncClient):
    body = {"email": "dup@test.com", "password": "Password123", "full_name": "Dup User"}
    resp1 = await client.post("/v1/auth/register", json=body)
    assert resp1.status_code == 201
    resp2 = await client.post("/v1/auth/register", json=body)
    assert resp2.status_code == 409
    assert resp2.json()["error"] == "email_already_exists"


async def test_login_success(client: AsyncClient):
    await client.post(
        "/v1/auth/register",
        json={"email": "login@test.com", "password": "MyPass123", "full_name": "Login User"},
    )
    resp = await client.post(
        "/v1/auth/login",
        json={"email": "login@test.com", "password": "MyPass123"},
    )
    assert resp.status_code == 200
    data = resp.json()
    assert "access_token" in data
    assert data["user"]["email"] == "login@test.com"


async def test_login_invalid_credentials(client: AsyncClient):
    resp = await client.post(
        "/v1/auth/login",
        json={"email": "nobody@test.com", "password": "wrongpass"},
    )
    assert resp.status_code == 401
    assert resp.json()["error"] == "invalid_credentials"


async def test_login_wrong_password(client: AsyncClient):
    await client.post(
        "/v1/auth/register",
        json={"email": "wrongpw@test.com", "password": "correct123", "full_name": "WP User"},
    )
    resp = await client.post(
        "/v1/auth/login",
        json={"email": "wrongpw@test.com", "password": "wrong123"},
    )
    assert resp.status_code == 401


async def test_refresh_token(client: AsyncClient):
    reg = await client.post(
        "/v1/auth/register",
        json={"email": "refresh@test.com", "password": "Refresh123", "full_name": "Ref User"},
    )
    refresh_token = reg.json()["refresh_token"]
    resp = await client.post("/v1/auth/refresh", json={"refresh_token": refresh_token})
    assert resp.status_code == 200
    data = resp.json()
    assert "access_token" in data
    assert "refresh_token" in data


async def test_me_authenticated(client: AsyncClient, auth_headers: dict):
    resp = await client.get("/v1/auth/me", headers=auth_headers)
    assert resp.status_code == 200
    data = resp.json()
    assert data["email"] == "test@scholarsync.test"
    assert "id" in data


async def test_me_unauthenticated(client: AsyncClient):
    resp = await client.get("/v1/auth/me")
    assert resp.status_code == 403  # no auth header → HTTPBearer returns 403


async def test_me_invalid_token(client: AsyncClient):
    resp = await client.get("/v1/auth/me", headers={"Authorization": "Bearer invalid.token.here"})
    assert resp.status_code == 401

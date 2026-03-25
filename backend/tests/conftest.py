import pytest
import pytest_asyncio
from typing import AsyncGenerator
from httpx import AsyncClient, ASGITransport
from unittest.mock import AsyncMock, MagicMock, patch
from sqlalchemy.ext.asyncio import create_async_engine, async_sessionmaker, AsyncSession
from sqlalchemy import event

TEST_DATABASE_URL = "sqlite+aiosqlite:///:memory:"


def _patch_vector_columns():
    """Replace pgvector Vector type with Text so SQLite can create tables."""
    try:
        from pgvector.sqlalchemy import Vector
        from sqlalchemy import Text

        # Monkey-patch Vector to behave like Text for DDL
        Vector.__class_getitem__ = classmethod(lambda cls, item: Text())
    except Exception:
        pass


_patch_vector_columns()

from app.core.database import Base, get_db  # noqa: E402 — must be after patch
from app.main import app  # noqa: E402

_test_engine = None
_TestSessionLocal = None


def get_test_engine():
    global _test_engine, _TestSessionLocal
    if _test_engine is None:
        _test_engine = create_async_engine(
            TEST_DATABASE_URL,
            connect_args={"check_same_thread": False},
        )
        _TestSessionLocal = async_sessionmaker(
            _test_engine, class_=AsyncSession, expire_on_commit=False
        )
    return _test_engine, _TestSessionLocal


@pytest_asyncio.fixture(scope="session")
async def setup_db():
    engine, _ = get_test_engine()
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)
    yield
    await engine.dispose()


@pytest_asyncio.fixture
async def db_session(setup_db) -> AsyncGenerator[AsyncSession, None]:
    _, SessionLocal = get_test_engine()
    async with SessionLocal() as session:
        yield session
        await session.rollback()


@pytest_asyncio.fixture
async def client(db_session: AsyncSession) -> AsyncGenerator[AsyncClient, None]:
    async def override_get_db():
        yield db_session

    app.dependency_overrides[get_db] = override_get_db

    with patch("app.core.cache.get_redis") as mock_redis:
        mock_redis_client = AsyncMock()
        mock_redis_client.get.return_value = None
        mock_redis_client.set.return_value = True
        mock_redis_client.delete.return_value = True
        mock_redis_client.keys.return_value = []
        mock_redis.return_value = mock_redis_client

        async with AsyncClient(
            transport=ASGITransport(app=app), base_url="http://test"
        ) as ac:
            yield ac

    app.dependency_overrides.clear()


@pytest_asyncio.fixture
async def auth_headers(client: AsyncClient) -> dict:
    """Register a test user and return auth headers."""
    resp = await client.post(
        "/v1/auth/register",
        json={
            "email": "test@scholarsync.test",
            "password": "TestPassword123!",
            "full_name": "Test User",
        },
    )
    assert resp.status_code == 201, resp.text
    token = resp.json()["access_token"]
    return {"Authorization": f"Bearer {token}"}

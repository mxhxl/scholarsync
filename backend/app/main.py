import asyncio
import logging
import traceback
from contextlib import asynccontextmanager
from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
from slowapi import Limiter, _rate_limit_exceeded_handler
from slowapi.util import get_remote_address
from slowapi.errors import RateLimitExceeded
from app.api.v1.router import v1_router

logger = logging.getLogger(__name__)

limiter = Limiter(key_func=get_remote_address)


@asynccontextmanager
async def lifespan(app: FastAPI):
    # Enable pgvector extension if available (skipped gracefully if not installed)
    try:
        from app.core.database import engine, enable_pgvector
        async with engine.begin() as conn:
            await enable_pgvector(conn)
        logger.info("pgvector extension enabled")
    except asyncio.CancelledError:
        raise  # allow graceful shutdown (e.g. Ctrl+C)
    except Exception as e:
        logger.warning("pgvector setup skipped (non-fatal): %s", e)

    # Auto-add role column if missing (safe idempotent migration)
    try:
        from app.core.database import engine as _engine
        from sqlalchemy import text as _text
        async with _engine.begin() as conn:
            await conn.execute(
                _text("ALTER TABLE users ADD COLUMN IF NOT EXISTS role VARCHAR DEFAULT 'user'")
            )
        logger.info("users.role column ensured")
    except asyncio.CancelledError:
        raise
    except Exception as e:
        logger.warning("Auto-migration for role column skipped (non-fatal): %s", e)

    # Seed admin account if it doesn't exist
    try:
        from app.core.database import AsyncSessionLocal
        from app.core.security import hash_password
        from app.models.users import User
        from sqlalchemy import select

        async with AsyncSessionLocal() as db:
            result = await db.execute(select(User).where(User.email == "admin@scholarsync.app"))
            if not result.scalar_one_or_none():
                admin_user = User(
                    email="admin@scholarsync.app",
                    hashed_password=hash_password("admin123"),
                    full_name="Admin User",
                    role="admin",
                    is_active=True,
                    is_verified=True,
                )
                db.add(admin_user)
                await db.commit()
                logger.info("Admin seed account created (admin@scholarsync.app)")
            else:
                logger.info("Admin seed account already exists")
    except asyncio.CancelledError:
        raise
    except Exception as e:
        logger.warning("Admin seed skipped (non-fatal): %s", e)

    # Preload SciBERT model so first request isn't slow
    try:
        from app.services.ai.embedding import get_scibert_model
        get_scibert_model()
        logger.info("SciBERT model preloaded")
    except Exception as e:
        logger.warning("SciBERT preload failed (non-fatal): %s", e)

    yield


app = FastAPI(
    title="ScholarSync API",
    version="1.0.0",
    lifespan=lifespan,
)

app.state.limiter = limiter
app.add_exception_handler(RateLimitExceeded, _rate_limit_exceeded_handler)

app.add_middleware(
    CORSMiddleware,
    allow_origin_regex=".*",
    allow_credentials=False,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(v1_router, prefix="/v1")


@app.get("/health")
async def health():
    return {"status": "ok", "version": "1.0.0"}


@app.exception_handler(RequestValidationError)
async def validation_exception_handler(request: Request, exc: RequestValidationError):
    return JSONResponse(
        status_code=422,
        content={"error": "validation_error", "detail": str(exc.errors()[0]["msg"])},
    )


@app.exception_handler(Exception)
async def global_exception_handler(request: Request, exc: Exception):
    logger.error("Unhandled exception: %s\n%s", exc, traceback.format_exc())
    return JSONResponse(
        status_code=500,
        content={"error": "internal_server_error", "detail": "An unexpected error occurred"},
    )

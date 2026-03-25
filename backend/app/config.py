from pathlib import Path
from pydantic_settings import BaseSettings
from functools import lru_cache

_ENV_FILE = Path(__file__).resolve().parent.parent / ".env"


class Settings(BaseSettings):
    # Database
    DATABASE_URL: str = "postgresql+asyncpg://scholarsync:scholarsync123@localhost:5432/scholarsync"

    # Redis
    REDIS_URL: str = "redis://localhost:6379/0"

    # RabbitMQ
    RABBITMQ_URL: str = "amqp://guest:guest@localhost:5672/"

    # Auth
    SECRET_KEY: str = "change-me-to-something-secure-at-least-32-chars"
    ALGORITHM: str = "HS256"
    ACCESS_TOKEN_EXPIRE_MINUTES: int = 30
    REFRESH_TOKEN_EXPIRE_DAYS: int = 7

    # Ollama (local LLM)
    OLLAMA_BASE_URL: str = "http://localhost:11434"
    OLLAMA_SUMMARY_MODEL: str = "llama3.2:3b"
    OLLAMA_OVERLAP_MODEL: str = "llama3.1:8b"

    # SciBERT
    SCIBERT_MODEL: str = "allenai/scibert_scivocab_uncased"

    # Firebase
    FIREBASE_SERVICE_ACCOUNT_PATH: str = "./firebase-service-account.json"

    # SendGrid
    SENDGRID_API_KEY: str = ""
    SENDGRID_FROM_EMAIL: str = "noreply@scholarsync.app"

    # External APIs
    SEMANTIC_SCHOLAR_API_KEY: str = ""
    ARXIV_MAX_RESULTS: int = 50
    PUBMED_MAX_RESULTS: int = 50

    # Environment
    ENVIRONMENT: str = "development"

    class Config:
        env_file = str(_ENV_FILE)
        env_file_encoding = "utf-8"
        extra = "ignore"


@lru_cache(maxsize=1)
def get_settings() -> Settings:
    return Settings()


settings = get_settings()

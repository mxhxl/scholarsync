from sentence_transformers import SentenceTransformer
from functools import lru_cache
from app.config import settings


@lru_cache(maxsize=1)
def get_scibert_model() -> SentenceTransformer:
    """Load once, cache forever. Called in lifespan to preload."""
    return SentenceTransformer(settings.SCIBERT_MODEL)


def generate_profile_embedding(field: str, topics: list[str], keywords: list[str]) -> list[float]:
    """
    Weighted text: field repeated 3x, topics 2x, keywords 1x.
    normalize_embeddings=True for cosine similarity via pgvector <=> operator.
    """
    model = get_scibert_model()
    parts = [field] * 3 + [t for t in topics for _ in range(2)] + keywords
    text = " ".join(parts)
    embedding = model.encode(text, normalize_embeddings=True)
    return embedding.tolist()


def generate_paper_embedding(title: str, abstract: str) -> list[float]:
    """Title repeated 2x + abstract. Truncate to 2048 chars."""
    model = get_scibert_model()
    text = (title + " " + title + " " + abstract)[:2048]
    embedding = model.encode(text, normalize_embeddings=True)
    return embedding.tolist()


def generate_project_embedding(title: str, description: str) -> list[float]:
    """Title repeated 3x + description. Truncate to 2048 chars."""
    model = get_scibert_model()
    text = (title + " " + title + " " + title + " " + description)[:2048]
    embedding = model.encode(text, normalize_embeddings=True)
    return embedding.tolist()


def generate_embeddings_batch(texts: list[str], batch_size: int = 32) -> list[list[float]]:
    """Batch encode. Use during Celery paper ingestion tasks."""
    model = get_scibert_model()
    embeddings = model.encode(texts, batch_size=batch_size, normalize_embeddings=True)
    return [e.tolist() for e in embeddings]

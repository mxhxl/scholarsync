import redis.asyncio as aioredis
from typing import Optional
from app.config import settings

_redis_client: Optional[aioredis.Redis] = None


def get_redis() -> aioredis.Redis:
    global _redis_client
    if _redis_client is None:
        _redis_client = aioredis.from_url(settings.REDIS_URL, decode_responses=True)
    return _redis_client


async def get_cached(key: str) -> Optional[str]:
    try:
        client = get_redis()
        return await client.get(key)
    except Exception:
        return None


async def set_cached(key: str, value: str, ttl: int) -> None:
    try:
        client = get_redis()
        await client.set(key, value, ex=ttl)
    except Exception:
        pass


async def delete_cached(key: str) -> None:
    try:
        client = get_redis()
        await client.delete(key)
    except Exception:
        pass


async def invalidate_pattern(pattern: str) -> None:
    try:
        client = get_redis()
        keys = await client.keys(pattern)
        if keys:
            await client.delete(*keys)
    except Exception:
        pass

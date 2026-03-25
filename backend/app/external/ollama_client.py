import httpx
import json
import logging
from app.config import settings

logger = logging.getLogger(__name__)


def chat_completion(
    model: str,
    system_prompt: str,
    user_prompt: str,
    temperature: float = 0.2,
) -> dict:
    """Send a chat completion request to local Ollama and return parsed JSON."""
    url = f"{settings.OLLAMA_BASE_URL}/api/chat"
    response = httpx.post(
        url,
        json={
            "model": model,
            "messages": [
                {"role": "system", "content": system_prompt},
                {"role": "user", "content": user_prompt},
            ],
            "format": "json",
            "stream": False,
            "options": {
                "temperature": temperature,
                "num_ctx": 2048,
            },
        },
        timeout=180.0,
    )
    response.raise_for_status()
    content = response.json()["message"]["content"]
    logger.info("Ollama response length: %d chars (model=%s)", len(content), model)
    return json.loads(content)

import asyncio
import json
import logging
from typing import Optional
from tenacity import retry, stop_after_attempt, wait_exponential, retry_if_exception_type
from httpx import ConnectError, ReadTimeout
from app.external.ollama_client import chat_completion
from app.config import settings

logger = logging.getLogger(__name__)

SYSTEM_PROMPT = """You are an expert academic advisor helping PhD researchers understand how recently published papers relate to their own research project.

Your analysis must be:
- Objective — assess overlap honestly, neither alarmist nor dismissive
- Actionable — give the researcher concrete next steps
- Specific — reference actual methodologies, datasets, and findings by name
- Constructive — even high-overlap papers can be framed as positioning opportunities

You must respond with a valid JSON object only. No preamble, no markdown, no text outside the JSON."""

USER_PROMPT_TEMPLATE = """A PhD researcher's project and a recently published paper have {similarity_pct}% semantic similarity. Analyze the relationship.

RESEARCHER'S PROJECT:
Title: {project_title}
Description: {project_description}

PUBLISHED PAPER:
Title: {paper_title}
Authors: {authors}
Abstract: {abstract}

Return ONLY this JSON:
{{
  "overlap_summary": "1-2 sentence plain-English summary of the relationship.",
  "similarities": ["similarity 1", "similarity 2", "similarity 3"],
  "differences": ["difference 1", "difference 2", "difference 3"],
  "recommendations": ["action 1", "action 2", "action 3"],
  "severity_explanation": "One sentence on why this level matters and what to do first."
}}"""

REQUIRED_KEYS = {"overlap_summary", "similarities", "differences", "recommendations", "severity_explanation"}


@retry(
    stop=stop_after_attempt(3),
    wait=wait_exponential(multiplier=1, min=2, max=30),
    retry=retry_if_exception_type((ConnectError, ReadTimeout)),
    reraise=False,
)
def _call_ollama_sync(prompt: str) -> Optional[dict]:
    model = settings.OLLAMA_OVERLAP_MODEL
    data = chat_completion(model, SYSTEM_PROMPT, prompt, temperature=0.3)
    if not REQUIRED_KEYS.issubset(data.keys()):
        logger.warning("Ollama overlap missing required keys: %s", data.keys())
        return None
    for key in ("similarities", "differences", "recommendations"):
        if not isinstance(data.get(key), list):
            data[key] = []
    return data


async def generate_overlap_report(
    project_title: str,
    project_description: str,
    paper_title: str,
    paper_authors: list[str],
    paper_abstract: str,
    similarity_score: float,
) -> Optional[dict]:
    """Generate a research overlap report using local Ollama. Returns None on failure."""
    try:
        similarity_pct = round(similarity_score * 100, 1)
        prompt = USER_PROMPT_TEMPLATE.format(
            similarity_pct=similarity_pct,
            project_title=project_title,
            project_description=project_description,
            paper_title=paper_title,
            authors=", ".join(paper_authors),
            abstract=paper_abstract or "Not available",
        )
        loop = asyncio.get_event_loop()
        result = await loop.run_in_executor(None, lambda: _call_ollama_sync(prompt))
        return result
    except Exception as e:
        logger.error("overlap generation failed: %s", e)
        return None

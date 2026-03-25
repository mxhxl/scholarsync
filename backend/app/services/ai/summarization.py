import asyncio
import json
import logging
from typing import Optional
from app.external.ollama_client import chat_completion
from app.config import settings

logger = logging.getLogger(__name__)

SYSTEM_PROMPT = """You are an expert academic research assistant specializing in scientific literature analysis. Your role is to create structured summaries that help PhD students and researchers quickly understand the core contributions of a paper.

Your summaries must be:
- Factually accurate — only state what is explicitly in the paper
- Specific — include exact metrics, dataset names, model names when mentioned
- Concise — each section is 2-4 sentences maximum
- Jargon-appropriate — preserve technical terminology, do not oversimplify
- Honest — if information for a section is not available in the abstract, write "Not specified in abstract"

You must respond with a valid JSON object only. No preamble, no markdown fences, no explanation outside the JSON."""

USER_PROMPT_TEMPLATE = """Analyze this academic paper and return a JSON summary.

PAPER:
Title: {title}
Authors: {authors}
Abstract: {abstract}

Return ONLY this JSON structure (no other text):
{{
  "purpose": "The specific research problem or gap this paper addresses.",
  "methodology": "The technical approach, models, datasets, and experimental setup used.",
  "key_results": "The main findings with specific numbers, percentages, or metrics where available.",
  "limitations": "Acknowledged weaknesses, constraints, or scope limitations.",
  "relevance_to_field": "Why this paper matters. What it advances or enables.",
  "research_gaps": "Identify 2-3 specific loopholes, unresolved questions, or gaps in this paper that a researcher could investigate further. For each gap, briefly suggest how it could be turned into a new research paper or project. Be concrete and actionable — mention specific experiments, datasets, or methods that could be explored."
}}

Each value: 2-4 sentences. Be specific, not generic."""

REQUIRED_KEYS = {"purpose", "methodology", "key_results", "limitations", "relevance_to_field", "research_gaps"}

SUGGESTIONS_SYSTEM_PROMPT = """You are an expert academic research advisor. Given a paper and the researcher's profile, generate actionable research suggestions. Respond with a valid JSON object only. No preamble, no markdown fences."""

SUGGESTIONS_USER_TEMPLATE = """Analyze this paper and generate research suggestions personalized to the researcher's profile.

PAPER:
Title: {title}
Authors: {authors}
Abstract: {abstract}

RESEARCHER PROFILE:
Field: {research_field}
Topics: {topics}
Keywords: {keywords}

Return ONLY this JSON structure:
{{
  "research_directions": ["3-4 specific research directions or open problems suggested by this paper that align with the researcher's interests"],
  "practical_applications": ["2-3 ways these findings could be applied in the researcher's field"],
  "recommended_reading": ["3-5 specific topics, methods, or seminal works to explore next based on this paper"],
  "key_takeaways": ["3-4 concise key takeaways relevant to the researcher's work"]
}}

Each array item: 1-2 sentences. Be specific and actionable, not generic."""

SUGGESTIONS_REQUIRED_KEYS = {"research_directions", "practical_applications", "recommended_reading", "key_takeaways"}

LITREVIEW_SYSTEM_PROMPT = """You are an expert academic writer specializing in IEEE-format literature reviews. You produce structured literature review tables that summarize related work for IEEE conference or journal papers.

You must respond with a valid JSON object only. No preamble, no markdown fences."""

LITREVIEW_USER_TEMPLATE = """Based on this paper's title, authors, and abstract, identify 5-8 closely related prior works and produce a structured literature review table in IEEE format.

PAPER:
Title: {title}
Authors: {authors}
Abstract: {abstract}

Return ONLY this JSON structure:
{{
  "entries": [
    {{
      "ref_no": 1,
      "authors": "LastName et al.",
      "year": "2023",
      "title": "Title of the related work",
      "methodology": "Brief description of the approach or technique used",
      "key_findings": "Main results or contributions of this work",
      "limitations": "Gaps or weaknesses this work leaves unaddressed"
    }}
  ]
}}

Generate 5-8 entries. Each entry must be a real or plausible related work in the same research area. Be specific: use real method names, dataset names, and metrics where appropriate. The limitations column should highlight gaps that the current paper potentially addresses."""


def _call_ollama_sync(prompt: str) -> Optional[dict]:
    model = settings.OLLAMA_SUMMARY_MODEL
    data = chat_completion(model, SYSTEM_PROMPT, prompt, temperature=0.2)
    missing = REQUIRED_KEYS - data.keys()
    if missing:
        logger.warning("Ollama summary missing keys: %s (got: %s)", missing, list(data.keys()))
        for key in missing:
            data[key] = ""
    return data


async def generate_paper_summary(
    title: str,
    authors: list[str],
    abstract: str,
) -> Optional[dict]:
    """Generate a paper summary using local Ollama. Returns None on failure."""
    try:
        prompt = USER_PROMPT_TEMPLATE.format(
            title=title,
            authors=", ".join(authors),
            abstract=abstract or "Not available",
        )
        loop = asyncio.get_event_loop()
        result = await loop.run_in_executor(None, lambda: _call_ollama_sync(prompt))
        return result
    except Exception as e:
        logger.error("summarization failed for paper '%s': %s", title, e, exc_info=True)
        return None


def _call_suggestions_sync(prompt: str) -> Optional[dict]:
    model = settings.OLLAMA_SUMMARY_MODEL
    data = chat_completion(model, SUGGESTIONS_SYSTEM_PROMPT, prompt, temperature=0.2)
    missing = SUGGESTIONS_REQUIRED_KEYS - data.keys()
    if missing:
        logger.warning("Ollama suggestions missing keys: %s (got: %s)", missing, list(data.keys()))
        for key in missing:
            data[key] = []
    # Ensure all values are lists
    for key in SUGGESTIONS_REQUIRED_KEYS:
        if not isinstance(data.get(key), list):
            data[key] = [data[key]] if data.get(key) else []
    return data


async def generate_paper_suggestions(
    title: str,
    authors: list[str],
    abstract: str,
    research_field: str,
    topics: list[str],
    keywords: list[str],
) -> Optional[dict]:
    """Generate personalized research suggestions using local Ollama."""
    try:
        prompt = SUGGESTIONS_USER_TEMPLATE.format(
            title=title,
            authors=", ".join(authors),
            abstract=abstract or "Not available",
            research_field=research_field,
            topics=", ".join(topics) if topics else "Not specified",
            keywords=", ".join(keywords) if keywords else "Not specified",
        )
        loop = asyncio.get_event_loop()
        result = await loop.run_in_executor(None, lambda: _call_suggestions_sync(prompt))
        return result
    except Exception as e:
        logger.error("suggestions failed for paper '%s': %s", title, e)
        return None


def _call_litreview_sync(prompt: str) -> Optional[dict]:
    model = settings.OLLAMA_SUMMARY_MODEL
    data = chat_completion(model, LITREVIEW_SYSTEM_PROMPT, prompt, temperature=0.3)
    if "entries" not in data:
        logger.warning("Ollama literature review missing 'entries' key (got: %s)", list(data.keys()))
        data["entries"] = []
    if not isinstance(data["entries"], list):
        data["entries"] = []
    # Ensure each entry has all required fields
    for entry in data["entries"]:
        for field in ("ref_no", "authors", "year", "title", "methodology", "key_findings", "limitations"):
            if field not in entry:
                entry[field] = 0 if field == "ref_no" else ""
    return data


async def generate_literature_review(
    title: str,
    authors: list[str],
    abstract: str,
) -> Optional[dict]:
    """Generate an IEEE-format literature review using local Ollama."""
    try:
        prompt = LITREVIEW_USER_TEMPLATE.format(
            title=title,
            authors=", ".join(authors),
            abstract=abstract or "Not available",
        )
        loop = asyncio.get_event_loop()
        result = await loop.run_in_executor(None, lambda: _call_litreview_sync(prompt))
        return result
    except Exception as e:
        logger.error("literature review failed for paper '%s': %s", title, e)
        return None

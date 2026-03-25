import logging
from datetime import datetime, timedelta, timezone
from fastapi import APIRouter, Depends, HTTPException
from app.config import settings
from fastapi.responses import JSONResponse
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select
from app.core.database import get_db
from app.core.security import get_current_user
from app.models.users import User
from app.models.papers import Paper
from app.models.paper_summaries import PaperSummary
from app.models.research_profiles import ResearchProfile
from app.schemas.paper import PaperResponse, SummaryResponse, SuggestionsResponse, LiteratureReviewResponse, LitReviewEntry, ProcessingResponse
from app.services.ai.summarization import generate_paper_summary, generate_paper_suggestions, generate_literature_review

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/papers", tags=["papers"])


@router.get("/{paper_id}", response_model=PaperResponse)
async def get_paper(
    paper_id: str,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    result = await db.execute(select(Paper).where(Paper.id == paper_id))
    paper = result.scalar_one_or_none()
    if not paper:
        raise HTTPException(
            status_code=404,
            detail={"error": "not_found", "detail": "Paper not found"},
        )
    return PaperResponse(
        id=str(paper.id),
        external_id=paper.external_id,
        source=paper.source,
        title=paper.title,
        authors=paper.authors or [],
        abstract=paper.abstract,
        published_date=paper.published_date,
        pdf_url=paper.pdf_url,
        citation_count=paper.citation_count or 0,
        venue=paper.venue,
        created_at=paper.created_at,
    )


@router.get("/{paper_id}/summary")
async def get_summary(
    paper_id: str,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    paper_result = await db.execute(select(Paper).where(Paper.id == paper_id))
    paper = paper_result.scalar_one_or_none()
    if not paper:
        raise HTTPException(
            status_code=404,
            detail={"error": "not_found", "detail": "Paper not found"},
        )

    summary_result = await db.execute(
        select(PaperSummary).where(PaperSummary.paper_id == paper_id)
    )
    summary = summary_result.scalar_one_or_none()

    # Return cached ready summary
    if summary and summary.status == "ready":
        return SummaryResponse(
            paper_id=str(paper_id),
            purpose=summary.purpose,
            methodology=summary.methodology,
            key_results=summary.key_results,
            limitations=summary.limitations,
            relevance_to_field=summary.relevance_to_field,
            research_gaps=summary.research_gaps,
            status="ready",
            model_version=summary.model_version,
            generated_at=summary.generated_at,
        )

    now = datetime.now(timezone.utc)

    # Create or reuse summary row
    if not summary:
        summary = PaperSummary(paper_id=paper_id, status="generating")
        db.add(summary)
        await db.commit()
        await db.refresh(summary)

    # Call Ollama directly (no 202 — wait for the result)
    logger.info("Calling Ollama for paper '%s' (id=%s)", paper.title[:60], paper_id)
    data = await generate_paper_summary(
        title=paper.title,
        authors=paper.authors or [],
        abstract=paper.abstract or "",
    )
    logger.info("Ollama result for paper %s: %s", paper_id, "success" if data else "FAILED (returned None)")

    if data:
        summary.purpose = data.get("purpose")
        summary.methodology = data.get("methodology")
        summary.key_results = data.get("key_results")
        summary.limitations = data.get("limitations")
        summary.relevance_to_field = data.get("relevance_to_field")
        # LLM may return research_gaps as a list of objects — flatten to string
        raw_gaps = data.get("research_gaps", "")
        if isinstance(raw_gaps, list):
            parts = []
            for item in raw_gaps:
                if isinstance(item, dict):
                    parts.append(item.get("gap", str(item)))
                else:
                    parts.append(str(item))
            summary.research_gaps = "\n\n".join(parts)
        else:
            summary.research_gaps = str(raw_gaps) if raw_gaps else ""
        summary.model_version = settings.OLLAMA_SUMMARY_MODEL
        summary.status = "ready"
        summary.generated_at = now
    else:
        summary.status = "failed"
        summary.failed_at = now
        summary.retry_count = (summary.retry_count or 0) + 1

    await db.commit()
    await db.refresh(summary)

    if summary.status == "ready":
        return SummaryResponse(
            paper_id=str(paper_id),
            purpose=summary.purpose,
            methodology=summary.methodology,
            key_results=summary.key_results,
            limitations=summary.limitations,
            relevance_to_field=summary.relevance_to_field,
            research_gaps=summary.research_gaps,
            status="ready",
            model_version=summary.model_version,
            generated_at=summary.generated_at,
        )

    raise HTTPException(
        status_code=500,
        detail={"error": "summary_failed", "detail": "AI summary generation failed. Please try again."},
    )


@router.get("/{paper_id}/suggestions", response_model=SuggestionsResponse)
async def get_suggestions(
    paper_id: str,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    paper_result = await db.execute(select(Paper).where(Paper.id == paper_id))
    paper = paper_result.scalar_one_or_none()
    if not paper:
        raise HTTPException(
            status_code=404,
            detail={"error": "not_found", "detail": "Paper not found"},
        )

    profile_result = await db.execute(
        select(ResearchProfile).where(ResearchProfile.user_id == current_user.id)
    )
    profile = profile_result.scalar_one_or_none()

    research_field = profile.research_field if profile else "General"
    topics = profile.topics if profile else []
    keywords = profile.keywords if profile else []

    data = await generate_paper_suggestions(
        title=paper.title,
        authors=paper.authors or [],
        abstract=paper.abstract or "",
        research_field=research_field,
        topics=topics,
        keywords=keywords,
    )

    if data:
        return SuggestionsResponse(
            paper_id=str(paper_id),
            research_directions=data.get("research_directions", []),
            practical_applications=data.get("practical_applications", []),
            recommended_reading=data.get("recommended_reading", []),
            key_takeaways=data.get("key_takeaways", []),
            status="ready",
        )

    return SuggestionsResponse(paper_id=str(paper_id), status="failed")


@router.get("/{paper_id}/literature-review", response_model=LiteratureReviewResponse)
async def get_literature_review(
    paper_id: str,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    paper_result = await db.execute(select(Paper).where(Paper.id == paper_id))
    paper = paper_result.scalar_one_or_none()
    if not paper:
        raise HTTPException(
            status_code=404,
            detail={"error": "not_found", "detail": "Paper not found"},
        )

    logger.info("Generating literature review for paper '%s' (id=%s)", paper.title[:60], paper_id)
    data = await generate_literature_review(
        title=paper.title,
        authors=paper.authors or [],
        abstract=paper.abstract or "",
    )

    if data and data.get("entries"):
        entries = [LitReviewEntry(**e) for e in data["entries"]]
        return LiteratureReviewResponse(
            paper_id=str(paper_id),
            entries=entries,
            status="ready",
        )

    return LiteratureReviewResponse(paper_id=str(paper_id), status="failed")

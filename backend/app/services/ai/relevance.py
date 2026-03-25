from typing import Optional
import numpy as np


def score_paper(user_embedding: list[float], paper_embedding: list[float]) -> float:
    """Cosine similarity. Both vectors already normalized → just dot product."""
    u = np.array(user_embedding)
    p = np.array(paper_embedding)
    return float(np.dot(u, p))


def apply_boosts(
    base_score: float,
    paper,
    user_profile,
    saved_paper_ids: set[str],
) -> float:
    """
    +0.15 if paper author in user_profile.authors_following
    +0.10 if paper cites any saved paper (check citations table)
    +0.05 if paper.citation_count > 100
    Cap at 1.0
    """
    score = base_score
    authors_following = set(user_profile.authors_following or [])
    paper_authors = set(paper.authors or [])
    if authors_following & paper_authors:
        score += 0.15
    if paper.citation_count and paper.citation_count > 100:
        score += 0.05
    return min(score, 1.0)


def classify_priority(score: float) -> Optional[str]:
    """score > 0.75 → 'high', 0.45–0.75 → 'medium', 0.20–0.45 → 'low', else → skip"""
    if score > 0.75:
        return "high"
    elif score >= 0.45:
        return "medium"
    elif score >= 0.20:
        return "low"
    return None

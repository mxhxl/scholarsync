"""
Quick test script — creates a fake 'new_paper' alert for a user so the
Android AlertCheckWorker picks it up and fires a phone notification.

Usage:
    cd backend
    python test_notification.py <user_email>

Requires: the backend DB to be running (PostgreSQL).
"""
import asyncio
import sys
import uuid
from datetime import datetime, timezone


async def main(email: str):
    from app.core.database import AsyncSessionLocal
    from sqlalchemy import select
    from app.models.users import User
    from app.models.papers import Paper
    from app.models.alerts import Alert

    async with AsyncSessionLocal() as db:
        # Find user
        result = await db.execute(select(User).where(User.email == email))
        user = result.scalar_one_or_none()
        if not user:
            print(f"No user found with email: {email}")
            return

        # Pick any paper from the DB (or create a dummy one)
        paper_result = await db.execute(select(Paper).limit(1))
        paper = paper_result.scalar_one_or_none()

        if not paper:
            # Create a dummy paper
            paper = Paper(
                id=str(uuid.uuid4()),
                title="Attention Mechanisms for Low-Resource NLP: A Comprehensive Survey",
                authors=["Alice Researcher", "Bob Scientist"],
                abstract="This paper surveys recent advances in attention-based architectures for NLP tasks in low-resource settings.",
                source="arxiv",
                external_id=f"test_{uuid.uuid4().hex[:8]}",
            )
            db.add(paper)
            await db.flush()
            print(f"Created dummy paper: {paper.title}")

        # Create the alert
        alert = Alert(
            id=str(uuid.uuid4()),
            user_id=user.id,
            paper_id=paper.id,
            type="new_paper",
            title="New paper on NLP",
            description=f'A new paper matching your "NLP" interest has been published: {paper.title}',
            similarity_score=0.85,
        )
        db.add(alert)
        await db.commit()

        print(f"Created test alert for {email}:")
        print(f"  Alert ID:  {alert.id}")
        print(f"  Title:     {alert.title}")
        print(f"  Paper:     {paper.title}")
        print()
        print("Now open the Android app — the AlertCheckWorker will pick this up")
        print("within 30 min, or call NotificationTest.triggerWorkerNow(context)")
        print("to trigger it immediately.")


if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: python test_notification.py <user_email>")
        sys.exit(1)
    asyncio.run(main(sys.argv[1]))

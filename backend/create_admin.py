"""Create an admin user for testing.

Usage:
    python create_admin.py
"""

import asyncio
from app.core.database import AsyncSessionLocal, engine, Base
from app.core.security import hash_password
from app.models.users import User
from sqlalchemy import select, text


ADMIN_EMAIL = "admin@scholarsync.app"
ADMIN_PASSWORD = "admin123"
ADMIN_NAME = "Admin User"


async def main():
    # Ensure tables exist
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)

    async with AsyncSessionLocal() as db:
        # Check if admin already exists
        result = await db.execute(select(User).where(User.email == ADMIN_EMAIL))
        existing = result.scalar_one_or_none()

        if existing:
            # Promote to admin if not already
            if existing.role != "admin":
                existing.role = "admin"
                existing.is_active = True
                existing.is_verified = True
                await db.commit()
                print(f"Existing user '{ADMIN_EMAIL}' promoted to admin.")
            else:
                print(f"Admin user '{ADMIN_EMAIL}' already exists.")
        else:
            user = User(
                email=ADMIN_EMAIL,
                hashed_password=hash_password(ADMIN_PASSWORD),
                full_name=ADMIN_NAME,
                role="admin",
                is_active=True,
                is_verified=True,
            )
            db.add(user)
            await db.commit()
            print(f"Admin user created successfully.")

        print(f"\n  Email:    {ADMIN_EMAIL}")
        print(f"  Password: {ADMIN_PASSWORD}")
        print(f"  Role:     admin")
        print(f"\n  Login at: http://localhost:3000/login")
        print(f"  Admin at: http://localhost:3000/admin")


if __name__ == "__main__":
    asyncio.run(main())

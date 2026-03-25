from fastapi import APIRouter, Depends, HTTPException, Response
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select
from app.core.database import get_db
from app.core.security import get_current_user
from app.models.users import User
from app.models.current_projects import CurrentProject
from app.schemas.project import ProjectCreate, ProjectResponse
from app.services.ai.embedding import generate_project_embedding

router = APIRouter(prefix="/projects", tags=["projects"])


@router.post("/current", response_model=ProjectResponse, status_code=201)
async def create_or_update_project(
    body: ProjectCreate,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    embedding = generate_project_embedding(body.title, body.description)

    result = await db.execute(
        select(CurrentProject).where(CurrentProject.user_id == current_user.id)
    )
    project = result.scalar_one_or_none()

    if project:
        project.title = body.title
        project.description = body.description
        project.set_embedding(embedding)
    else:
        project = CurrentProject(
            user_id=current_user.id,
            title=body.title,
            description=body.description,
        )
        project.set_embedding(embedding)
        db.add(project)

    await db.commit()
    await db.refresh(project)
    return ProjectResponse(
        id=str(project.id),
        user_id=str(project.user_id),
        title=project.title,
        description=project.description,
        created_at=project.created_at,
        updated_at=project.updated_at,
    )


@router.get("/current", response_model=ProjectResponse)
async def get_project(
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    result = await db.execute(
        select(CurrentProject).where(CurrentProject.user_id == current_user.id)
    )
    project = result.scalar_one_or_none()
    if not project:
        raise HTTPException(
            status_code=404,
            detail={"error": "not_found", "detail": "No current project found"},
        )
    return ProjectResponse(
        id=str(project.id),
        user_id=str(project.user_id),
        title=project.title,
        description=project.description,
        created_at=project.created_at,
        updated_at=project.updated_at,
    )


@router.delete("/current", status_code=204)
async def delete_project(
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    result = await db.execute(
        select(CurrentProject).where(CurrentProject.user_id == current_user.id)
    )
    project = result.scalar_one_or_none()
    if project:
        await db.delete(project)
        await db.commit()
    return Response(status_code=204)

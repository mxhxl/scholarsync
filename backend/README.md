# ScholarSync Backend

FastAPI + PostgreSQL + pgvector backend for the ScholarSync academic research assistant.

## How to Run Locally

```bash
# 1. Install dependencies
cd scholarsync/backend
pip install -r requirements.txt

# 2. Start PostgreSQL, Redis, RabbitMQ
# Option A: Homebrew (Mac)
brew services start postgresql
brew services start redis
brew services start rabbitmq

# Option B: run each manually
pg_ctl start
redis-server
rabbitmq-server

# 3. Create database
createdb scholarsync
psql scholarsync -c "CREATE EXTENSION IF NOT EXISTS vector;"

# 4. Copy and fill env
cp .env.example .env
# Edit .env — fill in SECRET_KEY at minimum. Ollama must be running locally.

# 5. Run migrations
alembic upgrade head

# 6. Start FastAPI
uvicorn app.main:app --reload --host 0.0.0.0 --port 8035

# 7. Start Celery worker (separate terminal)
celery -A app.core.celery_app worker --loglevel=info

# 8. Start Celery beat scheduler (separate terminal)
celery -A app.core.celery_app beat --loglevel=info

# API docs: http://180.235.121.253:8035/docs

# 9. Run tests
pip install aiosqlite
pytest tests/
```

## API Structure

All endpoints under `/v1/`. Swagger UI at `/docs`.

| Module | Prefix | Description |
|--------|--------|-------------|
| Auth | `/v1/auth` | Register, login, refresh, me |
| Profile | `/v1/profile` | Setup, update, notification preferences |
| Feed | `/v1/feed` | Personalized paper feed |
| Papers | `/v1/papers` | Paper details + AI summaries |
| Library | `/v1/library` | Saved papers + folders |
| Projects | `/v1/projects` | Current research project |
| Alerts | `/v1/alerts` | Overlap and citation alerts |
| Citations | `/v1/citations` | Citation graph + must-cite |
| Insights | `/v1/insights` | Research trend analysis |
| Notifications | `/v1/notifications` | Device token management |

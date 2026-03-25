# ScholarSync — Claude Context File

> This file is the source of truth for project context, architecture decisions, and workflow changes.
> Update this file whenever a major change, refactor, or workflow decision is made.

---

## Project Overview

**App name:** ScholarSync

### Platforms

| Platform | Location | Stack |
|---|---|---|
| Android | `app/` | Kotlin, Jetpack Compose, Material3 |
| Web | `web/` | Next.js 14, React 18, TypeScript, Tailwind CSS |
| Backend | `scholarsync/backend/` | FastAPI, PostgreSQL + pgvector, Redis, Celery, SciBERT, Ollama (local LLM) |

### Android
**Package:** `com.scholarsync`
**Min SDK:** 26 | **Target SDK:** 34 | **Compile SDK:** 34
**Build system:** Gradle (Kotlin DSL)

### Web
**Framework:** Next.js 14 (App Router)
**Language:** TypeScript
**Styling:** Tailwind CSS (matching Android color palette)
**State:** React hooks + Context (AuthProvider)
**API:** Proxied to FastAPI backend via Next.js rewrites (`/api/*` → `localhost:8000/v1/*`)

---

## Architecture

### Android
- **Pattern:** Single-activity, Compose-first
- **Navigation:** `androidx.navigation:navigation-compose` via `NavGraph.kt`
- **No ViewModel/state management layer yet** — screens are stateless Composables

```
app/src/main/java/com/scholarsync/
├── MainActivity.kt
├── navigation/
│   ├── NavGraph.kt
│   └── NavRoutes.kt
├── ui/
│   ├── components/
│   │   ├── BottomNavBar.kt
│   │   ├── Buttons.kt
│   │   ├── Cards.kt
│   │   ├── ChipComponents.kt
│   │   └── InputFields.kt
│   ├── screens/
│   │   ├── WelcomeScreen.kt
│   │   ├── ProfileSetupStep1Screen.kt
│   │   ├── ProfileSetupStep2Screen.kt
│   │   ├── ProfileSetupStep3Screen.kt
│   │   ├── HomeScreen.kt
│   │   ├── DiscoverScreen.kt
│   │   ├── LibraryScreen.kt
│   │   ├── BookmarksScreen.kt
│   │   ├── AlertsScreen.kt
│   │   ├── PaperDetailsScreen.kt
│   │   ├── SettingsScreen.kt
│   │   ├── EditProfileScreen.kt
│   │   ├── SecurityPrivacyScreen.kt
│   │   ├── HelpCenterScreen.kt
│   │   ├── HelpGettingStartedScreen.kt
│   │   ├── HelpResearchToolsScreen.kt
│   │   ├── HelpAccountScreen.kt
│   │   ├── HelpTroubleshootingScreen.kt
│   │   ├── HelpFaqScreen.kt
│   │   ├── RaiseTicketScreen.kt
│   │   └── EmailSupportScreen.kt
│   └── theme/
│       ├── Color.kt
│       ├── Theme.kt
│       └── Type.kt
├── notifications/
│   ├── NotificationHelper.kt
│   └── AlertCheckWorker.kt
```

### Web
- **Pattern:** Next.js App Router, client components with `"use client"`
- **Auth:** JWT stored in localStorage, auto-refresh via `lib/api.ts`
- **Layout:** Persistent sidebar (`AppShell` + `Sidebar`), public routes bypass sidebar

```
web/src/
├── app/
│   ├── layout.tsx              # Root layout (AuthProvider + AppShell)
│   ├── page.tsx                # Redirect → /feed
│   ├── login/page.tsx          # Login (split-screen with brand panel)
│   ├── register/page.tsx       # Registration
│   ├── profile-setup/page.tsx  # 3-step onboarding wizard
│   ├── feed/
│   │   ├── page.tsx            # Personalized paper feed
│   │   └── [paperId]/page.tsx  # Paper details + AI summary
│   ├── library/page.tsx        # Saved papers + folders
│   ├── alerts/page.tsx         # Overlap & new-paper alerts
│   ├── insights/page.tsx       # Trends, stats, activity chart
│   ├── settings/page.tsx       # Profile, notifications, account
│   └── admin/
│       ├── page.tsx             # Admin dashboard (stats overview)
│       ├── users/page.tsx       # User management (CRUD, roles, activate/deactivate)
│       ├── papers/page.tsx      # Paper management (search, delete)
│       ├── alerts/page.tsx      # Alert management (filter, delete)
│       └── settings/page.tsx    # System info & config display
├── components/
│   ├── AuthProvider.tsx
│   ├── AppShell.tsx             # Route guard (public / user / admin)
│   ├── Sidebar.tsx              # User sidebar (shows Admin Panel link for admins)
│   ├── AdminSidebar.tsx         # Admin sidebar (dark theme, "Back to App" link)
│   ├── PaperCard.tsx
│   ├── PageHeader.tsx
│   └── EmptyState.tsx
├── lib/
│   └── api.ts                  # Typed API client for all FastAPI endpoints
└── hooks/
    └── useAuth.ts              # Auth context + JWT management
```

---

## Navigation Flow

### Android
```
Welcome
  ├── Get Started → ProfileSetupStep1 → Step2 → Step3 → Home (clears back stack)
  └── Sign In → (not yet implemented)

Home (bottom nav)
  ├── Discover
  ├── Library → Bookmarks (pushed, has back)
  ├── Settings → Edit Profile | Security & Privacy | Help Center (pushed, has back)
  │     Help Center → Getting Started | Research Tools | Account | Troubleshooting | FAQ | Raise a Ticket | Email Support (each pushed, has back)
  └── Alerts (pushed, has back)

Home → PaperDetails/{paperId} (pushed, has back)
Home → Streaks (pushed via fire icon, has back)
Bookmarks → PaperDetails/{paperId} (pushed, has back)
```

### Web
```
/login ──→ /register
/register ──→ /profile-setup ──→ /feed

Sidebar (persistent):
  ├── /feed                  (personalized paper feed)
  ├── /feed/{paperId}        (paper details + AI summary)
  ├── /library               (saved papers + folders)
  ├── /alerts                (overlap & new-paper alerts)
  ├── /insights              (trends, stats, activity)
  └── /settings              (profile, notifications, account)

Admin Panel (role=admin only, separate dark sidebar):
  ├── /admin                 (dashboard — system stats)
  ├── /admin/users           (user management — search, CRUD, roles)
  ├── /admin/papers          (paper management — search, delete)
  ├── /admin/alerts          (alert management — filter, delete)
  └── /admin/settings        (system info)
```

---

## Key Dependencies

### Android
| Library | Version |
|---|---|
| `androidx.core:core-ktx` | 1.12.0 |
| `androidx.lifecycle:lifecycle-runtime-ktx` | 2.6.2 |
| `androidx.activity:activity-compose` | 1.8.1 |
| `compose-bom` | 2023.10.01 |
| `material3` | (via BOM) |
| `material-icons-extended` | (via BOM) |
| `navigation-compose` | 2.7.5 |
| `work-runtime-ktx` | 2.9.0 |
| Kotlin | 1.9.20 |
| AGP | 8.2.0 |

### Web
| Library | Version |
|---|---|
| `next` | ^14.2.0 |
| `react` / `react-dom` | ^18.3.0 |
| `typescript` | ^5.6.0 |
| `tailwindcss` | ^3.4.0 |
| `lucide-react` | ^0.460.0 |
| `clsx` | ^2.1.1 |
| `date-fns` | ^4.1.0 |

---

## Workflow Rules

- Always read `claude.md` at the start of each session for context.
- Update this file after any major change: new screen, new dependency, architecture shift, data layer addition, etc.
- Keep the navigation flow diagram up to date.

---

## Changelog

### 2026-03-25
- **Switched from Gemini API to local Ollama LLM** — All AI features now run locally.
  - Replaced `google-generativeai` SDK with direct HTTP calls to Ollama via `httpx` (already a dependency).
  - New `backend/app/external/ollama_client.py` — single `chat_completion()` function that calls `POST /api/chat` on Ollama.
  - Deleted `backend/app/external/gemini_client.py`.
  - Config: `GEMINI_API_KEY` / `GEMINI_SUMMARY_MODEL` / `GEMINI_OVERLAP_MODEL` → `OLLAMA_BASE_URL` / `OLLAMA_SUMMARY_MODEL` / `OLLAMA_OVERLAP_MODEL`.
  - Models: `llama3.2:3b` for summaries (fast), `llama3.1:8b` for overlap analysis (more capable).
  - Updated: `config.py`, `summarization.py`, `overlap.py`, `model_router.py`, `papers.py` endpoint, `paper_tasks.py`, `.env`, `.env.example`, `requirements.txt`.

### 2026-03-24
- **Admin Panel** — Full admin system for managing the platform.
  - Backend: Added `role` column to `users` table (`"user"` | `"admin"`, default `"user"`). Alembic migration `004_add_user_role.py`.
  - Backend: New `get_admin_user` dependency in `core/security.py` — returns 403 if user is not admin.
  - Backend: New `app/api/v1/endpoints/admin.py` with 9 endpoints:
    - `GET /v1/admin/dashboard` — system-wide stats (total users, papers, alerts, 7-day counts).
    - `GET /v1/admin/users` — paginated user list with search, role/active filters, papers read/saved counts.
    - `GET /v1/admin/users/{id}` — single user detail.
    - `PATCH /v1/admin/users/{id}` — update user role, is_active, is_verified (self-demotion guard).
    - `DELETE /v1/admin/users/{id}` — delete user (self-delete guard).
    - `GET /v1/admin/papers` — paginated paper list with search/source filters, summary/feed/saved counts.
    - `DELETE /v1/admin/papers/{id}` — delete paper (cascades to feeds, library, alerts).
    - `GET /v1/admin/alerts` — paginated alert list with type/read filters, joined user email and paper title.
    - `DELETE /v1/admin/alerts/{id}` — delete alert.
  - Backend: New `app/schemas/admin.py` — Pydantic schemas for all admin responses.
  - Backend: `UserResponse` schema now includes `role` field; `_user_to_response` updated.
  - Web: `User` interface now includes `role` field. Admin API types and methods added to `lib/api.ts`.
  - Web: New `AdminSidebar.tsx` — dark theme (#0f0f23), red accent, "Back to App" link.
  - Web: `AppShell.tsx` updated — admin routes use `AdminSidebar`, non-admin users redirected to `/feed`.
  - Web: `Sidebar.tsx` — shows "Admin Panel" link for users with `role === "admin"`.
  - Web: 5 new admin pages:
    - `/admin` — dashboard with stat cards and quick action links.
    - `/admin/users` — user table with search, role/status filters, action menu (activate/deactivate, promote/demote, delete).
    - `/admin/papers` — paper table with search, source filter, summary/feed/saved indicators.
    - `/admin/alerts` — alert table with type/read filters, color-coded type badges.
    - `/admin/settings` — system info cards (backend, database, scheduled tasks, data sources).
  - Build: All 18 routes compile cleanly (5 new admin routes).

### 2026-03-14
- **Reading Streaks (Gamification)** — Duolingo-style reading streak system.
  - Backend: `ReadingStreak` + `ReadingEvent` models, `GET /v1/streaks/me` and `POST /v1/streaks/record-read/{paper_id}` endpoints. XP system (10 base + 5/streak day bonus). Feed mark-read auto-records reading events.
  - Android: Full `StreaksScreen.kt` with hero streak card, stats row (best streak, XP, papers read), weekly goal progress bar, today's progress, week calendar with fire icons, and 6 achievements.
  - New `StreaksApi.kt` API client. New `NavRoutes.Streaks` route wired in `NavGraph.kt`.
  - Streak fire widget (orange fire icon + count) added to HomeScreen header — tapping navigates to StreaksScreen.
- **Follow Researchers** — Follow specific authors and get notified when they publish.
  - Backend: `POST /v1/profile/follow-author` and `POST /v1/profile/unfollow-author` endpoints.
  - Backend: `paper_tasks.py` now creates `followed_author` type alerts when a followed author publishes a new paper.
  - Android: `ProfileApi.kt` — new `getProfile`, `followAuthor`, `unfollowAuthor` methods.
  - Android: `PaperDetailsScreen` — each author name now has a Follow/Following button. Following state loaded from user profile.
  - `AlertCheckWorker` and `AlertsScreen` updated to handle `followed_author` alert type.
- **Highlights & Notes** — Save key passages from papers with color-coded highlights and notes.
  - Backend: `PaperHighlight` model, CRUD endpoints at `/v1/papers/{paper_id}/highlights`.
  - Android: `HighlightsApi.kt` API client. `PaperDetailsScreen` — new "Highlights & Notes" section with add dialog (color picker: yellow/blue/green/pink/orange), highlight cards with delete, and notes display. Highlights persist and reflect on revisit.
- **Push notifications for new papers** — Android app now sends real phone notifications when new papers matching the user's chosen topics are published.
  - New `notifications/NotificationHelper.kt` — creates notification channel, shows individual or batch notifications.
  - New `notifications/AlertCheckWorker.kt` — WorkManager periodic worker (every 30 min) polls `GET /v1/alerts/?is_read=false` and fires notifications for alerts created since last check.
  - New `api/AlertsApi.kt` — Android API client for `/v1/alerts/` endpoints (list, unread count, mark read) with both async and sync variants.
  - `SessionManager` — new `lastAlertCheckTime` tracking to avoid duplicate notifications.
  - `MainActivity` — requests `POST_NOTIFICATIONS` permission (Android 13+), creates notification channel, schedules the periodic worker on startup.
  - `AndroidManifest.xml` — added `POST_NOTIFICATIONS` permission.
  - `build.gradle.kts` — added `androidx.work:work-runtime-ktx:2.9.0`.
  - Backend `paper_tasks.py` — `fetch_papers_for_all_users` now creates `new_paper` type `Alert` entries for high/medium priority papers, matching paper content against user topics.
  - Backend `Alert` model comment updated to include `new_paper` type.
- **Profile picture support** — Users can pick an image from device gallery in EditProfileScreen; image is copied to app internal storage and displayed in both EditProfile and Settings screens. SessionManager stores the local file path.
- **Real user stats** — Replaced hardcoded stats (128/42/12) in SettingsScreen with live data from new backend endpoint `GET /v1/stats/me`. Stats: papers read, saved papers, summaries, unread alerts.
- **Backend stats endpoint** — New `backend/app/api/v1/endpoints/stats.py` aggregates counts from UserFeed, SavedPaper, PaperSummary, and Alert tables.
- **Android StatsApi client** — New `api/StatsApi.kt` calls the stats endpoint via authenticated OkHttp client.
- **Web API updated** — Added `stats.me()` and `UserStats` interface to `web/src/lib/api.ts`.
- **Change password** already fully wired: SecurityPrivacy → ChangePassword screen → backend `POST /v1/auth/change-password`.

### 2026-03-07
- **Help Center expanded** — 4 content pages (Getting Started, Research Tools, Account, Troubleshooting), FAQ screen with accordion (one open at a time), Raise a Ticket (dropdown issue types + Other), Email Support (opens mailto intent). Replaced Contact Us with Raise a Ticket; Account shows app account name/details from SessionManager.
- **Backend built** (`scholarsync/backend/`) — complete FastAPI backend.
  - 83 files: models, schemas, endpoints, services, tasks, tests.
  - Auth (JWT), Profile (SciBERT embeddings), Feed (pgvector relevance), Papers (Gemini summaries).
  - Library (saved papers + folders), Projects + Alerts (overlap detection), Citations (PageRank must-cite).
  - Insights (BERTopic trend analysis), Notifications (device tokens).
  - Celery beat: paper fetch (6h), overlap detection (daily 02:00), trends (weekly), email digest (daily 09:00).
  - Alembic migration `001_initial_schema.py` — all tables + ivfflat + GIN indexes.
  - Tests: 6 test files covering all endpoints; SQLite in-memory for CI.
  - Android compatibility: snake_case JSON, ISO 8601 datetimes, no null omission, arrays default `[]`, UUIDs as strings.
- **Android: four new screens from temp designs** — Security & Privacy, Help Center, Edit Profile, Bookmarks.
- New routes: `edit_profile`, `security_privacy`, `help_center`, `bookmarks`.
- Settings: Edit Profile, Security & Privacy, Help Center now navigate to their screens.
- Library: Bookmarks icon in header opens Bookmarks screen; bookmarks open Paper Details.

### 2026-03-06
- **Web app created** (`web/`) — Next.js 14 + React 18 + TypeScript + Tailwind CSS.
- Pages: Login, Register, Profile Setup (3-step wizard), Feed, Paper Details, Library, Alerts, Insights, Settings.
- Full typed API client (`lib/api.ts`) wired to all FastAPI backend endpoints.
- Auth flow with JWT (localStorage) + auto-refresh.
- Design system matches Android color palette (primary #1E3A5F, accent teal/gold).
- Mock data included for development without backend running.
- Successful production build: all 10 routes compile cleanly.

### 2026-02-19
- Initial `claude.md` created.
- Documented existing project structure: Android + Kotlin + Jetpack Compose.
- Screens: Welcome, ProfileSetup (3 steps), Home, Discover, Library, Alerts, PaperDetails, Settings.
- No backend/data layer yet — all screens are UI-only.

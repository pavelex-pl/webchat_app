# Webchat

Classic web-based chat built from the spec in `2026_04_18_AI_herders_jam_-_requirements_v3.pdf`.

## Stack

| Layer | Tech |
|---|---|
| Backend | Java 17, Spring Boot 3.3, Gradle (Kotlin DSL) |
| DB | PostgreSQL 16, Liquibase migrations |
| Cache / presence | Redis 7 |
| Real-time | Spring WebSocket + STOMP over SockJS |
| Auth | JWT access cookie + opaque refresh (DB-backed sessions) |
| Frontend | React 18, Vite, TypeScript, Tailwind, TanStack Query, Zustand |
| Files | Local FS, Docker volume |
| Run | `docker compose up` |

See `docs/design.md` for the full architecture document (data model, API surface, WS topics, auth flow, presence model, access-control rules).

## Run

```bash
docker compose up --build
```

Open http://localhost. Defaults (DB password, JWT secret) are baked into `docker-compose.yml`. To override, copy `.env.example` → `.env` before running.

Services:
- `frontend` (nginx + built React) on port 80
- `backend` (Spring Boot) on internal port 8080, reverse-proxied by nginx at `/api` and `/ws`
- `postgres` on internal 5432 with `pgdata` volume
- `redis` on internal 6379 (ephemeral, in-memory only)

File uploads are persisted to the `files` Docker volume. To fully reset, `docker compose down -v`.

## Features delivered

Maps to sections of the requirements PDF.

- **Auth (§2.1)** — register, login, logout, password reset (link logged to backend console), password change, delete account, active sessions view + per-session revoke
- **Presence (§2.2)** — ONLINE / AFK / OFFLINE via Redis ZSET tab tracking, 1 s delta broadcast, multi-tab aware
- **Friends (§2.3)** — requests with optional text, accept / decline / cancel, remove, user-to-user block (terminates friendship)
- **Rooms (§2.4)** — public catalog with search, private rooms by invitation, owner / admin / member roles, kick = ban, unban, admin promote / demote, invitations
- **Messaging (§2.5)** — text + replies + edit + delete, 3 KB limit, infinite scroll, live via STOMP, author or admin delete for rooms / author-only for DMs
- **Attachments (§2.6)** — files up to 20 MB / images up to 3 MB, copy-paste or file picker, per-attachment optional comment, access-controlled download
- **Notifications (§2.7)** — unread badges per chat, cleared on view
- **UI (§4)** — top bar, sidebar with accordion sections (public rooms, private rooms, DMs), main chat pane, right-side members panel, manage-room modal with tabbed admin controls, emoji picker
- **Non-functional (§3)** — soft-delete messages survive forever, reverse infinite scroll handles large history, attachments persist across access loss, no auto-logout

Not implemented: the optional **Jabber / XMPP federation** stretch goal (§6).

## Smoke test

```bash
# Register two users
curl -X POST -H 'Content-Type: application/json' -d '{"email":"a@x.io","username":"alice","password":"Hunter22!longer"}' http://localhost/api/auth/register
curl -X POST -H 'Content-Type: application/json' -d '{"email":"b@x.io","username":"bob","password":"Hunter22!longer"}' http://localhost/api/auth/register
```

Or use the UI at http://localhost to register, add a friend, exchange messages, attach a file, leave tabs idle to see AFK.

## Repo layout

```
webchat_app/
├── backend/
│   ├── build.gradle.kts
│   ├── src/main/
│   │   ├── java/com/webchat/
│   │   │   ├── auth/       users, sessions, JWT, password policy
│   │   │   ├── chat/       rooms + unified chat + direct
│   │   │   ├── message/    messages + live broadcaster
│   │   │   ├── attachment/ upload / download, local FS
│   │   │   ├── friends/    friendships + blocks
│   │   │   ├── presence/   Redis-backed presence + STOMP handlers
│   │   │   ├── config/     Spring Security, WebSocket
│   │   │   └── common/     exceptions, problem+json handler
│   │   └── resources/
│   │       ├── application.yml
│   │       └── db/changelog/  Liquibase master + change sets 0001..0006
│   └── Dockerfile          gradle build → temurin JRE
├── frontend/
│   ├── src/
│   │   ├── components/    AppLayout, Sidebar, MembersPanel, Composer, ManageRoomModal, PresenceDot, ...
│   │   ├── pages/         Login, Register, Forgot, Reset, Home, Chat, PublicRooms, Invitations, Friends, Sessions, Profile
│   │   ├── lib/           api, ws (STOMP), types, tabId
│   │   ├── routes/        AppRouter, ProtectedRoute
│   │   └── stores/        auth, presence (Zustand)
│   ├── nginx.conf         reverse-proxies /api + /ws to backend, 25m body limit
│   └── Dockerfile         node build → nginx:alpine
├── docs/design.md
├── docker-compose.yml
├── .env.example
└── README.md
```

## Notes

- Password reset links are logged to the backend container's stdout (no SMTP). Grep for `PASSWORD RESET LINK`.
- Cookies are `SameSite=Lax; HttpOnly`. `Secure` is off by default (dev-friendly over plain HTTP); flip via `COOKIE_SECURE=true` in `.env` behind TLS.
- JWT access tokens live 15 min; refresh tokens 30 d, stored hashed in `sessions` and rotated on each refresh.
- Presence state is ephemeral and lives only in Redis; a backend restart does not flap status as long as Redis stays up.

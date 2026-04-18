# Webchat — Design Document

Design for the online chat server specified in `2026_04_18_AI_herders_jam_-_requirements_v3.pdf`.

Goal of this doc: align on architecture, data model, APIs, and delivery phases before writing code. Everything below is a proposal — call out anything you want changed.

---

## 1. Tech stack (confirmed)

| Layer | Choice |
|---|---|
| Language / runtime | Java 17 |
| Backend framework | Spring Boot 3.3.x |
| Build tool | Gradle (Kotlin DSL) |
| Database | PostgreSQL 16 |
| Cache / presence store | Redis 7 |
| Migrations | Liquibase (YAML changelogs) |
| Real-time | Spring WebSocket + STOMP over SockJS |
| Auth | Spring Security + JWT in httpOnly cookies, session table in DB |
| ORM | Spring Data JPA (Hibernate 6) |
| Frontend | React 18 + Vite + TypeScript |
| UI state | Zustand (client state) + TanStack Query (server state) |
| Styling | Tailwind CSS |
| File storage | Local filesystem via Docker volume |
| Container runtime | Docker Compose (postgres + backend + frontend-nginx) |
| Jabber / XMPP | **Out of scope** for now |

---

## 2. Repository layout

```
webchat_app/
├── backend/
│   ├── build.gradle.kts
│   ├── settings.gradle.kts
│   ├── src/main/java/com/webchat/...
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   └── db/changelog/              Liquibase changesets
│   └── Dockerfile
├── frontend/
│   ├── package.json
│   ├── vite.config.ts
│   ├── tailwind.config.ts
│   ├── src/...
│   └── Dockerfile                     Multi-stage: build → nginx
├── docs/
│   └── design.md                      (this file)
├── docker-compose.yml
├── .env.example
└── README.md
```

Monorepo, one `docker compose up` brings up everything from the repo root (spec §7).

---

## 3. Architecture overview

```
Browser
  │  HTTP (REST)   ── /api/**
  │  WebSocket     ── /ws (STOMP over SockJS)
  ▼
nginx (frontend container, port 80)
  │  static React bundle
  │  reverse-proxy /api and /ws → backend:8080
  ▼
Spring Boot backend (port 8080)
  ├── REST controllers           CRUD, auth, uploads/downloads
  ├── STOMP broker (simple)      fan-out of messages/presence
  ├── Service layer              business rules
  ├── JPA repositories           Postgres
  ├── Redis client               presence state + pub/sub
  └── Local file store           /data/files (mounted volume)
  ▼           ▼
Postgres    Redis (6379)
(5432)      ephemeral presence + pub/sub channel
```

Single backend instance is enough for 300 concurrent users, but Redis is used for presence so restarts don't flap status, and so the architecture stays horizontal-scale-ready.

---

## 4. Data model

Key design decision: **"room chats" and "personal dialogs" are the same entity** (spec §2.5.1). One `chats` table, one `messages` table, with a `type` column distinguishing PUBLIC_ROOM / PRIVATE_ROOM / DIRECT. Direct chats have exactly 2 members and no owner/admins.

### Tables

```
users
  id              BIGSERIAL PK
  email           CITEXT UNIQUE NOT NULL
  username        CITEXT UNIQUE NOT NULL             -- immutable after creation
  password_hash   TEXT NOT NULL                       -- bcrypt
  created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
  deleted_at      TIMESTAMPTZ NULL                    -- soft delete (for message author FK)

sessions
  id              UUID PK
  user_id         BIGINT FK → users ON DELETE CASCADE
  refresh_hash    TEXT NOT NULL                       -- sha256 of refresh token
  user_agent      TEXT
  ip              INET
  created_at      TIMESTAMPTZ NOT NULL
  last_seen_at    TIMESTAMPTZ NOT NULL
  expires_at      TIMESTAMPTZ NOT NULL
  revoked_at      TIMESTAMPTZ NULL

password_reset_tokens
  token_hash      TEXT PK
  user_id         BIGINT FK → users ON DELETE CASCADE
  expires_at      TIMESTAMPTZ NOT NULL
  used_at         TIMESTAMPTZ NULL

chats
  id              BIGSERIAL PK
  type            VARCHAR(20) NOT NULL CHECK (type IN ('PUBLIC_ROOM','PRIVATE_ROOM','DIRECT'))
  name            CITEXT UNIQUE NULL                  -- NULL for DIRECT
  description     TEXT
  owner_id        BIGINT FK → users ON DELETE CASCADE -- cascades delete owner's rooms
  created_at      TIMESTAMPTZ NOT NULL
  CHECK (type='DIRECT' AND name IS NULL AND owner_id IS NULL OR type<>'DIRECT' AND name IS NOT NULL AND owner_id IS NOT NULL)

chat_members
  chat_id         BIGINT FK → chats ON DELETE CASCADE
  user_id         BIGINT FK → users ON DELETE CASCADE
  role            VARCHAR(10) NOT NULL CHECK (role IN ('OWNER','ADMIN','MEMBER'))
  joined_at       TIMESTAMPTZ NOT NULL
  PRIMARY KEY (chat_id, user_id)

chat_bans
  chat_id         BIGINT FK → chats ON DELETE CASCADE
  user_id         BIGINT FK → users ON DELETE CASCADE
  banned_by       BIGINT FK → users ON DELETE SET NULL
  banned_at       TIMESTAMPTZ NOT NULL
  PRIMARY KEY (chat_id, user_id)

chat_invitations
  id              BIGSERIAL PK
  chat_id         BIGINT FK → chats ON DELETE CASCADE
  invitee_id      BIGINT FK → users ON DELETE CASCADE
  invited_by      BIGINT FK → users ON DELETE SET NULL
  created_at      TIMESTAMPTZ NOT NULL
  accepted_at     TIMESTAMPTZ NULL
  declined_at     TIMESTAMPTZ NULL
  UNIQUE (chat_id, invitee_id) WHERE accepted_at IS NULL AND declined_at IS NULL

messages
  id              BIGSERIAL PK
  chat_id         BIGINT FK → chats ON DELETE CASCADE
  author_id       BIGINT FK → users ON DELETE SET NULL     -- survives author deletion in rooms the user didn't own
  reply_to_id     BIGINT FK → messages ON DELETE SET NULL
  body            TEXT                                 -- <= 3 KB (enforced at API)
  created_at      TIMESTAMPTZ NOT NULL
  edited_at       TIMESTAMPTZ NULL
  deleted_at      TIMESTAMPTZ NULL                     -- soft delete to preserve thread replies
  INDEX (chat_id, id DESC)                             -- for infinite scroll

attachments
  id              BIGSERIAL PK
  message_id      BIGINT FK → messages ON DELETE CASCADE
  uploader_id     BIGINT FK → users ON DELETE SET NULL
  original_name   TEXT NOT NULL
  storage_path    TEXT NOT NULL                        -- e.g. "chat-42/7a3e.../file"
  mime_type       TEXT NOT NULL
  size_bytes      BIGINT NOT NULL
  comment         TEXT
  created_at      TIMESTAMPTZ NOT NULL

read_markers
  chat_id         BIGINT FK → chats ON DELETE CASCADE
  user_id         BIGINT FK → users ON DELETE CASCADE
  last_read_message_id  BIGINT
  updated_at      TIMESTAMPTZ NOT NULL
  PRIMARY KEY (chat_id, user_id)

friendships
  user_a_id       BIGINT FK → users ON DELETE CASCADE
  user_b_id       BIGINT FK → users ON DELETE CASCADE
  status          VARCHAR(10) NOT NULL CHECK (status IN ('PENDING','ACCEPTED'))
  initiated_by    BIGINT NOT NULL                      -- user_a or user_b
  request_text    TEXT NULL                            -- optional text (spec §2.3.2)
  created_at      TIMESTAMPTZ NOT NULL
  accepted_at     TIMESTAMPTZ NULL
  PRIMARY KEY (user_a_id, user_b_id)
  CHECK (user_a_id < user_b_id)                         -- canonical ordering to avoid duplicates

user_blocks
  blocker_id      BIGINT FK → users ON DELETE CASCADE
  blocked_id      BIGINT FK → users ON DELETE CASCADE
  created_at      TIMESTAMPTZ NOT NULL
  PRIMARY KEY (blocker_id, blocked_id)
```

### Cascade rules — why these choices (spec §2.1.5)

| When... | Effect |
|---|---|
| User deletes account | `users` row deleted → cascades: sessions, tokens, room memberships, invitations, chats they owned (which cascades to messages/attachments in those rooms), read markers, friendships, blocks |
| User deleted, messages in rooms they didn't own | `author_id` → NULL via SET NULL. UI renders "deleted user". |
| Room deleted | cascade: members, bans, invitations, messages, attachments. Background job removes attachment files from disk. |
| Message deleted | soft delete (`deleted_at`) so replies keep their reference. UI shows "Message deleted". |

### Presence (not persisted to Postgres)

Tracked in Redis — ephemeral, TTL-pruned, and survives backend restarts. See §7.

---

## 5. REST API

All endpoints under `/api`. Auth required except register/login/password-reset/refresh.

### Auth
```
POST   /api/auth/register                        { email, username, password }
POST   /api/auth/login                           { email, password, keepSignedIn }
POST   /api/auth/logout                          (revokes current session)
POST   /api/auth/refresh                         (rotates access token)
POST   /api/auth/password-reset/request          { email }
POST   /api/auth/password-reset/confirm          { token, newPassword }
POST   /api/auth/password-change                 { currentPassword, newPassword }
DELETE /api/auth/account                         { password }  (double-confirm)
GET    /api/auth/sessions                        → list of active sessions
DELETE /api/auth/sessions/{id}                   (revoke one session)
```

### Users
```
GET    /api/users/me
GET    /api/users/search?q=...                   (username prefix search, for friend add)
GET    /api/users/{username}
```

### Friends
```
GET    /api/friends                              (accepted friends)
GET    /api/friends/requests?direction=incoming|outgoing
POST   /api/friends/requests                     { username, text? }
POST   /api/friends/requests/{id}/accept
POST   /api/friends/requests/{id}/decline
DELETE /api/friends/{username}                   (un-friend)
POST   /api/friends/block/{username}
DELETE /api/friends/block/{username}
```

### Rooms (public catalog + management)
```
GET    /api/rooms/public?q=&page=&size=          (catalog, spec §2.4.3)
POST   /api/rooms                                { name, description, visibility }
GET    /api/rooms/{id}
PATCH  /api/rooms/{id}                           (owner only — edit name/desc/visibility)
DELETE /api/rooms/{id}                           (owner only)
POST   /api/rooms/{id}/join
POST   /api/rooms/{id}/leave
GET    /api/rooms/{id}/members
DELETE /api/rooms/{id}/members/{userId}          (admin — remove = ban, spec §2.4.8)
POST   /api/rooms/{id}/admins/{userId}           (owner — promote)
DELETE /api/rooms/{id}/admins/{userId}           (owner or admin demoting another admin, not owner)
GET    /api/rooms/{id}/bans
DELETE /api/rooms/{id}/bans/{userId}             (admin — unban)
POST   /api/rooms/{id}/invitations               { username }
```

### Invitations (receiver side)
```
GET    /api/invitations
POST   /api/invitations/{id}/accept
POST   /api/invitations/{id}/decline
```

### Chats (unified — rooms + DMs)
```
GET    /api/chats                                (sidebar: all chats the user is in, with unread counts)
POST   /api/chats/direct                         { username }  (open/create DM)
GET    /api/chats/{id}/messages?before={id}&limit=50   (infinite scroll, newest-first)
POST   /api/chats/{id}/messages                  { body?, replyToId?, attachmentIds? }
PATCH  /api/messages/{id}                        { body }  (author only)
DELETE /api/messages/{id}                        (author or room admin)
POST   /api/chats/{id}/read                      { lastReadMessageId }
```

### Attachments
```
POST   /api/chats/{id}/attachments               (multipart; returns attachmentId + metadata — attach to message in a second call)
GET    /api/attachments/{id}                     (streams file, access-checked)
```

### Response shape

Consistent envelope is overkill — return the resource directly, use HTTP status codes and standard Problem-Detail (`application/problem+json`) for errors.

---

## 6. WebSocket (STOMP) surface

**Endpoint**: `/ws` (SockJS fallback enabled). Auth via JWT cookie on handshake.

On CONNECT the client sends a `tabId` header (uuid generated client-side, persisted in sessionStorage) — used for presence tracking.

### Subscriptions (server → client)

| Destination | Purpose |
|---|---|
| `/user/queue/notifications` | friend requests, invitations, unread count bumps for chats not currently open |
| `/topic/chat.{chatId}` | new messages, edits, deletes — one subscription per chat the user is in |
| `/user/queue/presence` | presence deltas for any user the client is "watching" (friends + open-room members) |

### Publishes (client → server)

| Destination | Purpose |
|---|---|
| `/app/presence/heartbeat` | every ~30s, keeps tab alive |
| `/app/presence/activity` | throttled to ~5s on any user input; bumps `lastActivityAt` |
| `/app/watch` / `/app/unwatch` | subscribe/unsubscribe to a specific user's presence |

Message SEND goes through REST (`POST /api/chats/{id}/messages`) for easier validation / idempotency; fan-out to `/topic/chat.{id}` happens server-side after DB commit. This keeps the WS surface minimal.

### Delivery SLA (spec §3.2)

- Message delivery ≤ 3s: REST commit + immediate STOMP broadcast → typically <200ms. Safe.
- Presence propagation ≤ 2s: scheduled delta tick every 1s.

---

## 7. Presence model (spec §2.2)

State stored in **Redis**. Backend is stateless w.r.t. presence; restarts don't flap status.

### Redis keys

| Key | Type | Contents | TTL |
|---|---|---|---|
| `presence:user:{userId}` | ZSET | member = `tabId`, score = `lastActivityAtMs` | 120s (refreshed on every activity/heartbeat) |
| `presence:status:{userId}` | STRING | last broadcast status (`ONLINE`/`AFK`/`OFFLINE`) | none — used for delta detection |
| `presence:watchers:{userId}` | SET | userIds subscribed to this user's presence | none |
| channel `presence.deltas` | PUB/SUB | JSON `{ userId, status }` | — |

### Status derivation (computed, not stored)

For user `u`:
- `ZRANGEBYSCORE presence:user:u (now-60000) +inf` → if non-empty → **ONLINE**
- Else `ZCARD presence:user:u > 0` → **AFK**
- Else → **OFFLINE**

One stale-entry sweep per query: `ZREMRANGEBYSCORE presence:user:u 0 (now-120000)` to prune tabs that haven't heartbeated in 2 minutes (covers crashed browsers that never sent `DISCONNECT`).

### Flow

1. Client opens tab → generates `tabId` (uuid, in sessionStorage), connects `/ws`, sends initial `/app/presence/heartbeat`.
2. Backend handler → `ZADD presence:user:{uid} <now> <tabId>` + `EXPIRE presence:user:{uid} 120`.
3. Client throttles `mousemove`/`keypress`/`scroll`/`visibilitychange` to one `/app/presence/activity` every ~5s.
4. Client sends `/app/presence/heartbeat` every 30s unconditionally (keeps tab alive even if idle).
5. WS `DISCONNECT` event → `ZREM presence:user:{uid} <tabId>`.
6. Backend scheduler (1s tick) iterates users with recent presence changes (tracked via a `presence:dirty` SET populated on every write), recomputes status, compares to `presence:status:{uid}`, and on change:
   - `SET presence:status:{uid} <new>`
   - `PUBLISH presence.deltas {userId, status}`
7. Every backend instance subscribes to `presence.deltas`. On message → look up watchers via WS session registry → push to each watcher's `/user/queue/presence`.

### Watchers

When a client opens a chat or loads its contact list, it sends `/app/watch { userIds: [...] }` → backend:
- `SADD presence:watchers:{targetId} {watcherId}` for each target.
- Immediately sends current status back on `/user/queue/presence`.

On WS disconnect, backend removes the user from all `presence:watchers:*` sets it added (tracked per-session in memory).

### Edge cases

- **Backend restart**: Redis keeps presence state; tabs reconnect and continue heartbeating. The in-memory WS session registry rebuilds as clients reconnect. No OFFLINE flap.
- **Redis restart**: presence is lost but rebuilds in <30s as heartbeats flow in. Acceptable.
- **Network drop**: no heartbeat for 120s → tab entry TTL-expires → next derivation returns AFK/OFFLINE → scheduler broadcasts delta.
- **Abrupt browser close without DISCONNECT**: same as network drop — TTL cleans up.

---

## 8. Auth & sessions

- **Access token**: short-lived JWT (15 min), httpOnly cookie `access_token`, SameSite=Lax.
- **Refresh token**: opaque token, **always 30-day TTL**, hashed in `sessions` table, httpOnly cookie `refresh_token` scoped to `/api/auth/refresh`. The "Keep me signed in" checkbox on the login wireframe is kept as UI chrome (spec Appendix A) but has no effect on token lifetime.
- **Multi-session**: one `sessions` row per login. "Active sessions" screen lists them with UA + IP. Revoking one row invalidates that browser only.
- **Sign out**: revokes current session's row, clears cookies. Does not affect other sessions (spec §2.1.3, §2.2.4).
- **Password change/reset**: optionally revoke all other sessions (ask user on password-change screen).

Passwords hashed with **bcrypt** (strength 12) via Spring Security's `BCryptPasswordEncoder`.

**Password policy** (enforced at registration, password-change, and password-reset):
- Minimum 12 characters
- At least 1 lowercase letter
- At least 1 uppercase letter
- At least 1 digit
- At least 1 special symbol (any non-alphanumeric)

**Username policy**: 3–32 characters, `[a-zA-Z0-9_.-]`. Immutable after registration (spec §2.1.2).

**Password reset delivery**: no SMTP. The reset link is logged to the backend console (`logger.info`). Good enough for homework/demo; swap for real email later by plugging in a `MailSender` bean.

---

## 9. File storage

- Volume `files` mounted at `/data/files` in backend container.
- Path layout: `/data/files/chat-{chatId}/{uuid}/{originalNameSanitized}`.
- On upload: Spring Boot multipart → validate size (20 MB files, 3 MB images by mime sniff) → write to disk → insert `attachments` row → return id.
- On download: access check (user is member of chat AND not banned) → stream via `StreamingResponseBody`.
- On chat delete: cascade in DB, plus background task deletes `/data/files/chat-{chatId}/` directory.
- On user delete: chats they owned are cascaded (which triggers file cleanup above). Attachments in other chats persist (spec §2.6.5, §5 notes).

---

## 10. Access-control rules — consolidated

| Action | Allowed when |
|---|---|
| Read messages in chat | User is member of chat AND chat not deleted |
| Post to room | Member AND not banned from room |
| Post to DM | Both participants are friends AND neither has blocked the other (spec §2.3.6) |
| Edit message | User is author AND message not deleted |
| Delete message | Author OR (chat type in [PUBLIC_ROOM, PRIVATE_ROOM] AND user is admin of that chat) |
| Join public room | Chat exists, is PUBLIC_ROOM, user not banned from chat |
| Join private room | User has a pending invitation to that chat |
| Invite to private room | Inviter is member of chat (any role) |
| Manage room (kick/ban/admin) | User is admin; owner cannot lose admin |
| Delete room | User is owner |
| Download attachment | User is current member of chat AND not banned |

Encoded as Spring Security `@PreAuthorize` on service methods + policy helper class.

---

## 11. UI structure

Routes:
- `/login`, `/register`, `/forgot-password`, `/reset-password/:token` — public
- `/` — app shell (auth required): top menu, right sidebar, chat pane, right members panel
- `/chat/:id` — specific chat open (shareable URL)
- `/profile`, `/sessions`, `/friends`, `/rooms/public` — secondary screens

Component tree (high-level):
```
<App>
  <AuthGate>
    <TopMenu />
    <Layout>
      <Sidebar>                          accordion: rooms + contacts
      <ChatPane>
        <ChatHeader />
        <MessageList />                  infinite scroll, stick-to-bottom logic
        <Composer />                     multiline + emoji + attach + reply indicator
      <MembersPanel>                     room members with presence; [Invite user], [Manage room]
      <ManageRoomModal>                  tabs: Members / Admins / Banned / Invitations / Settings
```

State:
- Zustand stores: `authStore`, `uiStore` (active chat, sidebar state), `presenceStore` (userId → status).
- TanStack Query: all REST-fetched data (chats, messages, members, etc.). WS events invalidate or patch query caches.

Emoji: **`emoji-mart`** library for the composer picker (spec §2.5.2, §4.3).

---

## 12. Docker Compose

```yaml
services:
  postgres:
    image: postgres:16
    environment:
      POSTGRES_DB: webchat
      POSTGRES_USER: webchat
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
    volumes: [pgdata:/var/lib/postgresql/data]

  redis:
    image: redis:7-alpine
    command: ["redis-server", "--save", "", "--appendonly", "no"]   # ephemeral: no persistence
    volumes: []

  backend:
    build: ./backend
    depends_on: [postgres, redis]
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/webchat
      SPRING_DATASOURCE_USERNAME: webchat
      SPRING_DATASOURCE_PASSWORD: ${POSTGRES_PASSWORD}
      SPRING_DATA_REDIS_HOST: redis
      SPRING_DATA_REDIS_PORT: 6379
      JWT_SECRET: ${JWT_SECRET}
      FILE_STORAGE_PATH: /data/files
    volumes: [files:/data/files]

  frontend:
    build: ./frontend
    depends_on: [backend]
    ports: ["80:80"]

volumes:
  pgdata:
  files:
```

`.env.example` in repo; real `.env` gitignored.

`docker compose up` from the repo root brings up all three services (spec §7).

---

## 13. Delivery phases

Each phase ends with a working demo.

| # | Phase | Scope |
|---|---|---|
| 1 | Skeleton | Spring Boot empty app, React empty app, Postgres, Redis, Liquibase baseline, docker compose boots all services, health endpoint, smoke test |
| 2 | Auth | Users table, register/login/logout, JWT cookies, session table + active-sessions screen, password change/reset, delete account |
| 3 | Rooms core | Chats table (room variant), membership, roles, bans, invitations, public catalog, create/join/leave/delete |
| 4 | Messaging | Messages table, send/edit/delete/reply, REST + WebSocket fan-out, infinite scroll |
| 5 | Friends & DMs | Friendships, requests, blocks, DM chats as 2-person DIRECT chats |
| 6 | Attachments | Upload/download, copy-paste, access control, disk cleanup |
| 7 | Presence | Tab tracking, heartbeat/activity, online/AFK/offline deltas |
| 8 | UI polish | Match wireframes, unread indicators, manage-room modal tabs, emoji picker |

Optional, only if everything above is solid: Jabber/XMPP + federation.

---

## 14. Resolved decisions

1. **Refresh token lifetime**: always 30 days. "Keep me signed in" checkbox kept as UI chrome only.
2. **Username policy**: 3–32 chars, `[a-zA-Z0-9_.-]`, immutable.
3. **Password policy**: min 12 chars with lower + upper + digit + special.
4. **Password reset delivery**: log reset link to backend console (no SMTP).
5. **Room settings editing** (name/description/visibility): owner only.
6. **Rate limiting**: deferred until needed.
7. **Emoji picker**: `emoji-mart`.

---

Ready to proceed to Phase 1 (skeleton). Call out anything to change before I start.

# Architecture

This describes how we will build the system in `spec/requirements.md`. Guidelines in `rules/` apply. There is no login, deletion, notifications, or pagination in this assignment.

## 1. Overall shape

Three parts, two processes at runtime:

```text
Browser  →  Next.js frontend (React)
                ↓  HTTP JSON `/api/v1`
            Spring Boot backend (Java 21)
                ↓  JPA
            PostgreSQL  (or H2 file DB for local)
```

- **Frontend:** screens, forms, search/filter, and display of API errors.
- **Backend:** REST API, validation, status state machine, persistence.
- **Database:** tickets and comments. Nothing is kept only in memory for the running app.

The frontend does not talk to the database. The backend is the only place that writes data or decides whether a status change is legal.

**Assumption:** frontend on `http://localhost:3000`, backend on `http://localhost:8080`. CORS on the backend allows only that frontend origin.

## 2. Spring Boot backend structure

Package **by layer** under a single application package (for example `com.example.tickets`):

```text
controller/     HTTP adapters
service/        use cases and business rules
repository/     Spring Data JPA
entity/         Ticket, Comment, enums
dto/            request and response records
exception/      not-found, invalid transition, plus @RestControllerAdvice
```

- Java 21, Spring Boot 3.x, constructor injection, records for DTOs.
- Controllers never call repositories.
- Entities never leave the backend as JSON; map to DTOs in the service.
- No Spring Security, no Lombok unless we later decide otherwise.

## 3. React / Next.js frontend structure

Keep a small App Router app:

```text
app/
  page.tsx                 ticket list (search + status filter + create)
  tickets/[id]/page.tsx    ticket details (edit, assignee, status, comments)
lib/
  api.ts                   fetch wrappers for `/api/v1`
  types.ts                 TypeScript types matching API JSON
components/                list table, ticket form, comment list, error banner
```

- Client components call the Spring API directly (browser → backend).
- List query params: `q` (keyword) and `status` (optional). No page/size.
- Show a visible error from the API `message` / `fieldErrors`; do not update local state as if a failed save succeeded.

## 4. Controller, service, repository

| Layer | Does | Does not |
| --- | --- | --- |
| **Controller** | Map URL and HTTP method, `@Valid` on bodies, call service, return DTO and status code | Status rules, queries, transactions |
| **Service** | One public method per use case; set initial `OPEN`; enforce transitions; map entity ↔ DTO; `@Transactional` on writes | HTTP, SQL |
| **Repository** | Save/load tickets and comments; list by keyword and/or status | Business rules, HTTP |

Use cases on the service: create ticket, list tickets, get ticket, update title/description/priority, change assignee, change status, add comment.

## 5. Ticket and comment relationship

- One **ticket** has many **comments**.
- Comment rows have a foreign key to the ticket. Deleting a ticket is out of scope; no cascade-delete requirement.
- Ticket details response includes comments, oldest first.
- Adding a comment does not change ticket status, title, or assignee.

**Assumption (from requirements):** assignee is optional free text on the ticket, not a user table. Comments store text and created timestamp only (no author).

## 6. REST API and the frontend

JSON, camelCase, `/api/v1`, as in `rules/api-standards.md`, limited to what the requirements need.

| Action | Method | Path | Success |
| --- | --- | --- | --- |
| Create ticket | `POST` | `/api/v1/tickets` | `201` + `Location` |
| List tickets | `GET` | `/api/v1/tickets?q=&status=` | `200` JSON array |
| Get ticket | `GET` | `/api/v1/tickets/{ticketId}` | `200` ticket + comments |
| Update title/description/priority | `PATCH` | `/api/v1/tickets/{ticketId}` | `200` |
| Change status | `PATCH` | `/api/v1/tickets/{ticketId}/status` | `200` |
| Change assignee | `PATCH` | `/api/v1/tickets/{ticketId}/assignee` | `200` |
| Add comment | `POST` | `/api/v1/tickets/{ticketId}/comments` | `201` |

- List returns a **JSON array**, not a page object. Pagination in the API guidelines is not used for this assignment.
- List `status` omitted or empty means all statuses. `q` omitted or empty means no keyword filter. Both may be used together.
- Create does not accept `status`; backend sets `OPEN`.
- Frontend maps each action to the matching method/path. No extra BFF layer.

**Assumption:** ticket and comment ids are server-generated `Long` values (simpler than UUID for this exercise). JSON field names stay camelCase (`ticketId`, `createdAt`).

## 7. Validation and error handling

Two steps, then a single `@RestControllerAdvice`:

1. **Request validation (controller):** Jakarta Bean Validation on DTOs — non-blank title, description, comment text; required known **priority** (`LOW` / `MEDIUM` / `HIGH` — requirements assumption). Unknown enum / bad JSON → `400` `VALIDATION_ERROR` or `MALFORMED_REQUEST`.
2. **Business validation (service):** ticket exists; status values; **state machine**. Missing ticket → `TicketNotFoundException` → `404` `TICKET_NOT_FOUND`. Illegal transition → `InvalidStatusTransitionException` → `409` `INVALID_STATUS_TRANSITION`.
3. **Unexpected errors** → `500` `INTERNAL_ERROR`, log server-side, no stack traces in JSON.

Error JSON follows `rules/api-standards.md`. The UI always surfaces `message` (and `fieldErrors` when present). After `409` on status change, the UI keeps the previous status (reload from GET if needed).

## 8. Where status transitions are enforced

**Only in the backend service** (one place, e.g. the change-status method or a small helper it calls).

Allowed (from `spec/requirements.md`):

```text
OPEN        → IN_PROGRESS | CANCELLED
IN_PROGRESS → RESOLVED    | CANCELLED
RESOLVED    → CLOSED
```

Anything else, including same-status and changes from `CLOSED` or `CANCELLED`, is rejected with `409`. The UI may hide illegal options, but that is not the source of truth.

On create, the service sets `OPEN`; it does not trust a client-sent status.

## 9. Database and persistence

- **PostgreSQL** for a realistic run (and for evaluation if a real DB is expected).
- **H2 file-based** is allowed for local convenience. Not in-memory for the app, so data survives restart.
- Spring Data JPA. Schema via **Flyway** (not `ddl-auto=update` for the app).
- Tables: `tickets`, `comments`. Index `tickets.status` for filtering. Keyword search can use JPA on title and description (case-insensitive contains); no extra search engine.

**Assumption:** Flyway over Liquibase. Priority and status stored as strings matching the enum names.

## 10. Testing boundaries

Follow `rules/testing.md`. The statuses and transitions to test are those in `spec/requirements.md` only:

**Statuses:** `OPEN`, `IN_PROGRESS`, `RESOLVED`, `CLOSED`, `CANCELLED`

**Allowed transitions:**

```text
OPEN         → IN_PROGRESS
OPEN         → CANCELLED
IN_PROGRESS  → RESOLVED
IN_PROGRESS  → CANCELLED
RESOLVED     → CLOSED
```

Any other transition is illegal and must be tested as a rejection (`409`).

| Boundary | Tooling | Covers |
| --- | --- | --- |
| Unit (service) | JUnit 5, Mockito | Create → `OPEN`; the five allowed transitions; illegal transitions; update fields; assignee; comments; not found |
| Controller | `@WebMvcTest` | HTTP codes, JSON, `@Valid`; mock service; no DB |
| Repository | `@DataJpaTest` + H2 | Keyword + status list queries |
| Integration | `@SpringBootTest` + MockMvc | Create → get; a happy path using allowed transitions; one illegal transition `409`; persist comments |

Prefer many service unit tests. A few integration tests for wiring and persistence. No security tests.

## 11. Local development configuration

- Spring profiles: `local` (default for developers).
- `local`: H2 file under a gitignored folder (or PostgreSQL via env vars if present). Flyway on. CORS for `http://localhost:3000`.
- Database URL, user, password from environment or `application-local.yml` (not committed; see `.gitignore`).
- Next.js: `NEXT_PUBLIC_API_BASE_URL=http://localhost:8080` in `.env.local` (not committed). `.env.example` may list the key only.
- Run backend (`8080`) and frontend (`3000`) separately. No Docker required for the default H2 path.

**Assumption:** a `prod`-like PostgreSQL profile can be added later; it is not needed to start local work.

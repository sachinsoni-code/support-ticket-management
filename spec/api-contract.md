# API contract

REST contract for the Support Ticket Management System. Sources: `spec/requirements.md`, `spec/architecture.md`, `spec/data-model.md`, `rules/api-standards.md`.

Base path: `/api/v1`. JSON only (`Content-Type: application/json`). No authentication, no delete, no pagination.

Path parameter `{ticketId}` is a numeric `Long` (database identity). Unknown or non-numeric id → `404` `TICKET_NOT_FOUND` (non-numeric may instead be `400` `MALFORMED_REQUEST`; clients must not rely on that distinction).

---

## Conventions

### JSON naming

- camelCase: `ticketId`, `createdAt`, `updatedAt`
- Do not wrap success payloads (`data` / `success` wrappers are not used)
- Ids are JSON numbers, not strings
- Assignee is a string or `null`, not `assigneeId`
- There is no `createdBy` field

### Date/time

- ISO-8601 UTC with `Z`, e.g. `2026-09-22T08:30:00Z`
- Maps to DB `TIMESTAMP` stored in UTC

### Status values

`OPEN` | `IN_PROGRESS` | `RESOLVED` | `CLOSED` | `CANCELLED`

### Priority values

`LOW` | `MEDIUM` | `HIGH` (assumption from requirements; assignment does not define the labels)

### Allowed status transitions

Exactly the requirements state machine. Enforced by the backend. Any other change → `409` `INVALID_STATUS_TRANSITION`.

```text
OPEN         → IN_PROGRESS
OPEN         → CANCELLED
IN_PROGRESS  → RESOLVED
IN_PROGRESS  → CANCELLED
RESOLVED     → CLOSED
```

Rejected examples: skip (`OPEN` → `RESOLVED` / `CLOSED`), reverse (`IN_PROGRESS` → `OPEN`), `RESOLVED` → `CANCELLED`, any change from `CLOSED` or `CANCELLED`, same-status (`OPEN` → `OPEN`). Create always results in `OPEN`; clients cannot set initial status.

---

## Common response objects

### Ticket (list item)

Used in list responses. Comments are omitted.

```json
{
  "ticketId": 1,
  "title": "Cannot reset password",
  "description": "Reset link returns 500.",
  "status": "OPEN",
  "priority": "HIGH",
  "assignee": null,
  "createdAt": "2026-09-22T08:30:00Z",
  "updatedAt": "2026-09-22T08:30:00Z"
}
```

`assignee` is `null` when unassigned.

### Ticket (details)

Same fields as the list item, plus `comments` (oldest first). Used for create, get, field update, status change, and assignee change.

```json
{
  "ticketId": 1,
  "title": "Cannot reset password",
  "description": "Reset link returns 500.",
  "status": "OPEN",
  "priority": "HIGH",
  "assignee": "Alex",
  "createdAt": "2026-09-22T08:30:00Z",
  "updatedAt": "2026-09-22T08:31:00Z",
  "comments": []
}
```

### Comment

```json
{
  "commentId": 10,
  "ticketId": 1,
  "body": "Tried a second reset, still 500.",
  "createdAt": "2026-09-22T08:35:00Z"
}
```

---

## Error formats

All errors use this shape. Omit `fieldErrors` when there are none.

```json
{
  "timestamp": "2026-09-22T08:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "code": "VALIDATION_ERROR",
  "message": "Request validation failed",
  "path": "/api/v1/tickets",
  "fieldErrors": [
    {
      "field": "title",
      "message": "must not be blank"
    }
  ]
}
```

| Code | HTTP | When |
| --- | --- | --- |
| `VALIDATION_ERROR` | `400` | Bean validation failed |
| `MALFORMED_REQUEST` | `400` | Invalid JSON or wrong types |
| `TICKET_NOT_FOUND` | `404` | Unknown `ticketId` |
| `INVALID_STATUS_TRANSITION` | `409` | Status change not in the state machine |
| `INTERNAL_ERROR` | `500` | Unexpected failure; `message` is safe for UI, no stack traces |

### Validation (`400`)

`code`: `VALIDATION_ERROR`. `fieldErrors` lists each field. Typical: blank `title` / `description` / `body`, missing `priority`, unknown `priority` or `status` enum, value longer than the data-model max (`title` 255, `description` 4000, `assignee` 255, `body` 4000).

### Not found (`404`)

`code`: `TICKET_NOT_FOUND`. No `fieldErrors`. Example `message`: `Ticket 99 was not found`.

### Invalid status transition (`409`)

`code`: `INVALID_STATUS_TRANSITION`. Example `message`: `Cannot change status from OPEN to CLOSED`. Ticket is unchanged.

### General / unexpected (`500`)

`code`: `INTERNAL_ERROR`.

---

## Endpoints

### 1. Create a ticket

| | |
| --- | --- |
| **Method / path** | `POST /api/v1/tickets` |
| **Purpose** | Create a ticket. Status is set to `OPEN` by the server. |

**Request body**

| Field | Required | Rules |
| --- | --- | --- |
| `title` | yes | non-blank, max 255 |
| `description` | yes | non-blank, max 4000 |
| `priority` | yes | `LOW`, `MEDIUM`, or `HIGH` |
| `assignee` | no | omit, `null`, or non-blank string max 255; blank string is rejected or treated as `null` (implementation may pick one; prefer `null` to mean unset) |

`status` must not be accepted. Extra unknown fields may be ignored or `400`; clients must not send `status` or ids.

```json
{
  "title": "Cannot reset password",
  "description": "Reset link returns 500.",
  "priority": "HIGH"
}
```

**Success:** `201 Created`. Header `Location: /api/v1/tickets/{ticketId}`. Body: ticket details with `status` `OPEN` and `comments: []`.

**Errors:** `400` validation / malformed JSON; `500`.

---

### 2. List, search, and filter tickets

| | |
| --- | --- |
| **Method / path** | `GET /api/v1/tickets` |
| **Purpose** | Return all tickets, optionally filtered. No pagination. |

**Query parameters**

| Name | Required | Rules |
| --- | --- | --- |
| `q` | no | Keyword. Case-insensitive contains on **title or description**. Omit or empty = no keyword filter. Comments are not searched. |
| `status` | no | One of the five status values. Omit or empty = all statuses. Unknown value → `400`. |

`q` and `status` together use **AND**.

**Request body:** none.

**Success:** `200 OK`. JSON **array** of ticket list items (no `comments`). Empty list is `[]`. Order: newest first by `createdAt` (assumption for a stable UI).

**Errors:** `400` if `status` is present and not a valid value; `500`.

---

### 3. Get ticket details

| | |
| --- | --- |
| **Method / path** | `GET /api/v1/tickets/{ticketId}` |
| **Purpose** | One ticket including comments, oldest first. |

**Request body:** none.

**Success:** `200 OK`. Ticket details object.

**Errors:** `404` `TICKET_NOT_FOUND`; `500`.

---

### 4. Update ticket fields

| | |
| --- | --- |
| **Method / path** | `PATCH /api/v1/tickets/{ticketId}` |
| **Purpose** | Update title, description, and priority. Does not change status, assignee, or comments. |

**Request body** — all three fields required on every call (so a field cannot be cleared by omission):

| Field | Required | Rules |
| --- | --- | --- |
| `title` | yes | non-blank, max 255 |
| `description` | yes | non-blank, max 4000 |
| `priority` | yes | `LOW`, `MEDIUM`, or `HIGH` |

Do not send `status` or `assignee` here.

**Success:** `200 OK`. Ticket details. `updatedAt` is refreshed.

**Errors:** `400` validation; `404` not found; `500`. Allowed even when status is `CLOSED` or `CANCELLED` (the assignment does not forbid field edits on terminal tickets).

---

### 5. Change ticket status

| | |
| --- | --- |
| **Method / path** | `PATCH /api/v1/tickets/{ticketId}/status` |
| **Purpose** | Move the ticket to a new status if the transition is allowed. |

**Request body**

| Field | Required | Rules |
| --- | --- | --- |
| `status` | yes | One of the five status values |

```json
{
  "status": "IN_PROGRESS"
}
```

**Success:** `200 OK` only if current → new is one of the five allowed transitions. Ticket details; `updatedAt` refreshed.

**Errors:**

- `400` if `status` missing or not a known value
- `404` if ticket does not exist
- `409` `INVALID_STATUS_TRANSITION` if the pair is not allowed (including same status, terminal → anything, skips, reverses)
- `500`

The UI must keep the previous status when this returns `409`.

---

### 6. Assign, change, or clear assignee

| | |
| --- | --- |
| **Method / path** | `PATCH /api/v1/tickets/{ticketId}/assignee` |
| **Purpose** | Set, replace, or clear assignee. Does not change status. |

**Request body**

| Field | Required | Rules |
| --- | --- | --- |
| `assignee` | yes (nullable) | String max 255 to set/change; JSON `null` to clear |

```json
{ "assignee": "Alex" }
```

```json
{ "assignee": null }
```

**Success:** `200 OK`. Ticket details; `assignee` is the new value or `null`; `updatedAt` refreshed.

**Errors:** `400` if the field is missing entirely or is a blank string; `404`; `500`.

---

### 7. Add a comment

| | |
| --- | --- |
| **Method / path** | `POST /api/v1/tickets/{ticketId}/comments` |
| **Purpose** | Append a comment. Does not change ticket status, title, description, priority, or assignee. |

**Request body**

| Field | Required | Rules |
| --- | --- | --- |
| `body` | yes | non-blank, max 4000 |

```json
{
  "body": "Tried a second reset, still 500."
}
```

**Success:** `201 Created`. Header `Location: /api/v1/tickets/{ticketId}` (ticket resource; there is no get-comment-by-id endpoint). Body: the created **comment** object.

**Errors:** `400` validation; `404` if the ticket does not exist; `500`.

There is no list-comments endpoint; clients load comments via get ticket details.

---

## Endpoint summary

| Action | Method | Path | Success |
| --- | --- | --- | --- |
| Create ticket | `POST` | `/api/v1/tickets` | `201` |
| List / search / filter | `GET` | `/api/v1/tickets` | `200` array |
| Get details | `GET` | `/api/v1/tickets/{ticketId}` | `200` |
| Update fields | `PATCH` | `/api/v1/tickets/{ticketId}` | `200` |
| Change status | `PATCH` | `/api/v1/tickets/{ticketId}/status` | `200` or `409` |
| Change assignee | `PATCH` | `/api/v1/tickets/{ticketId}/assignee` | `200` |
| Add comment | `POST` | `/api/v1/tickets/{ticketId}/comments` | `201` |

# API Standards

REST conventions for the Support Ticket Management backend. Frontend and AI-generated code should follow this file.

## Base URL and versioning

- Prefix all HTTP APIs with `/api/v1`.
- Example: `/api/v1/tickets`, `/api/v1/tickets/{ticketId}`.
- Do not put verbs in the path (`/api/v1/tickets/create` is wrong).

## Resources and HTTP methods

| Action | Method | Path | Success |
| --- | --- | --- | --- |
| Create ticket | `POST` | `/api/v1/tickets` | `201 Created` |
| List tickets | `GET` | `/api/v1/tickets` | `200 OK` |
| Get ticket | `GET` | `/api/v1/tickets/{ticketId}` | `200 OK` |
| Update ticket fields | `PATCH` | `/api/v1/tickets/{ticketId}` | `200 OK` |
| Change status | `PATCH` | `/api/v1/tickets/{ticketId}/status` | `200 OK` |
| Assign ticket | `PATCH` | `/api/v1/tickets/{ticketId}/assignee` | `200 OK` |

- Use `PUT` only for full replacement (we prefer `PATCH` for partial updates).
- Path IDs are the ticket id (`UUID` or numeric — match the spec). Use `{ticketId}`, not `{id}`, in docs.

Query parameters for list (when implemented): `status`, `assigneeId`, `page`, `size`, `sort`.

## HTTP status codes

| Status | When |
| --- | --- |
| `200 OK` | Successful GET/PATCH (body returned) |
| `201 Created` | Successful POST. Include `Location: /api/v1/tickets/{ticketId}` |
| `400 Bad Request` | Malformed JSON, failed bean validation, unknown enum |
| `404 Not Found` | Ticket (or other resource) does not exist |
| `409 Conflict` | Illegal ticket status transition or other business conflict |
| `500 Internal Server Error` | Unexpected failure. No internal details in the body |

Do not use `200` for created resources or for errors.

## Request and response format

- JSON only. `Content-Type: application/json`.
- Field names: **camelCase** (`createdAt`, `ticketId`, `assigneeId`).
- Timestamps: ISO-8601 UTC (`2026-09-22T08:30:00Z`).
- Enums: uppercase strings (`OPEN`, `IN_PROGRESS`, `RESOLVED`, `CLOSED`).
- Do not wrap successful payloads in `{ "success": true, "data": ... }`. Return the resource directly.

Create example:

```json
{
  "title": "Cannot reset password",
  "description": "Reset link returns 500.",
  "priority": "HIGH"
}
```

Ticket response example:

```json
{
  "ticketId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "title": "Cannot reset password",
  "description": "Reset link returns 500.",
  "status": "OPEN",
  "priority": "HIGH",
  "assigneeId": null,
  "createdBy": "user-123",
  "createdAt": "2026-09-22T08:30:00Z",
  "updatedAt": "2026-09-22T08:30:00Z"
}
```

Status change request:

```json
{
  "status": "IN_PROGRESS"
}
```

List responses use a simple page object:

```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0
}
```

## Validation errors (`400`)

Return a consistent body when `@Valid` fails or the payload is unreadable:

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

- `fieldErrors` is present only for field-level validation.
- One entry per field. Do not return framework stack traces.

## Common API error responses

All errors share this shape (omit `fieldErrors` when not needed):

```json
{
  "timestamp": "2026-09-22T08:30:00Z",
  "status": 409,
  "error": "Conflict",
  "code": "INVALID_STATUS_TRANSITION",
  "message": "Cannot change status from OPEN to CLOSED",
  "path": "/api/v1/tickets/a1b2c3d4-e5f6-7890-abcd-ef1234567890/status"
}
```

Stable `code` values:

| Code | HTTP | Meaning |
| --- | --- | --- |
| `VALIDATION_ERROR` | `400` | Request body/query failed validation |
| `MALFORMED_REQUEST` | `400` | Invalid JSON or wrong types |
| `TICKET_NOT_FOUND` | `404` | Unknown ticket id |
| `INVALID_STATUS_TRANSITION` | `409` | Status change is not allowed |
| `INTERNAL_ERROR` | `500` | Unexpected error |

- `message` is safe to show in a UI. It must not include SQL, class names, or stack traces.

## Other conventions

- Pagination is zero-based (`page=0` is the first page). Default `size` is 20; max `size` is 100.
- Idempotency: `GET` is safe. `PATCH` status with an illegal transition always returns `409`, never silently ignores.
- Breaking changes require `/api/v2`. Additive optional fields in responses are allowed in v1.

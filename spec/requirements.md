# Support Ticket Management System — Requirements

This document is the build scope. If a feature is not listed here, we do not implement it.

## 1. What the system does

A support team uses this application to record, track, and update support tickets.

Users can:

- Create a ticket
- See a list of tickets and open one ticket’s details
- Update title, description, and priority
- Change the assignee
- Add comments on a ticket
- Search tickets by keyword
- Filter tickets by status

Tickets are stored in a database. The backend enforces validation and the status state machine. The UI shows errors when a request fails.

Out of scope: login, roles, authentication, ticket deletion, notifications, email, dashboards, file attachments, and any status change not listed in section 12.

## 2. Ticket creation

A user can create a ticket with:

- Title (required)
- Description (required)
- Priority (required)
- Assignee (optional)

On create:

- Status is **OPEN**
- The ticket is saved in the database and shown in the list
- The user is taken to the ticket details (or the list with the new ticket visible)

## 3. Ticket listing

The list shows existing tickets. Each row includes enough to identify the ticket:

- Title
- Status
- Priority
- Assignee (empty if none)

The list supports:

- **Search by keyword** (section 8)
- **Filter by status** (section 9)

Selecting a row opens ticket details.

## 4. Ticket details

The details view shows one ticket:

- Title, description, priority, status, assignee
- Created and last-updated timestamps
- Comment thread (oldest first)

From details, the user can:

- Edit title, description, and priority
- Change assignee
- Change status (only allowed transitions)
- Add a comment

## 5. Update title, description, and priority

The user can change title, description, and priority on an existing ticket.

- All three remain required after update (no blank title or description)
- Status does not change as a side effect of this update
- Changes are persisted and visible immediately after a successful save

## 6. Change assignee

The user can set, change, or clear the assignee on an existing ticket.

- Assignee is not required
- Changing assignee does not change status by itself
- The new assignee is persisted and shown on list and details

## 7. Comments

The user can add a comment on a ticket.

- Comment text is required
- Comments are stored with the ticket and a created timestamp
- Comments cannot be edited or deleted (not required)
- Adding a comment does not change ticket status

## 8. Search by keyword

The list can be searched by a keyword.

- Match tickets whose title or description contains the keyword
- Search is case-insensitive
- An empty keyword means no search filter
- Search can be combined with status filter

## 9. Filter by status

The list can be filtered by ticket status.

- Allowed filter values: `OPEN`, `IN_PROGRESS`, `RESOLVED`, `CLOSED`, `CANCELLED`
- “All” (or no filter) shows every status
- Filter can be combined with keyword search

## 10. Database persistence

All of the following are stored in the database and survive application restart:

- Tickets (title, description, priority, status, assignee, timestamps)
- Comments (text, ticket reference, timestamp)

Do not keep tickets only in memory.

## 11. Backend validation

The backend rejects invalid requests. At minimum:

| Rule | When |
| --- | --- |
| Title is required and not blank | Create and update |
| Description is required and not blank | Create and update |
| Priority is required and must be a known value | Create and update |
| Status must be a known value | Status change |
| Status change must follow the state machine | Status change |
| Ticket id must exist | Get, update, status, assignee, comment |
| Comment text is required and not blank | Add comment |

Invalid input does not write a partial/invalid ticket.

## 12. Ticket status state machine

**Statuses:** `OPEN`, `IN_PROGRESS`, `RESOLVED`, `CLOSED`, `CANCELLED`

**Initial status:** `OPEN` (set by the backend on create; the client cannot pick another initial status)

**Allowed transitions:**

```text
OPEN         → IN_PROGRESS
OPEN         → CANCELLED
IN_PROGRESS  → RESOLVED
IN_PROGRESS  → CANCELLED
RESOLVED     → CLOSED
```

**Rejected (any other change):** including but not limited to:

- `OPEN` → `RESOLVED` or `CLOSED`
- `IN_PROGRESS` → `OPEN` or `CLOSED`
- `RESOLVED` → `OPEN`, `IN_PROGRESS`, or `CANCELLED`
- `CLOSED` → any status
- `CANCELLED` → any status
- Same-status updates (e.g. `OPEN` → `OPEN`)

`CLOSED` and `CANCELLED` are terminal.

The backend rejects illegal transitions with a conflict error. The UI must not treat them as success.

## 13. Error handling in the UI

When the backend returns an error, the UI must show a clear message and must not look as if the save succeeded.

Cover at least:

- Validation failure (blank title/description, invalid priority)
- Ticket not found
- Illegal status transition
- Unexpected server/network failure

After a failed status change, the ticket stays on the previous status.

## 14. Acceptance criteria

The system is acceptable when all of the following hold:

1. A new ticket can be created with title, description, and priority, and is stored as `OPEN`.
2. Created tickets appear in the list and can be opened in a details view.
3. Title, description, and priority can be updated and the new values persist after reload.
4. Assignee can be set, changed, and cleared; the change persists.
5. A comment can be added and remains visible on that ticket after reload.
6. Keyword search returns tickets matching title or description.
7. Status filter shows only tickets in the selected status.
8. Search and status filter work together.
9. Only the five allowed status transitions succeed; every other transition is rejected by the backend.
10. `CLOSED` and `CANCELLED` tickets cannot be moved to another status.
11. Invalid create/update payloads are rejected by the backend.
12. The UI shows an error for validation, not-found, illegal transition, and unexpected failures.
13. Data is still present after restarting the application (database persistence).

## 15. Assumptions

These are not extra features; they fill gaps so we can implement consistently:

- **Tech:** Java 21 / Spring Boot backend, React/Next.js frontend, relational database (see architecture later).
- **Priority values (assumption):** The assignment requires a priority field but does not define the allowed values. Initial values for implementation: `LOW`, `MEDIUM`, `HIGH`.
- **Assignee (implementation assumption):** The assignment only says that an assignee can be changed. It does not define users or an assignee type. We store assignee as optional free text.
- **Comment author:** not required (no login). Store comment text and timestamp only.
- **API shape:** follow `rules/api-standards.md` (REST, JSON, status codes), except anything this spec does not need.
- **IDs:** each ticket and comment has a server-generated id.
- **Pagination:** Not required for the current assignment. Return the full matching list. Pagination conventions in `rules/api-standards.md` are for possible future use only; they are not a required feature now.

## 16. Open questions

None that block a first architecture pass. If the written assignment disagrees with an assumption above (priority values, assignee as free text, comment author), the assignment wins and this file should be updated before implementation.

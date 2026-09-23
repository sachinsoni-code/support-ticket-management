# Testing Strategy for Support Ticket Management System

This document defines the testing strategy for the Support Ticket Management System, as specified in the assignment and based on the requirements and architecture documents, the API contract, state-machine, and project testing rules.

**Scope:**  
All test plans described here are focused on the current assignment. No authentication, authorization, deletion, notification, or external feature tests are included.

---

## 1. Service/Unit Tests

**Objectives:**  
- Verify domain/service logic isolated from the web and persistence layers.
- Focus on the core business rules, state-machine transitions, and data validation.

**Areas to Cover:**

- **Ticket Creation and Validation**  
  - Creating tickets with required and optional fields.  
  - Enforcing validation: required fields, max lengths, valid enum values, no initial status allowed.

- **Ticket Field Updates**  
  - Updating title, description, and priority (all fields present; partial updates rejected).  
  - Validation of required fields and allowed values on update.

- **Assignee Changes**  
  - Setting, changing, and clearing assignee (null or valid string; blank rejected; limit string length).

- **Comment Creation**  
  - Adding comments, validating body content is non-blank and under limit, correct association with ticket.

- **State-Machine Enforcement**  
  - All valid status transitions succeed and return the updated state.
  - All invalid transitions (including reverse, skip, terminal, or same-status) are rejected with no ticket update.
  - CLOSED and CANCELLED are terminal; no outbound transitions allowed.
  - Explicitly test that invalid transitions do not change ticket state or other fields.

---

## 2. Controller/API Tests

**Objectives:**  
- Verify the REST interface exactly matches the API contract.
- Enforce HTTP status codes, response structure, and error handling.

**Areas to Cover:**

- **Request Validation**  
  - All required fields enforced; extra/unknown fields rejected or ignored as per spec.
  - Proper error codes and messages for invalid JSON, missing fields, and malformed requests.

- **HTTP/JSON Compliance**  
  - Correct use of status codes:  
    - `201` on create  
    - `200` on get, update, status change, assign/clear assignee  
    - `409` for invalid status transitions  
    - `404` for unknown ticket  
    - `400` for validation errors  
    - `500` for general/unexpected errors
  - JSON response shape and naming: no data wrappers, camelCase, all fields as per contract.
  - Error objects contain timestamp, status, error, code, message, path, and fieldErrors (when relevant).

- **Error Handling**  
  - `400 VALIDATION_ERROR`: show correct validation errors and all relevant field errors.
  - `404 TICKET_NOT_FOUND`: distinct not-found responses.
  - `409 INVALID_STATUS_TRANSITION`: conflict for disallowed transitions (including same-status and terminal states).
  - `500 INTERNAL_ERROR`: generic error without stack trace or implementation details.
  - No updates are accepted unless API returns a full success response.

---

## 3. Repository Tests

**Objectives:**  
- Ensure persistence and retrieval logic is robust and correct.

**Areas to Cover:**

- **Ticket and Comment Persistence**  
  - Tickets are saved and loaded correctly.
  - Comments are saved, retrieved, and linked to the correct tickets (ordered oldest first).

- **Search and Filtering**  
  - Search by title and description, case-insensitive substring semantics.
  - Filtering by status with only allowed status values.
  - Combined keyword and status filtering (AND logic).

- **Ticket/Comment Relationship**  
  - Comments belong to the correct ticket.
  - Comment creation does not affect unrelated fields or tickets.

---

## 4. Integration Tests

**Objectives:**  
- Validate end-to-end flows across all layers (DB, service, API).

**Flows to Cover:**

- Create a ticket (happy path)
- Retrieve ticket details
- Update ticket fields
- Assign and clear assignee
- Add comments to a ticket
- List/search/filter tickets and verify results
- Valid status transitions (each permissible state change, state-machine enforced)
- Attempted invalid transitions (ensure 409/conflict, no state change)
- Persistence after DB interaction or simulated restart (when practical)
- Error scenarios: missing/invalid data, unknown ticket, rejected changes

---

## 5. Important Negative Cases

Comprehensive negative tests should include:

- Blank required fields on ticket or comment creation
- Missing `priority` or invalid `priority`/`status` value
- Invalid or non-numeric ticket IDs
- Unknown/nonexistent ticket ID referenced
- Blank or overly long comment body
- Same-status transition attempts
- Any disallowed status transition (reverse, skip, re-open, etc.)
- Assignee field blank, missing, or too long
- Attempts to set `status` on ticket creation or field update (should not be allowed)
- Malformed JSON in requests

---

## 6. State Machine Test Matrix

**Allowed Status Transitions (all others are invalid):**

| Current      | Allowed Next         |
| ------------ | ------------------- |
| OPEN         | IN_PROGRESS, CANCELLED |
| IN_PROGRESS  | RESOLVED, CANCELLED |
| RESOLVED     | CLOSED              |
| CLOSED       | _(terminal)_        |
| CANCELLED    | _(terminal)_        |

- Test all five allowed transitions:  
  - `OPEN` → `IN_PROGRESS`  
  - `OPEN` → `CANCELLED`  
  - `IN_PROGRESS` → `RESOLVED`  
  - `IN_PROGRESS` → `CANCELLED`  
  - `RESOLVED` → `CLOSED`
- All other transitions must be explicitly tested and confirmed to return `409 INVALID_STATUS_TRANSITION`; no change is made to the ticket.

---

## 7. Project-Specific Testing Rules

- All tests must conform to the project's testing rules as documented in `rules/testing.md`.
- Use recommended Spring Boot test patterns:
  - Use @SpringBootTest for integration tests.
  - Use @WebMvcTest or similar for controller tests.
  - Use appropriate mocking for service- and repository-level unit tests.
- Test method and class naming should clearly communicate the scenario and expected result.

---

**Note:**  
This strategy does not require or permit tests for authentication, permissions, notifications, dashboards, deletes, or pagination—these are strictly out of scope for this assignment.

---
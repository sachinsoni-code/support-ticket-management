# AI review findings

Review of the first draft of `rules/api-standards.md`, `rules/java-springboot.md`, and `rules/testing.md` against the assignment. Extra suggestions were removed so the guidelines stay in scope.

## 1. API guidelines: DELETE, auth, and ASSIGNEE_REQUIRED

**Suggested:** `DELETE /api/v1/tickets/{ticketId}`, HTTP `401`/`403` with `UNAUTHORIZED`/`FORBIDDEN`, and error code `ASSIGNEE_REQUIRED` (cannot move to `IN_PROGRESS` without an assignee).

**Why it was unnecessary:** The assignment does not define a delete-ticket API, authentication/authorization, or an assignee-required rule for status changes.

**What we changed:** Removed DELETE/`204`, all authentication/authorization rules and error codes, and `ASSIGNEE_REQUIRED`. Kept REST paths, JSON format, validation errors, and the remaining status codes.

## 2. Spring Boot guidelines: security and roles

**Suggested:** Spring Security, `USER`/`AGENT`/`ADMIN` roles, password encoding, and taking the current user from the security context.

**Why it was unnecessary:** Security, roles, and password handling are outside the current assignment scope.

**What we changed:** Removed the security section and `401`/`403` exception mapping. Kept Java 21, layering, DTOs, validation, exception handling, and JPA. Corrected the package example to package-by-layer (`controller`, `service`, `repository`, `entity`, `dto`, `exception`).

## 3. Testing guidelines: extra status transitions

**Suggested:** Allowed reverse transitions `IN_PROGRESS` → `OPEN` (return to queue) and `RESOLVED` → `IN_PROGRESS` (reopen).

**Why it was wrong:** Those transitions are not in the assignment state machine. The assignment only allows `OPEN` → `IN_PROGRESS` → `RESOLVED` → `CLOSED`.

**What we changed:** Removed those two transitions from the allowed list and listed them as invalid cases to test. Also dropped auth/delete/assignee-required test cases so they match the cleaned API and Spring Boot guidelines.

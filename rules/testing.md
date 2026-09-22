# Testing Guidelines

Write tests that protect behaviour, especially ticket lifecycle rules. Tests should be easy to run locally and in CI.

## What to test

| Type | What it covers | Typical tools |
| --- | --- | --- |
| Unit | Service logic, status transitions, mapping, validators | JUnit 5, Mockito |
| Controller | HTTP mapping, status codes, validation | `@WebMvcTest` |
| Repository | Queries, constraints, persistence mapping | `@DataJpaTest` |
| Integration | A use case through HTTP + service + database | `@SpringBootTest` + MockMvc or Testcontainers |

- Prefer many fast unit tests for business rules.
- Add a smaller set of controller, repository, and integration tests for wiring and persistence.
- Do not test framework behaviour (e.g. that `@NotNull` works) except where we need to confirm our DTO annotations are present.

## Unit tests (services)

- Mock repositories and other services. Do not start Spring unless needed.
- Name tests: `methodName_condition_expectedResult`  
  Example: `changeStatus_fromOpenToInProgress_updatesStatus`.
- Cover:
  - create ticket (default status `OPEN`)
  - assign ticket
  - valid status transitions
  - invalid status transitions
  - update title/description where allowed
  - not-found cases
- One logical assertion theme per test. Avoid giant “does everything” tests.

## Controller tests

- Use `@WebMvcTest` and mock the service.
- Assert HTTP status, JSON shape, and that the service is called with the right arguments.
- Cover:
  - valid create/get/update
  - bean-validation failures (`400`)
  - not found (`404`)
  - invalid transition (`409`)
- Do not hit a real database in controller tests.

## Repository tests

- Use `@DataJpaTest` (and Testcontainers/PostgreSQL when we lock the database).
- Cover custom queries: filter by status, assignee, paging/sorting if we add them.
- Cover uniqueness or not-null constraints that we rely on.
- Do not put business-rule tests here.

## Integration tests

- Use `@SpringBootTest` for critical flows only:
  - create ticket → fetch by id
  - assign → move `OPEN` → `IN_PROGRESS` → `RESOLVED` → `CLOSED`
  - reject an illegal transition end-to-end
- Prefer Testcontainers PostgreSQL over in-memory DB for integration tests once the schema exists.
- Clean data between tests (`@Transactional` rollback or explicit cleanup). Tests must not depend on run order.

## Negative scenarios (required)

Every feature needs happy-path **and** failure tests:

- Missing resource: get/update unknown ticket id → `404`
- Invalid payload: missing title, empty description, unknown enum value → `400`
- Invalid status transition: see table below → `409`
- Duplicate or conflicting update where the spec defines one (e.g. assigning a closed ticket)

Do not only test the success path.

## Ticket status transitions

Until the spec says otherwise, tickets use this lifecycle:

```text
OPEN → IN_PROGRESS → RESOLVED → CLOSED
```

Default status on create: **OPEN**. **CLOSED** is terminal.

### Allowed

| From | To | Notes |
| --- | --- | --- |
| `OPEN` | `IN_PROGRESS` | Ticket is picked up / assigned |
| `IN_PROGRESS` | `RESOLVED` | Work finished |
| `RESOLVED` | `CLOSED` | Confirmed done |

### Not allowed (must be tested)

| From | To | Why |
| --- | --- | --- |
| `OPEN` | `RESOLVED` | Cannot skip `IN_PROGRESS` |
| `OPEN` | `CLOSED` | Cannot close without work |
| `IN_PROGRESS` | `OPEN` | Reverse transition is not in the assignment |
| `IN_PROGRESS` | `CLOSED` | Must resolve first |
| `RESOLVED` | `OPEN` | Cannot reverse or skip |
| `RESOLVED` | `IN_PROGRESS` | Reverse transition is not in the assignment |
| `CLOSED` | `OPEN`, `IN_PROGRESS`, `RESOLVED` | Terminal state |
| Any | Same status | No-op updates are rejected (or ignored only if the spec later allows it; default: reject) |
| Any | Unknown / null status | Validation error |

Also test changing status on a ticket that does not exist → not found.

When the written spec disagrees with this table, **the spec wins**. Update this file in the same change as the code.

## Test data

- Use small, explicit fixtures. Do not share mutable static data across tests.
- Prefer builders or helper methods (`ticketOpen()`, `ticketInProgress()`).
- Do not depend on wall-clock time; inject a clock if we need timestamps.

## Quality bar

- A pull request that changes status rules, validation, or API errors must include tests for those changes.
- Tests must be deterministic. No `Thread.sleep`.
- Do not disable tests to make CI green.

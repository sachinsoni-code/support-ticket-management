# Java / Spring Boot Guidelines

Use these rules when implementing or reviewing backend code. Prefer simple, readable code over clever abstractions.

## Language and runtime

- Use **Java 21** and Spring Boot 3.x.
- Prefer records for DTOs and other immutable data.
- Prefer `Optional` only at service/repository boundaries. Do not use `Optional` as a field, constructor parameter, or collection element.
- Avoid `null` in public APIs. Return empty collections instead of `null`.
- Do not use field injection (`@Autowired` on fields). Use constructor injection.

## Project structure

Keep the backend **package by layer**:

```text
controller/
  TicketController.java
service/
  TicketService.java
repository/
  TicketRepository.java
entity/
  Ticket.java
  TicketStatus.java
dto/
  CreateTicketRequest.java
  UpdateTicketRequest.java
  TicketResponse.java
exception/
  TicketNotFoundException.java
  InvalidStatusTransitionException.java
```

- Controllers talk only to services.
- Services contain business rules (including ticket status transitions).
- Repositories talk only to the database. No business logic in repositories.
- Entities stay in the persistence layer. Do not expose JPA entities as API responses.

## Controllers

- Map HTTP only: validate input, call a service, return a response.
- No business rules, no repository calls, no transaction logic.
- Use `@Valid` on request bodies.
- Keep URLs under `/api/v1/`. See `rules/api-standards.md`.

## Services

- One public method should do one use case (create ticket, assign ticket, change status).
- Enforce ticket status transition rules in the service layer, not in the controller.
- Throw domain-specific exceptions (`TicketNotFoundException`, `InvalidStatusTransitionException`). Do not return `null` for missing tickets.
- Keep methods transactional at the service boundary (`@Transactional` on the service class or on write methods). Read-only queries may use `@Transactional(readOnly = true)`.

## Repositories

- Use Spring Data JPA interfaces.
- Name query methods clearly (`findByStatus`, `findByAssigneeId`).
- Put custom queries in the repository only when derived methods are not enough.
- Do not catch data-access exceptions in the repository.

## Constructor injection

```java
@RestController
@RequestMapping("/api/v1/tickets")
public class TicketController {
    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }
}
```

- Use `final` fields; let Spring inject the constructor.
- Use `@RequiredArgsConstructor` only if Lombok is adopted in the spec. Until then, write constructors explicitly.

## DTOs

- Request DTOs: `CreateTicketRequest`, `UpdateTicketStatusRequest`, and similar.
- Response DTOs: `TicketResponse`. Never return an entity.
- Map entity → DTO in the service (or a small mapper). Controllers should not know about entities.
- Do not put JPA annotations on DTOs.

## Validation

- Validate request DTOs with Jakarta Bean Validation (`@NotBlank`, `@NotNull`, `@Size`).
- Validate business rules in the service (status transitions, ticket is not already closed).
- Fail fast: invalid input must not reach the database write.

## Exception handling

- Use a single `@RestControllerAdvice` for API errors.
- Map:
  - not found → `404`
  - validation / bad request → `400`
  - invalid status transition → `409` (see `rules/api-standards.md`)
  - unexpected errors → `500` with no internal details
- Log unexpected errors. Do not leak stack traces to clients.

## Database access

- Use PostgreSQL in real environments; H2 is acceptable only for local/dev if the spec allows it.
- Use Flyway (preferred) or Liquibase for schema changes. No ad-hoc `ddl-auto=update` in anything meant for review or production.
- Every table needs a primary key. Prefer UUID or a long id; pick one and use it everywhere.
- Add indexes for columns we filter on (status, assignee, created date) when we write the schema spec.
- Do not use native SQL unless JPA cannot express the query.
- Do not commit database credentials. Use environment variables or Spring profiles.

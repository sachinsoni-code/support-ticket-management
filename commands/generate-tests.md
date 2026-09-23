/*
Review the Support Ticket Management System codebase and existing test suite in reference to the following specification and rules documents:

- spec/requirements.md
- spec/architecture.md
- spec/api-contract.md
- spec/state-machine.md
- spec/test-strategy.md
- rules/java-springboot.md
- rules/testing.md

Your task is to generate only production-ready tests for behaviors that are missing or insufficiently tested, strictly following the project's testing conventions and boundaries.

Instructions:

1. Thoroughly inspect the current implementation and all test files to determine which behaviors—defined in the specifications—are fully, partially, or not at all covered by tests.
2. For any behavior that is missing or insufficiently tested, generate new tests using established testing patterns and Spring Boot conventions present in the codebase.
3. The following behaviors must be confirmed as covered; if any are missing or insufficient, generate additional tests for:
   - Ticket creation (valid and invalid inputs, required and optional fields, all validation rules).
   - Ticket update (all fields present, partial updates must be rejected per spec, with proper validation coverage).
   - Assignee logic (assign, clear, reject blank/invalid/too long/null values).
   - Comment creation (valid, blank, overly long, proper ticket association).
   - Search and status filtering.
   - Exact five allowed status transitions (no others are valid):
     - OPEN → IN_PROGRESS
     - OPEN → CANCELLED
     - IN_PROGRESS → RESOLVED
     - IN_PROGRESS → CANCELLED
     - RESOLVED → CLOSED
   - All other, reverse, skip, same-status, and terminal-state transitions must be tested to ensure:
     - The correct error code (409 INVALID_STATUS_TRANSITION) and message are produced.
     - No change is made to ticket state or any other ticket field.
   - All required API-level request validations, proper HTTP status codes, error response structure, and field-level errors as outlined in the API contract and spec.
   - Ticket, assignee, and comment persistence and relationships as specified.
4. Do NOT generate or suggest tests for any out-of-scope features (including but not limited to: authentication, deletion, notifications, dashboards, or pagination).
5. Do NOT modify production code or any spec/rules files.
6. Name test classes and methods clearly, indicating scenario and expected results, and avoid duplicating existing coverage.
7. Use only the existing Spring Boot test infrastructure, annotations, and configurations.
8. After generating required tests, provide a concise summary including:
   - Exactly which new tests were added.
   - Which scenarios or specification requirements they cover.
   - Whether any spec ambiguities or contradictions were encountered that prevent test coverage or require developer clarification.

Output only production-ready test code for missing/insufficient behaviors and the required summary, as described above.
*/
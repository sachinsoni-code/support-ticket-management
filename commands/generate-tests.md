/*
Analyze the Support Ticket Management System codebase and its test suite to identify any gaps in test coverage based on the following documents:

- spec/requirements.md
- spec/architecture.md
- spec/api-contract.md
- spec/state-machine.md
- spec/test-strategy.md
- rules/java-springboot.md
- rules/testing.md

You must generate production-ready tests only for behaviors specified in the documentation above that are missing or inadequately covered, adhering to all project testing rules and conventions.

Process:

1. Examine all implementation and test files to determine which specification-driven behaviors are completely, partially, or not at all covered by existing tests.
2. For each behavior lacking sufficient coverage, write new tests consistent with the project's established Spring Boot testing patterns.
3. Confirm that tests exist—and add any missing or incomplete coverage—for at least the following areas:
   - Ticket creation: All required and optional fields, handling valid/invalid inputs, and all validation rules.
   - Ticket updating: Enforce that all fields must be present; partial updates are rejected. Validate all update-related rules.
   - Assignee: Assign, clear, reject blank, null, and overly long assignee values per requirements.
   - Comment creation: Test valid, blank, and overly long comment bodies, and correct comment-to-ticket association.
   - Search and status filtering functionality.
   - State transitions: Only these five must succeed—
     - OPEN → IN_PROGRESS
     - OPEN → CANCELLED
     - IN_PROGRESS → RESOLVED
     - IN_PROGRESS → CANCELLED
     - RESOLVED → CLOSED
   - All other, reverse, skip, same-status, or terminal status transitions: Test that they are rejected with HTTP 409 and an INVALID_STATUS_TRANSITION error with no ticket modifications.
   - Full API layer validation: HTTP status codes, required/extra fields, response and error payload structure, including all required fields as per the API contract.
   - Persistence and relationships for tickets, assignees, and comments.

4. Do NOT generate or suggest tests for out-of-scope features such as authentication, deletion, notifications, dashboards, or pagination.
5. Do NOT modify any production (non-test) code or any documentation/spec/rules files.
6. Name all new test classes and methods descriptively to clearly communicate the scenario and expected outcome; do not duplicate scenarios already covered.
7. Use only the current Spring Boot testing tools, annotations, configurations, and conventions found in the codebase.
8. After writing the necessary tests, provide a clear, concise summary specifying:
   - Which new tests were added.
   - Which requirements or scenarios each new test addresses.
   - Any ambiguities or contradictions encountered in the specification that limit test coverage or require clarification.

Produce only the new, production-quality test code required for missing/under-tested behaviors and the summary, as outlined above.
*/
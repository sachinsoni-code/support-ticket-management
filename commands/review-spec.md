/*
Review the Support Ticket Management specifications as follows:

Review the following files together (do not modify them):

- spec/requirements.md
- spec/architecture.md
- spec/data-model.md
- spec/api-contract.md
- spec/state-machine.md
- spec/ui-flow.md
- spec/test-strategy.md

Check:
1. All requirements are fully covered across docs.
2. Specifications are consistent with each other.
3. Ticket statuses and allowed state transitions are exactly the same everywhere.
4. API endpoints, HTTP status codes, validation rules, error codes, and response formats match the API contract.
5. Data model supports all required features, including status/state logic and comments.
6. UI flow does not contradict backend/API behavior and error handling.
7. Test strategy fully covers requirements and explicitly tests negative/error/invalid cases.
8. All assumptions and non-requirements (e.g. features intentionally not included) are clear and consistent.
9. No features out of scope (auth, deletes, notifications, dashboards, pagination, etc.) have been added or implied.
10. Identify any ambiguity, contradiction, missing requirement, or implementation risk.

For each issue found, return:

Issue:
Why it matters:
Affected files:
Recommended fix:

If no issues are found, return exactly:

No issues found.
*/
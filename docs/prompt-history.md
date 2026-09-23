# AI Prompt Usage Notes

This doc tracks how I used AI (Cursor) during this assignment and what I reviewed/changed along the way.

---

## Initial Setup

Wanted to get the repo and folder structure ready for specs and config.

- Used AI to help lay out folders and naming conventions.
- Reviewed and adjusted folder and file structure to fit assignment needs.
- Final: Baseline structure in place, minor AI suggestions rewritten for clarity.

---

## Java/Spring Boot, Testing, and API Guidelines

Needed starting-point guidelines for backend code, testing, and REST APIs.

- Asked AI for Java/Spring Boot best practices and how to organize code/tests.
- Reviewed the AI outputs and removed unnecessary or out-of-scope content (e.g. auth, user roles, deletes, extras).
- Final: Guidelines focused just on assignment scope.

---

## Reviewing AI-Generated Guidelines

Noticed that early AI drafts included features outside scope.

- Looked for and removed any mention of authentication, deletion, notification, pagination, etc.
- Asked AI to rewrite or clarify several sections.
- Final: Only required features and constraints are documented.

---

## Writing the Specification Files

Created requirements, architecture, data model, API contract, state machine, UI flow, and test strategy.

- Used AI to draft each spec based on assignment instructions.
- For each, checked content against the instructions. Cut anything not specified.
- Double-checked wording and technical details to keep things accurate.
- Final: Set of spec files matching the assignment.

---

## Reviewing and Fixing Specs with AI

Iteratively reviewed each spec file with AI review commands.

- Ran review prompts on each doc.
- Fixed inconsistencies (e.g. state transitions, error codes, allowed fields).
- Made sure status flows and field names matched everywhere.
- Final: Specs are now self-consistent and only include allowed features.

---

## Creating review-spec, review-code, and generate-tests Commands

Wanted reusable AI commands for spec/code/test reviews.

- Used AI to help draft review commands with clear, repeatable checklists.
- Reviewed the instructions and tweaked to cut anything that's not part of the assignment or seemed unnecessary.
- Final: Commands ready to use on future spec/code/test changes.

---

## Creating the Documentation Skill

Needed a skill to make sure AI-generated docs match the assignment and don't invent features.

- With AI's help, described exact doc requirements and do/don't rules.
- Checked AI's first draft, added more restrictions (like not adding out-of-scope topics).
- Final: Documentation Skill guides all doc changes.

---

## Issues Found and Corrections Made

Main issues spotted in AI output:

- AI sometimes added out-of-scope stuff (auth, deletions, permissions) or made up field names and flows.
- Noticed inconsistent state transitions and error codes in early drafts.
- In each case, reviewed output, removed anything not backed by the assignment, and reworded instructions/specs for clarity.

---

All work so far has covered project structure, rules/guidelines, specifications, reusable AI commands, and the documentation skill. Actual backend, frontend, database implementation, and automated test code have not been started yet.
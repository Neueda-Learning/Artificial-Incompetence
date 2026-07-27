---
description: "Use when implementing or modifying the Portfolio Manager backend REST API, data model, validation, tests, or API documentation. Enforces training-project requirements from portfolio_manager.md."
name: "Portfolio Manager Backend Requirements"
applyTo: "backend/src/main/java/**/*.java, backend/src/test/java/**/*.java, README.md"
---

# Portfolio Manager Backend Requirements

- Build and maintain a REST API for portfolio items as the primary deliverable.
- Assume a single user and do not add authentication or user management unless explicitly requested.
- Use persistent storage with the project database stack already in use.
- Start with the minimum viable data model and evolve incrementally; avoid over-engineering early entity design.
- Preserve core portfolio operations in order of priority:
  - Browse/list portfolio items
  - Add portfolio items
  - Remove portfolio items
- Use clear HTTP semantics:
  - `GET` for retrieval
  - `POST` for creation
  - `DELETE` for removal
  - Proper status codes (`200`, `201`, `204`, `4xx`, `5xx` as appropriate)
- Validate request payloads and normalize domain inputs where useful (for example, ticker symbol format and quantity constraints).
- Keep error responses consistent through centralized exception handling.
- Add or update automated tests for controller and service changes; tests should pass with the existing local test setup.
- Keep API usage documentation current in project docs (endpoint paths, sample requests, expected responses).
- If adding AI or quantum features, treat them as experimental stretch goals:
  - Keep them optional and separate from core CRUD behavior.
  - Document assumptions, limitations, and that outputs are advisory, not guaranteed.

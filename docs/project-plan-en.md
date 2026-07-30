# Portfolio Manager Project Plan

## 1. Project Objectives

A four-person team delivers a portfolio management application in phases:

1. Implement querying, adding, and deleting of holdings with MySQL persistence.
2. Add purchase history, current market prices, and performance analysis.
3. Complete frontend-backend integration, automated testing, Docker development environment, and project demonstration.

The project scope is based on US-01 through US-07 in [`user-stories.md`](./user-stories.md).

## 2. Team Roles

| Role | Primary Responsibility | Secondary Responsibility |
|---|---|---|
| Backend A | US-01, US-02, US-03 | Portfolio core model, CRUD APIs, unified error format |
| Backend B | US-04, US-05 | MySQL persistence, Docker volumes, purchase history |
| Backend C | US-06, US-07 | Market price service, exchange rate conversion, performance calculation |
| Frontend | Pages and interactions for US-01 through US-07 | API integration, charts, error and loading states |

All members share responsibility for:

- Requirements confirmation and API contract review.
- Pull Request code review.
- Automated testing for their own features.
- Cross-acceptance testing, regression testing, and defect management.
- Daily stand-ups and risk synchronization.
- README and final demo.

## 3. Design Decisions to Finalize Before Development

Before branching off to develop independently, the four members must confirm the following to avoid integration issues later:

### 3.1 Data Responsibilities

```text
portfolio_items
    Represents the current holdings a user owns

transactions
    Represents historical BUY or SELL transactions
```

Purchase history must be stored independently. Deleting a current holding must not cascade-delete historical transactions.

### 3.2 Core Data Model

```text
PortfolioItem
- id
- assetType
- symbol
- quantity

Transaction
- id
- transactionType
- assetType
- symbol
- quantity
- pricePerUnit
- currency
- purchasedAt
```

### 3.3 Initial Business Conventions

- The first version supports only a single base currency, e.g., USD.
- Stock symbols are trimmed and converted to uppercase before saving.
- All monetary amounts and rates of return use `BigDecimal`.
- All times are transmitted in ISO 8601 format and stored in UTC.
- When external market prices are unavailable, return `PARTIAL`; do not treat the price as 0.
- US-02 "Add Asset" and US-05 "Record Purchase" must be unified in Phase 2 to avoid inconsistency between current holdings and transaction history.

### 3.4 API Contract

```http
GET    /api/portfolio/items
POST   /api/portfolio/items
DELETE /api/portfolio/items/{id}

POST   /api/transactions
GET    /api/transactions?type=BUY

GET    /api/portfolio/value
GET    /api/portfolio/performance
```

Before development, the frontend and backend teams should jointly confirm the request, response, status codes, and error format for each endpoint.

## 4. Implementation Phases

The total development time is 3 days. All four members must work in parallel and merge a runnable version at the end of each day. Frontend-backend integration must not be deferred until the last day.

### Three-Day Scope Priorities

| Priority | Must Deliver |
|---|---|
| P0 | US-01 through US-05, MySQL persistence, Docker startup, basic frontend |
| P1 | US-06 current price and current value |
| P2 | US-07 basic performance: cost, current value, P&L, and rate of return |

Out of scope for the three days (or to be done only after all core features are complete):

- Historical return line charts.
- Multi-currency portfolio aggregation.
- SELL transactions and complex cost methods.
- User authentication.
- AI or quantum features.
- Complex animations and page beautification.

US-07 remains in scope, but deliver the numeric summary first; the asset allocation pie chart can be completed if time permits.

## Phase 0: Requirements and Interface Freeze

**Time: 1 to 1.5 hours after the start of Day 1**

### Backend A

- Confirm `PortfolioItem` fields and interfaces.
- Define the unified error response.

### Backend B

- Confirm `Transaction` fields.
- Confirm MySQL, JPA, Docker, and volume configuration.
- Define synchronization rules between holdings and transaction history.

### Backend C

- Define the market price service interface and failure strategy.
- Define the exchange rate conversion strategy.
- Define the performance response and calculation precision.

### Frontend

- Create wireframes for holdings, purchase history, and performance pages.
- Prepare mock JSON based on the API contract.
- Confirm the data format required by charts.

### Team-Wide Testing Arrangements

- Convert acceptance criteria for US-01 through US-07 into test cases.
- Establish test data including normal, empty, error, and boundary values.
- Confirm the test commands that CI should run.
- Agree on the cross-acceptance approach: Backend B reviews Backend A, Backend C reviews Backend B, Backend A reviews Backend C.

### Phase Deliverables

- API contract.
- Data model or ER diagram.
- GitHub Issues task list.
- Page wireframes.
- Test plan.

### Completion Gate

- All four members jointly confirm API fields and error formats.
- The frontend can begin development using mock JSON.
- The three backend members confirm dependencies among holdings, transaction history, market prices, and performance.

## Phase 1: Core MVP Parallel Development

**Time: Remainder of Day 1**

### Backend A: US-01 through US-03

- Implement `GET /api/portfolio/items`.
- Implement `POST /api/portfolio/items`.
- Implement `DELETE /api/portfolio/items/{id}`.
- Complete request validation and 404/400 error handling.
- Add tests for Controller, Service, and Repository.

### Backend B: US-04 and US-05

- Configure MySQL persistence and Docker volumes.
- Verify data persists after container restart.
- Create `Transaction` entity and database table.
- Implement `POST /api/transactions`.
- Implement `GET /api/transactions?type=BUY`.
- Return records in descending order of `purchasedAt`.
- Ensure transaction history remains after a holding is deleted.
- Add tests for transaction queries, validation, and sorting.

### Backend C: US-06 and US-07 Basics

- Define the market price client interface and a fixed-price stub.
- Define current value and performance response DTOs.
- Implement the performance calculator skeleton.
- Prepare test data for weighted average cost, P&L, rate of return, and asset allocation.
- Use stubs to continue development while Backend B's APIs are not yet ready.

### Frontend

- Create the project skeleton, routing, and API client.
- Implement the holdings list using mock data.
- Implement the add-asset form.
- Implement delete confirmation and error messages.
- Implement loading, empty data, and request failure states.

### Cross-Testing

- Backend B reviews US-01 through US-03, including empty lists, invalid quantities, and non-existent IDs.
- Backend C reviews US-04 through US-05, including container restart, history sorting, and data retention.
- Backend A reviews the calculation test design for US-06 through US-07.
- Frontend validates list, form, error, and empty states under mock data.
- Team-wide check of Maven tests and Docker build CI.

### Phase Deliverables

- Runnable holdings CRUD API.
- Persistable MySQL development environment.
- Savable and queryable purchase history API.
- Core frontend working with mock data.
- Automated tests for US-01 through US-05.

### Completion Gate

- Core scenarios for US-01 through US-05 pass acceptance.
- Data is not lost after MySQL restart.
- Pull Requests are reviewed by another member.
- GitHub CI passes fully.
- Before the end of Day 1, all four members must pull the same integration branch and successfully start the project.

## Phase 2: Market Prices and Performance Development

**Time: Day 2**

### Backend A

- Support Backend B and Backend C in integrating holdings data.
- Handle the business rule for duplicate stock additions.
- Ensure holding deletion does not cascade-delete transaction history.
- Fix Phase 1 defects.

### Backend B: Complete US-04 and US-05

- Complete purchase history edge cases.
- Complete transaction boundaries between holdings and transaction history.
- Provide cost and purchase history query capabilities to Backend C.
- Fix MySQL, Docker, and transaction history defects.

### Backend C: Complete US-06 and US-07

- Create market price client and service.
- Implement current price, update time, and market-price-unavailable state.
- Implement `GET /api/portfolio/value`.
- Implement `GET /api/portfolio/performance`.
- Calculate weighted average cost, current value, unrealized P&L, rate of return, and asset allocation.
- Handle empty holdings, zero cost, and partial market price availability.
- Add unit tests for all calculations.

### Frontend

- Connect to real holdings API.
- Create the purchase history page.
- Create the current value display.
- Create performance summary cards.
- Display asset allocation using a pie chart.
- Show explicit indicators for missing market prices.

### Cross-Testing

- Backend B regresses US-01 through US-03.
- Backend C regresses US-04 through US-05.
- Backend A validates US-06 through US-07 using independent fixed-price data.
- Frontend checks that all responses conform to the API contract.
- Team-wide simulation of external market price failures, partial price availability, and empty holdings.

### Phase Deliverables

- Purchase history page and API.
- Current value API and page.
- Performance API and page.
- Performance calculation unit tests.

### Completion Gate

- US-05 through US-07 pass acceptance.
- Backend A validates performance calculation results with independent test data.
- External market price failures do not cause holdings queries to fail.
- Frontend and backend no longer depend on mock data.
- Before the end of Day 2, a demo candidate version covering US-01 through US-07 is formed.

## Phase 3: System Integration and Regression Testing

**Time: Morning of Day 3**

### Backend A

- Fix CRUD and database integration issues.
- Verify the unified error response.
- Refine API documentation.

### Backend B

- Fix MySQL, transaction history, and data consistency issues.
- Verify database restart and history retention behavior.

### Backend C

- Fix market price and performance issues.
- Add handling for timeouts, exceptions, and partial results.
- Confirm calculation precision and rounding rules.

### Frontend

- Complete integration with all real APIs.
- Unify page styling.
- Refine responsive layout.
- Complete empty states, error states, and refresh behavior.

### Team-Wide Regression

- Backend B performs API regression on US-01 through US-03.
- Backend C performs persistence and history regression on US-04 through US-05.
- Backend A performs market price and performance regression on US-06 through US-07.
- Frontend executes core browser workflows and end-to-end tests.
- All four members jointly perform Docker full-stack testing and maintain a defect list.

### Phase Deliverables

- Complete system launchable via Docker.
- Complete test report.
- Triaged defect list.
- Updated README and API examples.

### Completion Gate

- All high-priority defects are closed.
- All core workflows execute successfully at least once.
- CI remains passing.
- A new member can complete startup by reading only the README.

## Phase 4: Stable Release and Demo

**Time: Afternoon of Day 3**

### All Members

- Freeze the demo version.
- Prepare fixed demo data.
- Conduct one complete dry run.
- Prepare the architecture diagram, data model diagram, and project challenges summary.
- Confirm each member's demo content.

### Demo Responsibilities

| Member | Suggested Content |
|---|---|
| Backend A | Project objectives, holdings query, add, and delete |
| Backend B | MySQL persistence, Docker volumes, and purchase history |
| Backend C | Market prices, exchange rate conversion, and performance calculation |
| Frontend | Page design, user operations, chart display |

### Completion Gate

- The demo flow is repeatable.
- The Docker environment can be started from scratch.
- README, User Stories, and test results are complete.
- All four members can explain their own modules.

## 5. User Story Responsibility Matrix

| User Story | Backend Owner | Frontend Owner | Cross-Acceptance Reviewer | Primary Dependency |
|---|---|---|---|---|
| US-01 View Portfolio | Backend A | Frontend | Backend B | Portfolio Repository |
| US-02 Add Asset | Backend A | Frontend | Backend B | Validation, MySQL |
| US-03 Delete Asset | Backend A | Frontend | Backend B | 404 handling, history retention rules |
| US-04 Persistence | Backend B | No standalone page | Backend C | Docker Volume, MySQL |
| US-05 Purchase History | Backend B | Frontend | Backend C | Transaction table |
| US-06 Current Value | Backend C | Frontend | Backend A | Current holdings, market price service |
| US-07 Performance | Backend C | Frontend | Backend A | US-05, US-06 |

## 6. Key Dependency Relationships

```text
US-01/02/03/04: Holdings MVP
          │
          ├──────────────┐
          ▼              ▼
US-05: Purchase History   US-06: Current Market Price and Value
          │              │
          └──────┬───────┘
                 ▼
        US-07: Performance Analysis
                 │
                 ▼
          Frontend Charts and Final Display
```

US-07 cannot be completed before US-05 and US-06 because:

- Without purchase history, there is no reliable cost basis.
- Without current market prices, the current value cannot be calculated.
- Without cost and current value, P&L and rate of return cannot be calculated.

## 7. Git and Pull Request Plan

Suggested branches:

```text
feature/us01-list-portfolio
feature/us02-add-asset
feature/us03-delete-asset
feature/us04-persistence
feature/us05-purchase-history
feature/us06-current-value
feature/us07-performance
feature/frontend-portfolio
test/cross-acceptance
```

Pull Request requirements:

- Each PR should correspond to one User Story as much as possible.
- Link the corresponding GitHub Issue in the PR description.
- At least one other member must review.
- The three backend members use round-robin review: Backend B reviews A, Backend C reviews B, Backend A reviews C.
- PRs involving shared entities or DTOs require at least two backend members to confirm.
- CI must pass before merging.
- Do not push feature code directly to `main`.

## 8. Testing Strategy

| Test Type | Focus | Primary Owner |
|---|---|---|
| Unit Tests | Service calculations, validation, exceptions | Feature backend owner |
| Repository Tests | MySQL/JPA queries and sorting | Backend A, Backend B |
| Market Price Client Tests | Success, timeout, failure, and partial data | Backend C |
| API Tests | Status codes, JSON, error responses | Corresponding cross-acceptance reviewer |
| Frontend Component Tests | Forms, lists, chart states | Frontend |
| End-to-End Tests | Add, query, history, performance | Frontend leads, three backend members support |
| Full Regression | US-01 through US-07 | All four members jointly |

The project has no dedicated QA person, so every feature must follow "developer writes automated tests, another backend member performs cross-acceptance." Testing must not be concentrated at the end; it should be completed before each PR is merged.

## 9. Key Risks and Mitigations

| Risk | Impact | Mitigation |
|---|---|---|
| Inconsistency between holdings and transaction history | Incorrect performance calculation | Finalize the single update flow in Phase 0 and use transactions |
| Yahoo market prices unavailable or restricted | US-06, US-07 blocked | Abstract the market price interface, provide a fixed-price stub |
| Three backend members modifying shared models simultaneously | Merge conflicts | Freeze DTO and entity fields first; frequent small PR merges |
| Frontend waiting for backend | Progress blocked | Provide mock JSON and API contract in Phase 0 |
| Using floating-point for monetary amounts | Calculation errors | Use `BigDecimal` consistently with unified rounding rules |
| Deleting holdings cascades to history | History loss | Do not configure cascade delete on Transaction |
| No dedicated QA person | Testing squeezed by development tasks | Include test tasks in every Issue and use round-robin cross-acceptance |

## 10. Project Completion Definition

Project completion requires all of the following:

- US-01 through US-07 acceptance criteria passed.
- The frontend can complete all core user operations.
- MySQL data can be persisted.
- Performance calculations are verified by independent test data.
- The system degrades gracefully when market prices are unavailable.
- The Docker full stack can be started.
- GitHub CI passes.
- No unaddressed high-priority defects remain.
- README, API documentation, and demo steps are complete.

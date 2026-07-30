# Test Documentation

This document summarizes the current automated test suite in the repository based on the existing test code.

## 1. Scope and Test Stack

- **Backend:** Java 21, Spring Boot Test, JUnit, MockMvc, H2 test database (`backend/src/test/java`)
- **Frontend:** React Testing Library + Jest (`frontend/src/**/*.test.ts*`)
- **Total test cases:** **255**
  - Backend: **150**
  - Frontend: **105**

## 2. How to Run Tests

### Backend

```bash
cd backend
mvn test
```

### Frontend

```bash
cd frontend
npm run test:ci
npm run typecheck
```

## 3. Backend Test Coverage Summary

### 3.1 Application and Common Error Handling

| Test Class | Cases | Main Coverage |
| --- | ---: | --- |
| `PortfolioManagerApplicationTests` | 1 | Spring application context bootstrap |
| `GlobalExceptionHandlerTest` | 8 | HTTP error mapping (`400/404/502/503`), field-error formatting and deduplication |

### 3.2 Market Data and FX Services

| Test Class | Cases | Main Coverage |
| --- | ---: | --- |
| `ExchangeRateServiceTest` | 11 | USD shortcut, missing/blank API keys, API error handling, missing rates, caching behavior |
| `HistoricalExchangeRateServiceTest` | 5 | Historical rate lookup, persistence on cache miss, currency normalization |
| `HistoricalMarketDataServiceImplTest` | 5 | Delegation to historical price/FX services, handling unavailable inputs |
| `HistoricalPriceServiceTest` | 5 | Cached historical prices, remote fetch fallback, input normalization |
| `MarketDataServiceImplTest` | 3 | Price + FX delegation, source availability tracking |
| `OpenExchangeRatesHistoricalClientTest` | 12 | API key validation, malformed/empty responses, rate inversion, timestamp parsing, exception handling |
| `TwelveDataHistoricalClientTest` | 16 | Historical price parsing, fallback exchange/currency, filtering invalid rows, API error handling |
| `TwelveDataPriceServiceTest` | 1 | Previous close parsing |

### 3.3 Portfolio Holdings and Metadata

| Test Class | Cases | Main Coverage |
| --- | ---: | --- |
| `PortfolioControllerIntegrationTest` | 5 | Create holding, symbol normalization, repeated symbol quantity merge, input validation |
| `PortfolioServiceTest` | 12 | CRUD-like holding flows, partial/full removal, duplicate consolidation, not-found behavior, activity side-effects |
| `TwelveDataAssetMetadataClientTest` | 2 | Symbol exchange resolution and API-plan limitation handling |

### 3.4 Transactions and Activity Ledger

| Test Class | Cases | Main Coverage |
| --- | ---: | --- |
| `TransactionControllerIntegrationTest` | 15 | Transaction creation/query, default type behavior, validation errors, currency/exchange checks, portfolio-history consistency |
| `TransactionServiceTest` | 11 | Reject unsupported sell flow, metadata-driven stock creation, currency validation, default USD behavior, history retrieval |
| `PortfolioActivityControllerIntegrationTest` | 5 | Activity retrieval, ordering, activity generation from transaction and removal actions |
| `PortfolioActivityServiceTest` | 7 | Empty/non-empty history, ordering, mapping correctness, persisted activity variants |

### 3.5 Analytics and Performance

| Test Class | Cases | Main Coverage |
| --- | ---: | --- |
| `AnalyticsControllerIntegrationTest` | 8 | Portfolio value/performance APIs, empty portfolio behavior, partial/unavailable states, historical range validation |
| `AnalyticsServiceTest` | 14 | Market value composition, missing-price handling, gain/loss math, weighted average cost, allocation %, price timestamp behavior |
| `HistoricalPerformanceServiceTest` | 4 | Daily and per-stock time series, missing transaction data, missing historical price diagnostics |

## 4. Frontend Test Coverage Summary

### 4.1 End-to-End UI Flows at App Level

| Test File | Cases | Main Coverage |
| --- | ---: | --- |
| `App.test.tsx` | 12 | First-visit empty state, add/remove asset flows, repeated symbol aggregation in UI, loading/error states, USD labeling, performance chart switching |

### 4.2 Dashboard, Holdings, and Performance Components

| Test File | Cases | Main Coverage |
| --- | ---: | --- |
| `Dashboard.test.tsx` | 16 | Skeleton/error/empty states, metric card rendering, negative style rendering, top holdings, activity feed, partial performance warnings |
| `Holdings.test.tsx` | 14 | Tab behavior (positions/allocation/history), URL param tab selection, allocation fallback, loading/error banners |
| `Performance.test.tsx` | 10 | Loading/error states, range switching (`1M/3M/...`), historical fetch behavior, cleanup on unmount |

### 4.3 Frontend Service Layer

| Test File | Cases | Main Coverage |
| --- | ---: | --- |
| `portfolioService.test.ts` | 14 | API mapping/normalization, CRUD calls, historical/performance/activity fetches, network failure propagation, field-level API error exposure |

### 4.4 Frontend Utility Functions

| Test File | Cases | Main Coverage |
| --- | ---: | --- |
| `formatters.test.ts` | 29 | Number/USD/signed/percent/date formatting, null/undefined fallbacks, sign handling |
| `portfolio.test.ts` | 9 | Holding aggregation by normalized symbol, quantity summation, sorting behavior |
| `constants.test.ts` | 1 | App name constant contract |

## 5. Coverage Characteristics

- The suite strongly emphasizes **service-layer business rules**, **API validation/error mapping**, and **UI state rendering**.
- Backend tests include both **unit tests** and **integration-style controller tests**.
- Frontend tests focus on **component behavior**, **state-driven rendering**, and **API adapter correctness**.

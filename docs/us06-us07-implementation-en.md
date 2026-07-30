# US-06 & US-07 Implementation Document

> Author: Serena | Branch: `serena` | Date: 2026-07-28

---

## Table of Contents

1. [Overview](#overview)
2. [API Endpoints](#api-endpoints)
3. [Code Structure](#code-structure)
4. [Business Logic](#business-logic)
5. [External API Integration](#external-api-integration)
6. [Exception Handling](#exception-handling)
7. [Configuration](#configuration)
8. [Testing](#testing)
9. [How to Start](#how-to-start)

---

## Overview

Implements two core portfolio management features:

| User Story | Endpoint | Description |
|------------|----------|-------------|
| US-06 | `GET /api/portfolio/value` | View current price and market value for each holding |
| US-07 | `GET /api/portfolio/performance` | Analyze total cost, profit/loss, return rate, and asset allocation |

**Dependency**: US-05 (purchase history) provides cost basis data.

**External Data Sources**:
- [TwelveData](https://twelvedata.com) — Real-time stock prices
- [Open Exchange Rates](https://openexchangerates.org) — Exchange rate conversion (unified to USD)

---

## API Endpoints

### US-06: View Current Value

```http
GET /api/portfolio/value
```

**Success Response** `200 OK`:

```json
{
  "currency": "USD",
  "priceUpdatedAt": "2026-07-28T01:56:00.969Z",
  "status": "COMPLETE",
  "assets": [
    {
      "symbol": "AAPL",
      "assetType": "STOCK",
      "quantity": 10.00,
      "currentPrice": 336.91,
      "marketValue": 3369.10,
      "currency": "USD"
    },
    {
      "symbol": "TSLA",
      "assetType": "STOCK",
      "quantity": 5.00,
      "currentPrice": 309.22,
      "marketValue": 1546.10,
      "currency": "USD"
    }
  ],
  "missingPrices": []
}
```

**Status Descriptions**:

| status | Meaning |
|--------|---------|
| `COMPLETE` | All holdings have real-time prices |
| `PARTIAL` | Some holdings are missing market data |
| `UNAVAILABLE` | All market data is unavailable |

---

### US-07: Portfolio Performance Analysis

```http
GET /api/portfolio/performance
```

**Success Response** `200 OK`:

```json
{
  "currency": "USD",
  "totalCost": 3030.00,
  "currentValue": 4915.20,
  "unrealizedProfitLoss": 1885.20,
  "returnPercentage": 62.22,
  "status": "COMPLETE",
  "priceUpdatedAt": "2026-07-28T01:56:00.969Z",
  "assets": [
    {
      "symbol": "AAPL",
      "quantity": 10.00,
      "averageCost": 180.50,
      "currentPrice": 336.91,
      "costBasis": 1805.00,
      "currentValue": 3369.10,
      "unrealizedProfitLoss": 1564.10,
      "returnPercentage": 86.65,
      "allocationPercentage": 68.54
    },
    {
      "symbol": "TSLA",
      "quantity": 5.00,
      "averageCost": 245.00,
      "currentPrice": 309.22,
      "costBasis": 1225.00,
      "currentValue": 1546.10,
      "unrealizedProfitLoss": 321.10,
      "returnPercentage": 26.21,
      "allocationPercentage": 31.46
    }
  ],
  "missingPrices": []
}
```

**Status Descriptions**:

| status | Meaning |
|--------|---------|
| `COMPLETE` | All holding data is complete |
| `PARTIAL` | Some holdings are missing market data; incomplete data |

---

## Code Structure

```
backend/src/main/java/com/hsbc/portfoliomanager/
├── transaction/                              # Transaction records
│   ├── Transaction.java                      # JPA entity (transactions table)
│   ├── TransactionRepository.java            # Data access layer
│   ├── TransactionController.java            # POST/GET /api/transactions
│   └── TransactionType.java                  # BUY / SELL enum
│
├── marketdata/                               # External market data services
│   ├── MarketDataService.java                # Interface (includes PriceData record)
│   ├── MarketDataServiceImpl.java            # Implementation: delegates to specific Client
│   ├── TwelveDataPriceService.java           # TwelveData API client
│   ├── ExchangeRateService.java              # OpenExchangeRates client
│   └── MarketDataConfig.java                 # RestTemplate Bean + API Key
│
├── portfolio/                                # Core business logic (extends existing package)
│   ├── AnalyticsController.java              # GET /value + /performance
│   ├── AnalyticsService.java                 # Calculation logic
│   ├── PortfolioValueResponse.java           # US-06 response DTO
│   ├── PortfolioPerformanceResponse.java     # US-07 response DTO
│   └── MarketDataUnavailableException.java   # Market data unavailable exception
│
└── common/
    └── GlobalExceptionHandler.java           # Unified exception handling (extended)
```

---

## Business Logic

### Weighted Average Cost Calculation

```
Total Spend = Σ(quantity per purchase × price per unit per purchase)
Total Quantity = Σ(quantity per purchase)
Weighted Average Cost = Total Spend ÷ Total Quantity
```

Example: AAPL bought at $180 for 10 shares, then at $190 for 5 shares

```
Weighted Average Cost = (10×180 + 5×190) ÷ 15 = $183.33
```

### Per-Asset Calculations

```
Cost Basis            = quantity held × weighted average cost
Current Value         = quantity held × current price
Unrealized P&L        = current value − cost basis
Return Percentage     = unrealized P&L ÷ cost basis × 100%
Allocation Percentage = per-asset current value ÷ total portfolio value × 100%
```

### Portfolio Aggregation

```
Total Cost        = Σ(cost basis per asset)
Total Market Value = Σ(current value per asset)
Total P&L          = total market value − total cost
Total Return %     = total P&L ÷ total cost × 100%
```

### Precision and Rounding

- Uses `BigDecimal` throughout to avoid floating-point errors
- Rounding mode: `RoundingMode.HALF_UP`
- Unit price / cost: 4 decimal places; return percentage: 4 decimal places

### Edge Case Handling

| Scenario | Handling |
|----------|----------|
| Empty portfolio | Returns zero values; no division-by-zero errors |
| No transaction records | averageCost = null, returnPercentage = 0 |
| Cost is zero | Return percentage is not computed; returns 0 |
| Missing market data | status = PARTIAL, missingPrices lists missing symbols |
| All market data unavailable | status = UNAVAILABLE |

---

## External API Integration

### TwelveData

```
GET https://api.twelvedata.com/quote?symbol={SYMBOL}&apikey={KEY}
```

**Sample Response**:

```json
{
  "symbol": "AAPL",
  "name": "Apple Inc.",
  "exchange": "NASDAQ",
  "currency": "USD",
  "close": "336.91",
  "datetime": "2026-07-27"
}
```

**Caching Strategy**: 60-second in-memory cache to avoid repeated calls consuming API quota.

### Open Exchange Rates

```
GET https://openexchangerates.org/api/latest.json?app_id={APP_ID}
```

The free tier uses USD as the base currency. Non-USD prices are converted to USD by dividing by the corresponding exchange rate:

```
GBP → USD: amount ÷ GBP_rate
```

**Usage Example**: £100, GBP rate = 0.79 → 100 ÷ 0.79 = $126.58

**Caching Strategy**: 5-minute cache; exchange rates are shared across all currencies.

---

## Exception Handling

| Exception | HTTP Status | Description |
|-----------|-------------|-------------|
| `MarketDataUnavailableException` | 503 | External market data service unavailable |
| TwelveData API 401/403 | Silent degradation, no exception thrown | Returns UNAVAILABLE |
| Network timeout | Silent degradation, no exception thrown | Returns UNAVAILABLE |

When market data is unavailable, **holding queries do not fail** — holding data is returned as normal, with only the status flag and missingPrices list indicating the issue.

---

## Configuration

### application.yml

```yaml
marketdata:
  twelvedata:
    api-key: ${TWELVE_DATA_API_KEY:default key}
  openexchangerates:
    api-key: ${OPENEXCHANGERATES_API_KEY:default key}
```

Supports overriding via environment variables (for production use):

```bash
export TWELVE_DATA_API_KEY=your_key
export OPENEXCHANGERATES_API_KEY=your_key
```

### Database Migration Files

| Version | File | Description |
|---------|------|-------------|
| V1 | `V1__create_portfolio_items.sql` | Holdings table |
| V2 | `V2__create_transactions.sql` | Transaction records table |

---

## Testing

### Unit Tests: `AnalyticsServiceTest` (14 test cases)

| Scenario | Verification |
|----------|-------------|
| All market data available | COMPLETE, market values correct |
| Partial market data missing | PARTIAL, missingPrices list present |
| All market data unavailable | UNAVAILABLE |
| Empty portfolio | Zero values, no division by zero |
| Weighted average cost | Multiple purchases at different prices |
| Unrealized loss | Current price below cost |
| No transaction records | Cost is zero, no errors |
| Asset allocation | Sum of all allocation percentages = 100% |
| Non-USD conversion | Exchange rate conversion |
| Update timestamp | Uses latest market data time |

### Integration Tests: `AnalyticsControllerIntegrationTest` (6 test cases)

Uses H2 in-memory database + Mockito to mock market data services, verifying the complete HTTP request-response chain.

### Running Tests

```bash
# Docker (recommended)
docker run --rm \
  -v "$(pwd)/backend:/workspace" \
  -v "$HOME/.m2:/root/.m2" \
  -w /workspace \
  maven:3.9-eclipse-temurin-21 \
  mvn test

# Local (requires Java 21 + Maven)
cd backend && mvn test
```

**Test Results**: 23 tests all passed, 0 failures, 0 skipped.

---

## How to Start

```bash
# 1. Copy and fill in .env
cp .env.example .env
# Edit .env, fill in passwords and API keys

# 2. Start all services
docker compose up -d --build

# 3. Confirm services are running
docker compose ps

# 4. Test the APIs
curl -X POST http://localhost:8080/api/portfolio/items \
  -H "Content-Type: application/json" \
  -d '{"assetType":"STOCK","symbol":"AAPL","quantity":10}'

curl -X POST http://localhost:8080/api/transactions \
  -H "Content-Type: application/json" \
  -d '{"transactionType":"BUY","assetType":"STOCK","symbol":"AAPL","quantity":10,"pricePerUnit":180.50,"currency":"USD","purchasedAt":"2026-07-25T10:30:00Z"}'

curl http://localhost:8080/api/portfolio/value
curl http://localhost:8080/api/portfolio/performance
```

# Portfolio Manager User Stories

## Epic

**As an investor,**  
I want to record, view, and manage my investment portfolio, and understand its basic performance,  
so that I can keep track of which assets I hold and their current value.

## US-01: View Portfolio

**As an investor,**  
I want to view all assets in my portfolio,  
so that I can quickly understand my current holdings.

### Acceptance Criteria

```gherkin
Given portfolio assets exist in the database
When the user requests to view the portfolio
Then the system returns the id, assetType, symbol, and quantity for each asset
And the HTTP status code is 200
```

```gherkin
Given no portfolio assets exist in the database
When the user requests to view the portfolio
Then the system returns an empty array
And the HTTP status code is 200
```

Corresponding endpoint:

```http
GET /api/portfolio/items
```

## US-02: Add Asset

**As an investor,**  
I want to add an asset by entering its asset type, stock symbol, and quantity held,  
so that I can record my holdings in the portfolio.

### Acceptance Criteria

```gherkin
Given the user provides valid assetType, symbol, and quantity
When the user submits a request to add an asset
Then the system saves the asset to the database
And the symbol is trimmed and converted to uppercase
And the system returns the new asset with its id
And the HTTP status code is 201
```

```gherkin
Given the symbol is empty or the quantity is less than or equal to 0
When the user submits a request to add an asset
Then the system does not save the data
And returns field-level error messages
And the HTTP status code is 400
```

Corresponding endpoint:

```http
POST /api/portfolio/items
```

Example request:

```json
{
  "assetType": "STOCK",
  "symbol": "AAPL",
  "quantity": 10
}
```

## US-03: Delete Asset

**As an investor,**  
I want to delete an asset I no longer hold from the portfolio,  
so that my portfolio records remain accurate.

### Acceptance Criteria

```gherkin
Given an asset with the specified id exists
When the user deletes that asset
Then the system removes the record from the database
And the HTTP status code is 204
```

```gherkin
Given no asset with the specified id exists
When the user attempts to delete that asset
Then the system returns a clear not-found error
And the HTTP status code is 404
```

Corresponding endpoint:

```http
DELETE /api/portfolio/items/{id}
```

## US-04: Persist Portfolio

**As an investor,**  
I want my portfolio data to persist after the application restarts,  
so that I can continue viewing and managing my holdings later.

### Acceptance Criteria

```gherkin
Given the user has added an asset
When the Spring Boot and MySQL containers are restarted
Then the user can still query that asset
```

Technical requirements:

- Use MySQL to persist portfolio data.
- Use Docker named volumes to persist MySQL data.
- A plain `docker compose down` should not delete the database data.

## US-05: View Purchase History (Phase 2)

**As an investor,**  
I want to view my stock purchase history chronologically,  
so that I can see when, at what price, and how many assets I purchased.

### Acceptance Criteria

```gherkin
Given the user has recorded one or more purchase transactions
When the user views the purchase history
Then the system returns records ordered from newest to oldest by purchase time
And each record contains id, assetType, symbol, quantity, pricePerUnit, currency, and purchasedAt
And the HTTP status code is 200
```

```gherkin
Given the user has no purchase records yet
When the user views the purchase history
Then the system returns an empty array
And the HTTP status code is 200
```

```gherkin
Given the user has deleted a current holding
When the user views the purchase history
Then the previous purchase records for that asset still exist
```

```gherkin
Given the user provides a quantity or pricePerUnit less than or equal to 0
When the user submits a purchase record
Then the system does not save the record
And returns field-level error messages
And the HTTP status code is 400
```

Suggested endpoints:

```http
POST /api/transactions
GET  /api/transactions?type=BUY
```

Example purchase record:

```json
{
  "transactionType": "BUY",
  "assetType": "STOCK",
  "symbol": "AAPL",
  "quantity": 10,
  "pricePerUnit": 180.50,
  "currency": "USD",
  "purchasedAt": "2026-07-27T10:30:00Z"
}
```

Technical notes:

- Purchase history should be stored in a separate `transactions` table and must not be derived solely from current holdings.
- Current holdings represent "how much is held now," while purchase history represents "what happened in the past."
- Deleting a current holding should not cascade-delete historical transactions.
- When sell functionality is added later, the `SELL` transaction type can be used in the same table.

Suggested minimal transaction data model:

```text
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

## US-06: View Current Value (Phase 2)

**As an investor,**  
I want to see the current price and market value of each stock holding,  
so that I know how much my portfolio is currently worth.

### Acceptance Criteria

```gherkin
Given valid stock symbols and quantities exist in the portfolio
And the market data service can return current prices
When the user views the portfolio
Then the system displays the current price
And the system displays the market value calculated as quantity multiplied by currentPrice
And the response indicates the price currency and update time
```

```gherkin
Given the external market data service is temporarily unavailable
When the user views the portfolio
Then the saved holding data is still displayed
And the system clearly indicates that market data is temporarily unavailable
```

## US-07: Analyze Portfolio Performance (Phase 2)

**As an investor,**  
I want to see the cost, current value, profit/loss, and return rate of the portfolio and each asset,  
so that I can evaluate my investment performance and understand which assets have the greatest impact on the overall result.

### Acceptance Criteria

```gherkin
Given the user has purchase records
And the market data service can return current prices
When the user views the Performance analysis
Then the system returns the total cost invested
And the system returns the current total portfolio value
And the system returns the unrealized profit/loss amount
And the system returns the unrealized return rate
And the response indicates the currency and market data update time
And the HTTP status code is 200
```

```gherkin
Given the user has purchased the same stock multiple times at different prices
When the system calculates the cost of that stock
Then the system uses the weighted average purchase cost
And correctly calculates the unrealized profit/loss and return rate for that stock
```

```gherkin
Given the portfolio contains multiple assets
When the user views the Performance analysis
Then the system returns the cost, current value, profit/loss, and return rate for each asset
And the system returns each asset's proportion of the portfolio's current value
```

```gherkin
Given the user currently has no holdings
When the user views the Performance analysis
Then the system returns total cost and current value as 0
And does not produce a division-by-zero error
And the HTTP status code is 200
```

```gherkin
Given some assets are missing current market data
When the user views the Performance analysis
Then the system marks the result as PARTIAL
And explicitly lists the stock symbols missing market data
And does not incorrectly treat missing prices as 0
```

Suggested endpoint:

```http
GET /api/portfolio/performance
```

Example response:

```json
{
  "currency": "USD",
  "totalCost": 1805.00,
  "currentValue": 1950.00,
  "unrealizedProfitLoss": 145.00,
  "returnPercentage": 8.03,
  "status": "COMPLETE",
  "priceUpdatedAt": "2026-07-27T10:35:00Z",
  "assets": [
    {
      "symbol": "AAPL",
      "quantity": 10,
      "averageCost": 180.50,
      "currentPrice": 195.00,
      "currentValue": 1950.00,
      "unrealizedProfitLoss": 145.00,
      "returnPercentage": 8.03,
      "allocationPercentage": 100.00
    }
  ],
  "missingPrices": []
}
```

Basic calculation rules:

```text
Single asset cost = quantity held × weighted average purchase price
Single asset current value = quantity held × current price
Unrealized profit/loss = current value - current holding cost
Return rate = unrealized profit/loss ÷ current holding cost × 100%
Asset allocation = single asset current value ÷ portfolio total current value × 100%
```

Technical notes:

- This feature depends on the purchase history from US-05 and current market data from US-06.
- Amount and ratio calculations should use `BigDecimal`, not `double`.
- Multi-currency assets cannot be directly summed; the first version may support only one base currency.
- The Performance response should include the market data update time to avoid users mistakenly thinking the data is real-time.
- Historical return curves require historical market data or daily portfolio snapshots and can be considered as a future enhancement.
- The page can use a line chart to show portfolio value changes and a pie chart to show asset allocation.

## MVP Definition of Done

The first version MVP is considered complete when all of the following conditions are met:

- `GET /api/portfolio/items` can query all assets.
- `POST /api/portfolio/items` can validate and save assets.
- `DELETE /api/portfolio/items/{id}` can delete assets.
- Invalid requests return a unified error format.
- Data is not lost after the MySQL container restarts.
- Maven tests pass.
- The Docker image can be built.
- GitHub CI checks pass.
- The README includes startup instructions and API call examples.

US-05, US-06, and US-07 belong to Phase 2 and do not block the first version MVP delivery.

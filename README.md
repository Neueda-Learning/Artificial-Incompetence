# Portfolio Manager

Portfolio Manager is a full-stack portfolio tracking application. It allows
users to maintain their holdings, record purchases, view current valuations,
and analyse portfolio performance using live and historical market data.

## Implemented Features

### Portfolio and Holdings

- Add stock, ETF, bond, and cash holdings.
- Store the symbol, asset type, quantity, company name, exchange, and currency.
- Retrieve company metadata from Twelve Data when an asset is first added.
- Combine repeated entries for the same symbol into one active holding.
- Partially reduce or completely remove a holding.
- Keep portfolio data in MySQL across container restarts.

### Purchase History

- Record purchases and removals in one immutable `portfolio_activities` ledger.
- Store purchase quantity, price, currency, and date without a duplicate
  `transactions` table.
- Display purchase history in reverse chronological order.
- Replay added and removed activity to calculate remaining average cost and
  cost basis.

### Current Valuation and Performance

- Retrieve current market prices from Twelve Data.
- Convert non-USD values using Open Exchange Rates.
- Calculate current market value for each asset.
- Calculate total cost basis, unrealised profit or loss, return percentage, and
  portfolio allocation.
- Report `COMPLETE`, `PARTIAL`, or `UNAVAILABLE` when external market data is
  missing.

### Historical Performance

- Support `1W`, `1M`, `3M`, `1Y`, and `ALL` performance ranges.
- Retrieve and store historical prices and exchange rates.
- Reuse cached market data from MySQL to reduce repeated external API calls.
- Calculate historical market value, cost basis, profit or loss, and return
  percentage.
- Return details about missing price or exchange-rate data.

### Web Interface

- Dashboard with portfolio summary, market indicators, top holdings, and recent
  activity.
- Holdings page with positions, allocation, and purchase-history views.
- Performance page with portfolio and per-asset metrics.
- Add Asset and Remove Asset workflows.
- Loading, empty, partial-data, and error states.
- Client-side routing with direct URL support through Nginx.

## Technology Stack

### Backend

- Java 21
- Spring Boot 4.1
- Spring Web MVC
- Spring Data JPA and Hibernate
- Jakarta Bean Validation
- Spring Boot Actuator
- Maven

### Frontend

- React 18
- TypeScript
- React Router
- Axios
- Create React App
- Nginx

### Data and External Services

- MySQL 8.4
- Flyway database migrations
- H2 in-memory database for backend tests
- Twelve Data API for asset metadata and market prices
- Open Exchange Rates API for currency conversion

### Development and Delivery

- Docker and Docker Compose
- GitHub Actions continuous integration
- Jest and React Testing Library
- JUnit and Spring Boot Test

## Project Structure

```text
.
├── backend/                 Spring Boot REST API
│   └── src/main/
│       ├── java/com/hsbc/portfoliomanager/
│       │   ├── portfolio/
│       │   │   ├── holding/     Holding and asset-metadata feature
│       │   │   ├── activity/    Unified purchase and removal ledger
│       │   │   ├── transaction/ Purchase API and validation
│       │   │   └── analytics/   Valuation and performance feature
│       │   ├── marketdata/      Current and historical market-data feature
│       │   └── common/          Shared API error handling
│       └── resources/
│           └── db/migration Flyway SQL migrations
├── frontend/                React and TypeScript web application
├── compose.yaml             Frontend, backend, and MySQL services
├── .env.example             Environment variable template
└── .github/workflows/       GitHub Actions CI configuration
```

## Configuration

Create a local environment file:

```bash
cp .env.example .env
```

Set the MySQL passwords and personal API keys in `.env`:

```dotenv
MYSQL_DATABASE=portfolio
MYSQL_USER=portfolio_user
MYSQL_PASSWORD=your_development_password
MYSQL_ROOT_PASSWORD=your_root_password

MYSQL_PORT=3306
BACKEND_PORT=8080
FRONTEND_PORT=3000

TWELVE_DATA_API_KEY=your_twelve_data_key
OPENEXCHANGERATES_API_KEY=your_open_exchange_rates_key
```

The `.env` file is ignored by Git and must not be committed. Each developer can
use a personal API key without changing the shared database structure or API
response format.

## Run with Docker

Build and start the complete application:

```bash
docker compose up --build -d
```

Open the application:

- Frontend: `http://localhost:3000`
- Backend API: `http://localhost:8080`
- Health check: `http://localhost:8080/actuator/health`

Check container status and logs:

```bash
docker compose ps
docker compose logs -f backend
docker compose logs -f frontend
docker compose logs -f database
```

Stop the application without deleting MySQL data:

```bash
docker compose down
```

## Main REST API

| Method | Endpoint | Description |
| --- | --- | --- |
| `GET` | `/api/portfolio/items` | List current holdings |
| `POST` | `/api/portfolio/items` | Add a holding |
| `DELETE` | `/api/portfolio/items/{id}` | Delete a holding |
| `GET` | `/api/transactions?type=BUY` | List purchase history |
| `POST` | `/api/transactions` | Record a purchase |
| `GET` | `/api/portfolio/value` | Calculate current portfolio value |
| `GET` | `/api/portfolio/performance` | Calculate current performance |
| `GET` | `/api/portfolio/performance/history?range=1M` | Calculate historical performance |

Example request:

```bash
curl -X POST http://localhost:8080/api/portfolio/items \
  -H 'Content-Type: application/json' \
  -d '{"assetType":"STOCK","symbol":"AAPL","quantity":10}'
```

## Local Development

Start MySQL in Docker:

```bash
docker compose up -d database
```

Run the backend:

```bash
cd backend
DB_USERNAME=portfolio_user \
DB_PASSWORD=your_password_from_dot_env \
mvn spring-boot:run
```

Run the frontend in another terminal:

```bash
cd frontend
npm install
npm start
```

The React development server proxies `/api` requests to
`http://localhost:8080`.

## Tests

The repository contains **255 automated test cases**:

- **Backend:** 150 tests (JUnit + Spring Boot Test, including service and integration-style controller tests)
- **Frontend:** 105 tests (Jest + React Testing Library for components, service adapters, and utilities)

For detailed per-module coverage, see [TEST_DOCUMENTATION.md](./TEST_DOCUMENTATION.md).

Run backend tests:

```bash
cd backend
mvn test
```

Run frontend tests and type checking:

```bash
cd frontend
npm run test:ci
npm run typecheck
```

## Continuous Integration

GitHub Actions runs for pushes and pull requests targeting `main`. The workflow:

1. Sets up Java 21.
2. runs Maven verification and backend tests;
3. packages the Spring Boot application; and
4. builds the backend Docker image without publishing it.

The workflow is defined in `.github/workflows/ci.yml`.

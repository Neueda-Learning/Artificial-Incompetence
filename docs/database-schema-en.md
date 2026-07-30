# Portfolio Manager Database Schema

## 1. Unified Approach

The database structure is centrally managed by Flyway migration files:

```text
backend/src/main/resources/db/migration/
├── V1__create_portfolio_items.sql
├── V2__create_transactions.sql
├── V3__add_asset_metadata_to_portfolio_items.sql
├── V4__create_market_history_tables.sql
├── V5__create_portfolio_activities.sql
└── V6__remove_redundant_transactions_table.sql
```

All three backend developers use their own MySQL containers and data, but when launching the same version of the code, Flyway executes the same migrations in version order.

Do not manually add or modify fields directly in personal databases. Structural changes should be introduced via new migration files, for example:

```text
V3__add_transaction_notes.sql
```

Migration files that have already been executed in other environments must not be modified or renamed.

## 2. Table Relationships

```text
portfolio_items
    Current holding snapshot
    One row per asset

portfolio_activities
    Unified historical ledger for additions, purchases, and removals
    One row per business operation
```

The two tables do not use cascading deletes. When a current holding is deleted, the `portfolio_activities` history records are still preserved.

`symbol`, `asset_type`, and `currency` together represent an asset's identity. The first version uniformly uses USD, but the database retains the `currency` field.

## 3. portfolio_items

| Field | Type | Rule | Purpose |
|---|---|---|---|
| `id` | `BIGINT` | Primary key, auto-increment | Holding ID |
| `asset_type` | `VARCHAR(20)` | STOCK/BOND/CASH | Asset type |
| `symbol` | `VARCHAR(20)` | Not null | Stock or asset ticker |
| `company_name` | `VARCHAR(255)` | Nullable | Company name, fetched once by the market data API |
| `exchange` | `VARCHAR(50)` | Nullable | Exchange, fetched once by the market data API |
| `quantity` | `DECIMAL(19,6)` | Greater than 0 | Current holding quantity |
| `currency` | `VARCHAR(3)` | Default USD | ISO currency code |
| `created_at` | `TIMESTAMP(6)` | Auto-generated | Creation time |
| `updated_at` | `TIMESTAMP(6)` | Auto-updated | Last modification time |

Unique constraint:

```text
asset_type + symbol + currency
```

When purchasing the same asset again, the existing holding quantity should be updated rather than inserting a second row.

## 4. portfolio_activities

| Field | Type | Rule | Purpose |
|---|---|---|---|
| `id` | `BIGINT` | Primary key, auto-increment | Activity ID |
| `action_type` | `VARCHAR(10)` | ADDED/REMOVED | Activity type |
| `asset_type` | `VARCHAR(20)` | STOCK/BOND/CASH | Asset type |
| `symbol` | `VARCHAR(20)` | Not null | Asset ticker at the time of the activity |
| `quantity` | `DECIMAL(19,6)` | Greater than 0 | Quantity added or removed |
| `price_per_unit` | `DECIMAL(19,4)` | Has value when ADDED | Price per unit at time of purchase |
| `currency` | `VARCHAR(3)` | Nullable | Activity currency |
| `remaining_quantity` | `DECIMAL(19,6)` | Has value when REMOVED | Remaining holding after removal |
| `occurred_at` | `TIMESTAMP(6)` | Not null | Time when the activity occurred |
| `created_at` | `TIMESTAMP(6)` | Auto-generated | Record creation time |

Index support:

- Query Recent Activity and History by activity time.
- Reconstruct cost basis and historical Performance by ticker and time.

`transactions` is the old table created in V2. V5 first copies old records into `portfolio_activities`, and V6 deletes the old table after confirming the migration order.

## 5. Write Rules

When recording a purchase, the following should occur within a single Spring transaction:

1. Insert an `ADDED` activity record.
2. Insert or update the corresponding `portfolio_items` row.

```text
First purchase of AAPL 10 shares
    portfolio_items: AAPL 10
    portfolio_activities: ADDED AAPL 10

Second purchase of AAPL 5 shares
    portfolio_items: AAPL 15
    portfolio_activities: Another ADDED AAPL 5 record
```

Partial reduction or liquidation adds a `REMOVED` record. A full liquidation only deletes `portfolio_items` — `portfolio_activities` must not be deleted.

## 6. Performance Data Sources

```text
Current quantity        portfolio_items.quantity
Historical cost         ADDED/REMOVED records in portfolio_activities
Current stock price     External market data API
Current exchange rate   Open Exchange Rates API
```

Current stock prices and exchange rates change frequently and are not stored in these two core business tables.

## 7. Handling Locally Existing Old Tables

If the MySQL data volume previously had tables created by Hibernate, Flyway will not take over old structures that are not under version management. Since the project is still in its early stages and old data is not important, each developer should first back up any needed data, then delete the local development data volume and reinitialize from migrations.

Deleting the volume permanently removes that member's local database data, so it must only be performed by each member after their own confirmation.

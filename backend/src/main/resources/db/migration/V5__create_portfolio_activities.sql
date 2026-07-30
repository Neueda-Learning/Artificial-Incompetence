CREATE TABLE portfolio_activities (
    id BIGINT NOT NULL AUTO_INCREMENT,
    action_type VARCHAR(10) NOT NULL,
    asset_type VARCHAR(20) NOT NULL,
    symbol VARCHAR(20) NOT NULL,
    quantity DECIMAL(19, 6) NOT NULL,
    price_per_unit DECIMAL(19, 4) NULL,
    currency VARCHAR(3) NULL,
    remaining_quantity DECIMAL(19, 6) NULL,
    occurred_at TIMESTAMP(6) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    CONSTRAINT pk_portfolio_activities PRIMARY KEY (id),
    CONSTRAINT chk_portfolio_activities_action
        CHECK (action_type IN ('ADDED', 'REMOVED')),
    CONSTRAINT chk_portfolio_activities_asset_type
        CHECK (asset_type IN ('STOCK', 'BOND', 'CASH')),
    CONSTRAINT chk_portfolio_activities_quantity
        CHECK (quantity > 0),
    CONSTRAINT chk_portfolio_activities_currency
        CHECK (currency IS NULL OR CHAR_LENGTH(currency) = 3),
    CONSTRAINT chk_portfolio_activities_remaining
        CHECK (remaining_quantity IS NULL OR remaining_quantity >= 0),

    INDEX idx_portfolio_activities_time (occurred_at),
    INDEX idx_portfolio_activities_symbol_time (symbol, occurred_at)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

INSERT INTO portfolio_activities (
    action_type,
    asset_type,
    symbol,
    quantity,
    price_per_unit,
    currency,
    remaining_quantity,
    occurred_at
)
SELECT
    CASE
        WHEN transaction_type = 'BUY' THEN 'ADDED'
        ELSE 'REMOVED'
    END,
    asset_type,
    symbol,
    quantity,
    price_per_unit,
    currency,
    NULL,
    transacted_at
FROM transactions;

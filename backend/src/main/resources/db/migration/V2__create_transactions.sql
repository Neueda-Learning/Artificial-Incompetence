CREATE TABLE transactions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    transaction_type VARCHAR(10) NOT NULL,
    asset_type VARCHAR(20) NOT NULL,
    symbol VARCHAR(20) NOT NULL,
    quantity DECIMAL(19, 6) NOT NULL,
    price_per_unit DECIMAL(19, 4) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    transacted_at TIMESTAMP(6) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    CONSTRAINT pk_transactions PRIMARY KEY (id),
    CONSTRAINT chk_transactions_type
        CHECK (transaction_type IN ('BUY', 'SELL')),
    CONSTRAINT chk_transactions_asset_type
        CHECK (asset_type IN ('STOCK', 'BOND', 'CASH')),
    CONSTRAINT chk_transactions_quantity
        CHECK (quantity > 0),
    CONSTRAINT chk_transactions_price
        CHECK (price_per_unit > 0),
    CONSTRAINT chk_transactions_currency
        CHECK (CHAR_LENGTH(currency) = 3),

    INDEX idx_transactions_type_time (transaction_type, transacted_at),
    INDEX idx_transactions_symbol_time (symbol, transacted_at)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

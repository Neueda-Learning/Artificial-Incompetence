CREATE TABLE portfolio_items (
    id BIGINT NOT NULL AUTO_INCREMENT,
    asset_type VARCHAR(20) NOT NULL,
    symbol VARCHAR(20) NOT NULL,
    quantity DECIMAL(19, 6) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),

    CONSTRAINT pk_portfolio_items PRIMARY KEY (id),
    CONSTRAINT uk_portfolio_items_asset
        UNIQUE (asset_type, symbol, currency),
    CONSTRAINT chk_portfolio_items_asset_type
        CHECK (asset_type IN ('STOCK', 'BOND', 'CASH')),
    CONSTRAINT chk_portfolio_items_quantity
        CHECK (quantity > 0),
    CONSTRAINT chk_portfolio_items_currency
        CHECK (CHAR_LENGTH(currency) = 3)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
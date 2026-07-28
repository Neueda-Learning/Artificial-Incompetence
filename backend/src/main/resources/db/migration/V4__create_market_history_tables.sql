CREATE TABLE price_history (
    id BIGINT NOT NULL AUTO_INCREMENT,
    asset_type VARCHAR(20) NOT NULL,
    symbol VARCHAR(20) NOT NULL,
    exchange_name VARCHAR(50) NOT NULL DEFAULT '',
    currency VARCHAR(3) NOT NULL,
    price_date DATE NOT NULL,
    time_interval VARCHAR(20) NOT NULL DEFAULT '1day',
    open_price DECIMAL(19, 6) NULL,
    high_price DECIMAL(19, 6) NULL,
    low_price DECIMAL(19, 6) NULL,
    close_price DECIMAL(19, 6) NOT NULL,
    volume DECIMAL(24, 6) NULL,
    source VARCHAR(30) NOT NULL DEFAULT 'TWELVE_DATA',
    source_updated_at TIMESTAMP(6) NULL,
    fetched_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    CONSTRAINT pk_price_history PRIMARY KEY (id),
    CONSTRAINT uk_price_history_point UNIQUE (
        asset_type,
        symbol,
        exchange_name,
        currency,
        price_date,
        time_interval
    ),
    CONSTRAINT chk_price_history_asset_type
        CHECK (asset_type IN ('STOCK', 'BOND', 'CASH')),
    CONSTRAINT chk_price_history_currency
        CHECK (CHAR_LENGTH(currency) = 3),
    CONSTRAINT chk_price_history_close
        CHECK (close_price >= 0),

    INDEX idx_price_history_lookup (symbol, price_date)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE exchange_rate_history (
    id BIGINT NOT NULL AUTO_INCREMENT,
    base_currency VARCHAR(3) NOT NULL,
    quote_currency VARCHAR(3) NOT NULL,
    rate_date DATE NOT NULL,
    exchange_rate DECIMAL(24, 12) NOT NULL,
    source VARCHAR(30) NOT NULL DEFAULT 'OPEN_EXCHANGE_RATES',
    source_timestamp TIMESTAMP(6) NULL,
    fetched_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    CONSTRAINT pk_exchange_rate_history PRIMARY KEY (id),
    CONSTRAINT uk_exchange_rate_history_point UNIQUE (
        base_currency,
        quote_currency,
        rate_date
    ),
    CONSTRAINT chk_exchange_rate_history_base
        CHECK (CHAR_LENGTH(base_currency) = 3),
    CONSTRAINT chk_exchange_rate_history_quote
        CHECK (CHAR_LENGTH(quote_currency) = 3),
    CONSTRAINT chk_exchange_rate_history_positive
        CHECK (exchange_rate > 0),

    INDEX idx_exchange_rate_history_lookup (
        base_currency,
        quote_currency,
        rate_date
    )
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

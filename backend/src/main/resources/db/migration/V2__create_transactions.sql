CREATE TABLE transactions
(
    id               BIGINT AUTO_INCREMENT NOT NULL,
    transaction_type VARCHAR(10)   NOT NULL,
    asset_type       VARCHAR(20)   NOT NULL,
    symbol           VARCHAR(20)   NOT NULL,
    quantity         DECIMAL(20, 8) NOT NULL,
    price_per_unit   DECIMAL(20, 4) NOT NULL,
    currency         VARCHAR(10)   NOT NULL,
    purchased_at     TIMESTAMP     NOT NULL,
    PRIMARY KEY (id)
);

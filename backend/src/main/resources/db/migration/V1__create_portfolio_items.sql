CREATE TABLE portfolio_items
(
    id        BIGINT AUTO_INCREMENT NOT NULL,
    asset_type VARCHAR(20)  NOT NULL,
    symbol    VARCHAR(20)  NOT NULL,
    quantity  DECIMAL(20, 8) NOT NULL,
    PRIMARY KEY (id)
);

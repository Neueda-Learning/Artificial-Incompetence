ALTER TABLE portfolio_items
    ADD COLUMN company_name VARCHAR(255) NULL AFTER symbol,
    ADD COLUMN exchange VARCHAR(50) NULL AFTER company_name;

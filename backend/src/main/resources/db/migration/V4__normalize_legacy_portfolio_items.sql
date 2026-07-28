-- Bring portfolio_items databases created before Flyway under the current
-- schema contract. These statements are also safe after the normal V1
-- migration, so fresh installations and upgraded installations converge.
ALTER TABLE portfolio_items
    MODIFY COLUMN asset_type VARCHAR(20) NOT NULL,
    MODIFY COLUMN symbol VARCHAR(20) NOT NULL,
    MODIFY COLUMN quantity DECIMAL(19, 6) NOT NULL;

SET @add_currency = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE portfolio_items ADD COLUMN currency VARCHAR(3) NOT NULL DEFAULT ''USD'' AFTER quantity',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'portfolio_items'
      AND column_name = 'currency'
);
PREPARE add_currency_stmt FROM @add_currency;
EXECUTE add_currency_stmt;
DEALLOCATE PREPARE add_currency_stmt;

SET @add_created_at = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE portfolio_items ADD COLUMN created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) AFTER currency',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'portfolio_items'
      AND column_name = 'created_at'
);
PREPARE add_created_at_stmt FROM @add_created_at;
EXECUTE add_created_at_stmt;
DEALLOCATE PREPARE add_created_at_stmt;

SET @add_updated_at = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE portfolio_items ADD COLUMN updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) AFTER created_at',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'portfolio_items'
      AND column_name = 'updated_at'
);
PREPARE add_updated_at_stmt FROM @add_updated_at;
EXECUTE add_updated_at_stmt;
DEALLOCATE PREPARE add_updated_at_stmt;

SET @add_asset_type_check = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE portfolio_items ADD CONSTRAINT chk_portfolio_items_asset_type_v4 CHECK (asset_type IN (''STOCK'', ''ETF'', ''BOND'', ''CASH''))',
        'SELECT 1'
    )
    FROM information_schema.table_constraints
    WHERE constraint_schema = DATABASE()
      AND table_name = 'portfolio_items'
      AND constraint_name = 'chk_portfolio_items_asset_type_v4'
);
PREPARE add_asset_type_check_stmt FROM @add_asset_type_check;
EXECUTE add_asset_type_check_stmt;
DEALLOCATE PREPARE add_asset_type_check_stmt;

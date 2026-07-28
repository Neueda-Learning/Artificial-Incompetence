package com.hsbc.portfoliomanager.portfolio;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

record PortfolioValueResponse(
        String currency,
        Instant priceUpdatedAt,
        String status, // COMPLETE, PARTIAL, UNAVAILABLE
        List<AssetValue> assets,
        List<String> missingPrices
) {
    record AssetValue(
            String symbol,
            String assetType,
            BigDecimal quantity,
            BigDecimal currentPrice,
            BigDecimal marketValue,
            String currency
    ) {
    }
}

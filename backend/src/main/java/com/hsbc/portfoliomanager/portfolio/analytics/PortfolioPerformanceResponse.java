package com.hsbc.portfoliomanager.portfolio.analytics;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

record PortfolioPerformanceResponse(
        String currency,
        BigDecimal totalCost,
        BigDecimal currentValue,
        BigDecimal unrealizedProfitLoss,
        BigDecimal returnPercentage,
        String status, // COMPLETE, PARTIAL
        Instant priceUpdatedAt,
        List<AssetPerformance> assets,
        List<String> missingPrices
) {
    record AssetPerformance(
            String symbol,
            BigDecimal quantity,
            BigDecimal averageCost,
            BigDecimal currentPrice,
            BigDecimal costBasis,
            BigDecimal currentValue,
            BigDecimal unrealizedProfitLoss,
            BigDecimal returnPercentage,
            BigDecimal allocationPercentage
    ) {
    }
}

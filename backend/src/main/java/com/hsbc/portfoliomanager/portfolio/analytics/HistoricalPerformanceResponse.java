package com.hsbc.portfoliomanager.portfolio.analytics;

import com.hsbc.portfoliomanager.portfolio.holding.AssetType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

record HistoricalPerformanceResponse(
        String currency,
        String range,
        LocalDate startDate,
        LocalDate endDate,
        String status,
        List<PerformancePoint> points,
        List<AssetSeries> assets,
        List<String> missingData
) {
    record AssetSeries(
            String id,
            String symbol,
            AssetType assetType,
            String currency,
            List<PerformancePoint> points
    ) {
    }

    record PerformancePoint(
            LocalDate date,
            BigDecimal marketValue,
            BigDecimal costBasis,
            BigDecimal profitLoss,
            BigDecimal returnPercentage
    ) {
    }
}

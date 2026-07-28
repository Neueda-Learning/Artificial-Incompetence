package com.hsbc.portfoliomanager.portfolio;

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
        List<String> missingData
) {
    record PerformancePoint(
            LocalDate date,
            BigDecimal marketValue,
            BigDecimal costBasis,
            BigDecimal profitLoss,
            BigDecimal returnPercentage
    ) {
    }
}

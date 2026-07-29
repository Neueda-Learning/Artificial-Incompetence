package com.hsbc.portfoliomanager.marketdata;

import com.hsbc.portfoliomanager.portfolio.holding.AssetType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface HistoricalMarketDataService {

    List<PricePoint> getDailyPrices(
            AssetType assetType,
            String symbol,
            String exchange,
            String currency,
            LocalDate startDate,
            LocalDate endDate
    );

    Optional<BigDecimal> getRateToUsd(String fromCurrency, LocalDate date);

    record PricePoint(
            String symbol,
            String currency,
            LocalDate date,
            BigDecimal closePrice
    ) {
    }
}

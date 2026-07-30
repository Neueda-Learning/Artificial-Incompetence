package com.hsbc.portfoliomanager.marketdata;

import com.hsbc.portfoliomanager.portfolio.holding.AssetType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface HistoricalMarketDataService {

    /**
     * 中文：获取指定资产和日期范围内的每日收盘价格。
     * English: Returns daily closing prices for an asset within the requested date range.
     */
    List<PricePoint> getDailyPrices(
            AssetType assetType,
            String symbol,
            String exchange,
            String currency,
            LocalDate startDate,
            LocalDate endDate
    );

    /**
     * 中文：获取指定币种在某一天兑换为美元的汇率。
     * English: Returns the rate for converting a currency to USD on a given date.
     */
    Optional<BigDecimal> getRateToUsd(String fromCurrency, LocalDate date);

    record PricePoint(
            String symbol,
            String currency,
            LocalDate date,
            BigDecimal closePrice
    ) {
    }
}

package com.hsbc.portfoliomanager.marketdata;

import com.hsbc.portfoliomanager.portfolio.holding.AssetType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

interface PriceHistoryRepository extends JpaRepository<PriceHistory, Long> {

    /**
     * 中文：按资产标识和日期范围查询每日历史价格，并按日期升序返回。
     * English: Finds daily historical prices by asset identity and date range in ascending order.
     */
    List<PriceHistory> findByAssetTypeAndSymbolAndExchangeAndCurrencyAndTimeIntervalAndPriceDateBetweenOrderByPriceDateAsc(
            AssetType assetType,
            String symbol,
            String exchange,
            String currency,
            String timeInterval,
            LocalDate startDate,
            LocalDate endDate
    );

    /**
     * 中文：查询某个资产在指定日期和时间粒度下是否已有唯一价格记录。
     * English: Finds the unique stored price for an asset, date, and time interval.
     */
    Optional<PriceHistory> findByAssetTypeAndSymbolAndExchangeAndCurrencyAndPriceDateAndTimeInterval(
            AssetType assetType,
            String symbol,
            String exchange,
            String currency,
            LocalDate priceDate,
            String timeInterval
    );
}

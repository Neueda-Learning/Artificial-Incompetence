package com.hsbc.portfoliomanager.marketdata;

import com.hsbc.portfoliomanager.portfolio.AssetType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

interface PriceHistoryRepository extends JpaRepository<PriceHistory, Long> {

    List<PriceHistory> findByAssetTypeAndSymbolAndExchangeAndCurrencyAndTimeIntervalAndPriceDateBetweenOrderByPriceDateAsc(
            AssetType assetType,
            String symbol,
            String exchange,
            String currency,
            String timeInterval,
            LocalDate startDate,
            LocalDate endDate
    );

    Optional<PriceHistory> findByAssetTypeAndSymbolAndExchangeAndCurrencyAndPriceDateAndTimeInterval(
            AssetType assetType,
            String symbol,
            String exchange,
            String currency,
            LocalDate priceDate,
            String timeInterval
    );
}

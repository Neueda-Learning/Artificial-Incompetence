package com.hsbc.portfoliomanager.marketdata;

import com.hsbc.portfoliomanager.portfolio.holding.AssetType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
class HistoricalMarketDataServiceImpl implements HistoricalMarketDataService {

    private final HistoricalPriceService historicalPriceService;
    private final HistoricalExchangeRateService historicalExchangeRateService;

    /**
     * 中文：注入历史价格服务和历史汇率服务。
     * English: Injects the historical price and historical exchange-rate services.
     */
    HistoricalMarketDataServiceImpl(
            HistoricalPriceService historicalPriceService,
            HistoricalExchangeRateService historicalExchangeRateService
    ) {
        this.historicalPriceService = historicalPriceService;
        this.historicalExchangeRateService = historicalExchangeRateService;
    }

    /**
     * 中文：委托历史价格服务获取并缓存每日价格。
     * English: Delegates daily price retrieval and caching to the historical price service.
     */
    @Override
    public List<PricePoint> getDailyPrices(
            AssetType assetType,
            String symbol,
            String exchange,
            String currency,
            LocalDate startDate,
            LocalDate endDate
    ) {
        return historicalPriceService.getDailyPrices(
                assetType,
                symbol,
                exchange,
                currency,
                startDate,
                endDate
        );
    }

    /**
     * 中文：委托历史汇率服务获取并缓存指定日期的美元汇率。
     * English: Delegates dated USD exchange-rate retrieval and caching to the historical rate service.
     */
    @Override
    public Optional<BigDecimal> getRateToUsd(String fromCurrency, LocalDate date) {
        return historicalExchangeRateService.getRateToUsd(fromCurrency, date);
    }
}

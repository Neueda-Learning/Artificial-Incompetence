package com.hsbc.portfoliomanager.marketdata;

import com.hsbc.portfoliomanager.portfolio.AssetType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
class HistoricalMarketDataServiceImpl implements HistoricalMarketDataService {

    private final HistoricalPriceService historicalPriceService;
    private final HistoricalExchangeRateService historicalExchangeRateService;

    HistoricalMarketDataServiceImpl(
            HistoricalPriceService historicalPriceService,
            HistoricalExchangeRateService historicalExchangeRateService
    ) {
        this.historicalPriceService = historicalPriceService;
        this.historicalExchangeRateService = historicalExchangeRateService;
    }

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

    @Override
    public Optional<BigDecimal> getRateToUsd(String fromCurrency, LocalDate date) {
        return historicalExchangeRateService.getRateToUsd(fromCurrency, date);
    }
}

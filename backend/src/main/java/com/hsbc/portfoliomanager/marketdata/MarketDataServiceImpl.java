package com.hsbc.portfoliomanager.marketdata;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

@Service
class MarketDataServiceImpl implements MarketDataService {

    private static final Logger log = LoggerFactory.getLogger(MarketDataServiceImpl.class);

    private final TwelveDataPriceService priceService;
    private final ExchangeRateService exchangeRateService;
    private volatile boolean lastCallSucceeded = true;

    MarketDataServiceImpl(TwelveDataPriceService priceService, ExchangeRateService exchangeRateService) {
        this.priceService = priceService;
        this.exchangeRateService = exchangeRateService;
    }

    @Override
    public Optional<PriceData> getCurrentPrice(String symbol) {
        Optional<PriceData> result = priceService.fetchPrice(symbol);
        lastCallSucceeded = result.isPresent();
        return result;
    }

    @Override
    public Optional<BigDecimal> convertToUsd(BigDecimal amount, String fromCurrency) {
        return exchangeRateService.convertToUsd(amount, fromCurrency);
    }

    @Override
    public boolean isAvailable() {
        return lastCallSucceeded;
    }
}

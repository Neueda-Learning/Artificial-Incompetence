package com.hsbc.portfoliomanager.marketdata;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

public interface MarketDataService {

    /**
     * Get the current price for a stock symbol in its native currency.
     *
     * @param symbol the stock ticker symbol
     * @return price data if available, empty if the service is unavailable
     */
    Optional<PriceData> getCurrentPrice(String symbol);

    /**
     * Convert an amount from one currency to USD.
     *
     * @param amount         the amount to convert
     * @param fromCurrency   the source currency code (e.g. "GBP", "EUR")
     * @return the converted amount in USD, or empty if conversion is unavailable
     */
    Optional<BigDecimal> convertToUsd(BigDecimal amount, String fromCurrency);

    /**
     * Check whether the market data service is currently available.
     */
    boolean isAvailable();

    record PriceData(String symbol, BigDecimal price, String currency, Instant updatedAt) {
    }
}

package com.hsbc.portfoliomanager.marketdata;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

public interface MarketDataService {

    /**
     * 中文：获取股票代码以其原始计价币种表示的当前价格。
     * English: Gets the current price for a stock symbol in its native currency.
     *
     * @param symbol the stock ticker symbol
     * @return price data if available, empty if the service is unavailable
     */
    Optional<PriceData> getCurrentPrice(String symbol);

    /**
     * 中文：将指定币种的金额转换为美元。
     * English: Converts an amount from its source currency into USD.
     *
     * @param amount         the amount to convert
     * @param fromCurrency   the source currency code (e.g. "GBP", "EUR")
     * @return the converted amount in USD, or empty if conversion is unavailable
     */
    Optional<BigDecimal> convertToUsd(BigDecimal amount, String fromCurrency);

    /**
     * 中文：检查市场数据服务当前是否可用。
     * English: Checks whether the market data service is currently available.
     */
    boolean isAvailable();

    record PriceData(String symbol, BigDecimal price, String currency, Instant updatedAt) {
    }
}

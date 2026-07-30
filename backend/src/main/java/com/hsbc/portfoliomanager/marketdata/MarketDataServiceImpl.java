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

    /**
     * 中文：注入当前价格服务和汇率转换服务。
     * English: Injects the current-price service and exchange-rate conversion service.
     */
    MarketDataServiceImpl(TwelveDataPriceService priceService, ExchangeRateService exchangeRateService) {
        this.priceService = priceService;
        this.exchangeRateService = exchangeRateService;
    }

    /**
     * 中文：获取指定资产的当前价格，并记录最近一次调用是否成功。
     * English: Retrieves the current asset price and records whether the latest request succeeded.
     */
    @Override
    public Optional<PriceData> getCurrentPrice(String symbol) {
        Optional<PriceData> result = priceService.fetchPrice(symbol);
        lastCallSucceeded = result.isPresent();
        return result;
    }

    /**
     * 中文：使用资产代码和交易所获取当前价格，避免国际市场代码重名。
     * English: Retrieves current price with symbol and exchange to disambiguate international instruments.
     */
    @Override
    public Optional<PriceData> getCurrentPrice(String symbol, String exchange) {
        Optional<PriceData> result = priceService.fetchPrice(symbol, exchange);
        lastCallSucceeded = result.isPresent();
        return result;
    }

    /**
     * 中文：将指定币种金额转换为美元金额。
     * English: Converts an amount in the specified currency into USD.
     */
    @Override
    public Optional<BigDecimal> convertToUsd(BigDecimal amount, String fromCurrency) {
        return exchangeRateService.convertToUsd(amount, fromCurrency);
    }

    /**
     * 中文：返回最近一次当前价格查询是否成功。
     * English: Reports whether the most recent current-price lookup succeeded.
     */
    @Override
    public boolean isAvailable() {
        return lastCallSucceeded;
    }
}

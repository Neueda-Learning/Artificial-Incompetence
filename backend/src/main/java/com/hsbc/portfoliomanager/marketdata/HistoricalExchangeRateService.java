package com.hsbc.portfoliomanager.marketdata;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Optional;

@Service
class HistoricalExchangeRateService {

    private static final String USD = "USD";

    private final ExchangeRateHistoryRepository repository;
    private final OpenExchangeRatesHistoricalClient client;

    /**
     * 中文：注入历史汇率仓库和 Open Exchange Rates 客户端。
     * English: Injects the historical rate repository and Open Exchange Rates client.
     */
    HistoricalExchangeRateService(
            ExchangeRateHistoryRepository repository,
            OpenExchangeRatesHistoricalClient client
    ) {
        this.repository = repository;
        this.client = client;
    }

    /**
     * 中文：优先读取指定日期的本地汇率，缺失时获取并保存外部汇率。
     * English: Reads the dated rate locally first, fetching and storing it when missing.
     */
    @Transactional
    Optional<BigDecimal> getRateToUsd(String fromCurrency, LocalDate date) {
        String normalizedCurrency = fromCurrency.trim().toUpperCase(Locale.ROOT);
        if (USD.equals(normalizedCurrency)) {
            return Optional.of(BigDecimal.ONE);
        }

        Optional<ExchangeRateHistory> stored =
                repository.findByBaseCurrencyAndQuoteCurrencyAndRateDate(
                        normalizedCurrency,
                        USD,
                        date
                );
        if (stored.isPresent()) {
            return stored.map(ExchangeRateHistory::getExchangeRate);
        }

        return client.fetchRateToUsd(normalizedCurrency, date)
                .map(rate -> {
                    ExchangeRateHistory saved = repository.save(new ExchangeRateHistory(
                            normalizedCurrency,
                            USD,
                            date,
                            rate.rateToUsd(),
                            "OPEN_EXCHANGE_RATES",
                            rate.sourceTimestamp()
                    ));
                    return saved.getExchangeRate();
                });
    }
}

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

    HistoricalExchangeRateService(
            ExchangeRateHistoryRepository repository,
            OpenExchangeRatesHistoricalClient client
    ) {
        this.repository = repository;
        this.client = client;
    }

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

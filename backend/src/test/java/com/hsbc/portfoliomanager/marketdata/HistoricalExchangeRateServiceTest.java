package com.hsbc.portfoliomanager.marketdata;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("HistoricalExchangeRateService")
class HistoricalExchangeRateServiceTest {

    @Mock
    private ExchangeRateHistoryRepository repository;

    @Mock
    private OpenExchangeRatesHistoricalClient client;

    private HistoricalExchangeRateService service;

    private static final LocalDate DATE = LocalDate.of(2026, 7, 15);

    @BeforeEach
    void setUp() {
        service = new HistoricalExchangeRateService(repository, client);
    }

    @Nested
    @DisplayName("getRateToUsd")
    class GetRateToUsd {

        @Test
        @DisplayName("returns BigDecimal.ONE for USD without querying repository or client")
        void returnsOneForUsd() {
            Optional<BigDecimal> result = service.getRateToUsd("USD", DATE);

            assertThat(result).isPresent();
            assertThat(result.get()).isEqualByComparingTo("1");

            verify(repository, never()).findByBaseCurrencyAndQuoteCurrencyAndRateDate(any(), any(), any());
            verify(client, never()).fetchRateToUsd(any(), any());
        }

        @Test
        @DisplayName("returns stored rate when found in local database")
        void returnsStoredRate() {
            ExchangeRateHistory stored = new ExchangeRateHistory(
                    "GBP", "USD", DATE,
                    new BigDecimal("1.2658"),
                    "OPEN_EXCHANGE_RATES", Instant.now());

            when(repository.findByBaseCurrencyAndQuoteCurrencyAndRateDate("GBP", "USD", DATE))
                    .thenReturn(Optional.of(stored));

            Optional<BigDecimal> result = service.getRateToUsd("GBP", DATE);

            assertThat(result).isPresent();
            assertThat(result.get()).isEqualByComparingTo("1.2658");

            verify(client, never()).fetchRateToUsd(any(), any());
        }

        @Test
        @DisplayName("fetches from API and persists when no stored rate exists")
        void fetchesAndPersistsWhenMissing() {
            when(repository.findByBaseCurrencyAndQuoteCurrencyAndRateDate("EUR", "USD", DATE))
                    .thenReturn(Optional.empty());

            OpenExchangeRatesHistoricalClient.HistoricalUsdRate apiRate =
                    new OpenExchangeRatesHistoricalClient.HistoricalUsdRate(
                            "EUR", DATE, new BigDecimal("0.9200"), Instant.now());

            when(client.fetchRateToUsd("EUR", DATE))
                    .thenReturn(Optional.of(apiRate));

            ExchangeRateHistory savedEntity = new ExchangeRateHistory(
                    "EUR", "USD", DATE,
                    new BigDecimal("0.9200"),
                    "OPEN_EXCHANGE_RATES", apiRate.sourceTimestamp());
            when(repository.save(any(ExchangeRateHistory.class))).thenReturn(savedEntity);

            Optional<BigDecimal> result = service.getRateToUsd("EUR", DATE);

            assertThat(result).isPresent();
            assertThat(result.get()).isEqualByComparingTo("0.9200");

            // Verify persisted entity was saved with correct exchange rate
            assertThat(savedEntity.getExchangeRate()).isEqualByComparingTo("0.9200");

            verify(client).fetchRateToUsd("EUR", DATE);
            verify(repository).save(any(ExchangeRateHistory.class));
        }

        @Test
        @DisplayName("returns empty when client returns no rate")
        void returnsEmptyWhenClientReturnsEmpty() {
            when(repository.findByBaseCurrencyAndQuoteCurrencyAndRateDate("XYZ", "USD", DATE))
                    .thenReturn(Optional.empty());

            when(client.fetchRateToUsd("XYZ", DATE))
                    .thenReturn(Optional.empty());

            Optional<BigDecimal> result = service.getRateToUsd("XYZ", DATE);

            assertThat(result).isEmpty();

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("normalizes currency input to uppercase")
        void normalizesCurrencyInput() {
            when(repository.findByBaseCurrencyAndQuoteCurrencyAndRateDate("GBP", "USD", DATE))
                    .thenReturn(Optional.empty());

            when(client.fetchRateToUsd("GBP", DATE))
                    .thenReturn(Optional.empty());

            service.getRateToUsd(" gbp ", DATE);

            verify(repository).findByBaseCurrencyAndQuoteCurrencyAndRateDate("GBP", "USD", DATE);
            verify(client).fetchRateToUsd("GBP", DATE);
        }
    }
}

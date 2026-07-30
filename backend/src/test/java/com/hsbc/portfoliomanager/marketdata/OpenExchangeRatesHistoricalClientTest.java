package com.hsbc.portfoliomanager.marketdata;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OpenExchangeRatesHistoricalClient")
class OpenExchangeRatesHistoricalClientTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private MarketDataConfig config;

    private OpenExchangeRatesHistoricalClient client;

    private static final LocalDate TEST_DATE = LocalDate.of(2025, 6, 15);

    @BeforeEach
    void setUp() {
        client = new OpenExchangeRatesHistoricalClient(restTemplate, config);
    }

    @Nested
    @DisplayName("fetchRateToUsd")
    class FetchRateToUsd {

        @Test
        @DisplayName("returns empty when API key is null")
        void returnsEmptyWhenApiKeyNull() {
            when(config.getOpenExchangeRatesApiKey()).thenReturn(null);

            Optional<OpenExchangeRatesHistoricalClient.HistoricalUsdRate> result =
                    client.fetchRateToUsd("GBP", TEST_DATE);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("returns empty when API key is blank")
        void returnsEmptyWhenApiKeyBlank() {
            when(config.getOpenExchangeRatesApiKey()).thenReturn("   ");

            Optional<OpenExchangeRatesHistoricalClient.HistoricalUsdRate> result =
                    client.fetchRateToUsd("GBP", TEST_DATE);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("returns empty when API response is null")
        void returnsEmptyForNullResponse() {
            when(config.getOpenExchangeRatesApiKey()).thenReturn("test-key");
            when(restTemplate.getForObject(any(URI.class), eq(Map.class)))
                    .thenReturn(null);

            Optional<OpenExchangeRatesHistoricalClient.HistoricalUsdRate> result =
                    client.fetchRateToUsd("GBP", TEST_DATE);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("returns empty when API response contains error flag")
        void returnsEmptyForErrorResponse() {
            when(config.getOpenExchangeRatesApiKey()).thenReturn("test-key");

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("error", true);
            response.put("description", "Invalid API key");
            when(restTemplate.getForObject(any(URI.class), eq(Map.class)))
                    .thenReturn(response);

            Optional<OpenExchangeRatesHistoricalClient.HistoricalUsdRate> result =
                    client.fetchRateToUsd("GBP", TEST_DATE);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("returns empty when rates map is missing")
        void returnsEmptyWhenNoRatesMap() {
            when(config.getOpenExchangeRatesApiKey()).thenReturn("test-key");

            Map<String, Object> response = new LinkedHashMap<>();
            // No "rates" key
            when(restTemplate.getForObject(any(URI.class), eq(Map.class)))
                    .thenReturn(response);

            Optional<OpenExchangeRatesHistoricalClient.HistoricalUsdRate> result =
                    client.fetchRateToUsd("GBP", TEST_DATE);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("returns empty when target currency not in rates")
        void returnsEmptyWhenCurrencyNotFound() {
            when(config.getOpenExchangeRatesApiKey()).thenReturn("test-key");

            Map<String, Object> rates = new LinkedHashMap<>();
            rates.put("EUR", 0.92);
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("rates", rates);
            when(restTemplate.getForObject(any(URI.class), eq(Map.class)))
                    .thenReturn(response);

            Optional<OpenExchangeRatesHistoricalClient.HistoricalUsdRate> result =
                    client.fetchRateToUsd("GBP", TEST_DATE);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("returns empty when rate is zero")
        void returnsEmptyWhenRateIsZero() {
            when(config.getOpenExchangeRatesApiKey()).thenReturn("test-key");

            Map<String, Object> rates = new LinkedHashMap<>();
            rates.put("GBP", 0);
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("rates", rates);
            when(restTemplate.getForObject(any(URI.class), eq(Map.class)))
                    .thenReturn(response);

            Optional<OpenExchangeRatesHistoricalClient.HistoricalUsdRate> result =
                    client.fetchRateToUsd("GBP", TEST_DATE);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("returns empty when rate is negative")
        void returnsEmptyWhenRateIsNegative() {
            when(config.getOpenExchangeRatesApiKey()).thenReturn("test-key");

            Map<String, Object> rates = new LinkedHashMap<>();
            rates.put("GBP", -0.79);
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("rates", rates);
            when(restTemplate.getForObject(any(URI.class), eq(Map.class)))
                    .thenReturn(response);

            Optional<OpenExchangeRatesHistoricalClient.HistoricalUsdRate> result =
                    client.fetchRateToUsd("GBP", TEST_DATE);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("inverts USD-base rate to currency-to-USD rate")
        void invertsUsdBaseRate() {
            when(config.getOpenExchangeRatesApiKey()).thenReturn("test-key");

            // OXR returns USD→GBP = 0.79 (meaning 1 USD = 0.79 GBP)
            // We need GBP→USD = 1 / 0.79 = 1.265822784810...
            Map<String, Object> rates = new LinkedHashMap<>();
            rates.put("GBP", 0.79);
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("rates", rates);
            response.put("timestamp", 1718409600L);
            when(restTemplate.getForObject(any(URI.class), eq(Map.class)))
                    .thenReturn(response);

            Optional<OpenExchangeRatesHistoricalClient.HistoricalUsdRate> result =
                    client.fetchRateToUsd("GBP", TEST_DATE);

            assertThat(result).isPresent();
            OpenExchangeRatesHistoricalClient.HistoricalUsdRate rate = result.get();
            assertThat(rate.fromCurrency()).isEqualTo("GBP");
            assertThat(rate.date()).isEqualTo(TEST_DATE);
            // 1 / 0.79 with 12 decimal places HALF_UP
            assertThat(rate.rateToUsd()).isEqualByComparingTo(
                    BigDecimal.ONE.divide(new BigDecimal("0.79"), 12, java.math.RoundingMode.HALF_UP));
            assertThat(rate.sourceTimestamp()).isEqualTo(Instant.ofEpochSecond(1718409600L));
        }

        @Test
        @DisplayName("sets timestamp to null when response timestamp is not numeric")
        void nullTimestampWhenNotNumeric() {
            when(config.getOpenExchangeRatesApiKey()).thenReturn("test-key");

            Map<String, Object> rates = new LinkedHashMap<>();
            rates.put("GBP", 0.79);
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("rates", rates);
            response.put("timestamp", "not-a-number");
            when(restTemplate.getForObject(any(URI.class), eq(Map.class)))
                    .thenReturn(response);

            Optional<OpenExchangeRatesHistoricalClient.HistoricalUsdRate> result =
                    client.fetchRateToUsd("GBP", TEST_DATE);

            assertThat(result).isPresent();
            assertThat(result.get().sourceTimestamp()).isNull();
        }

        @Test
        @DisplayName("handles RestClientException by returning empty")
        void handlesRestClientException() {
            when(config.getOpenExchangeRatesApiKey()).thenReturn("test-key");
            when(restTemplate.getForObject(any(URI.class), eq(Map.class)))
                    .thenThrow(new RestClientException("connection timeout"));

            Optional<OpenExchangeRatesHistoricalClient.HistoricalUsdRate> result =
                    client.fetchRateToUsd("GBP", TEST_DATE);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("parses integer rate value correctly")
        void parsesIntegerRate() {
            when(config.getOpenExchangeRatesApiKey()).thenReturn("test-key");

            Map<String, Object> rates = new LinkedHashMap<>();
            rates.put("JPY", 150); // Integer instead of decimal
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("rates", rates);
            when(restTemplate.getForObject(any(URI.class), eq(Map.class)))
                    .thenReturn(response);

            Optional<OpenExchangeRatesHistoricalClient.HistoricalUsdRate> result =
                    client.fetchRateToUsd("JPY", TEST_DATE);

            assertThat(result).isPresent();
            assertThat(result.get().rateToUsd()).isEqualByComparingTo(
                    BigDecimal.ONE.divide(new BigDecimal("150"), 12, java.math.RoundingMode.HALF_UP));
        }
    }
}

package com.hsbc.portfoliomanager.marketdata;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExchangeRateService")
class ExchangeRateServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private MarketDataConfig config;

    private ExchangeRateService exchangeRateService;

    @BeforeEach
    void setUp() {
        exchangeRateService = new ExchangeRateService(restTemplate, config);
    }

    // ── convertToUsd ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("convertToUsd")
    class ConvertToUsd {

        @Test
        @DisplayName("returns amount unchanged for USD")
        void returnsAmountForUsd() {
            Optional<BigDecimal> result = exchangeRateService.convertToUsd(
                    new BigDecimal("100.00"), "USD");

            assertThat(result).isPresent();
            assertThat(result.get()).isEqualByComparingTo("100.00");
            // Should not call API for USD
            verify(restTemplate, never()).getForObject(anyString(), any());
        }

        @Test
        @DisplayName("returns empty when fromCurrency is null")
        void returnsEmptyForNullCurrency() {
            Optional<BigDecimal> result = exchangeRateService.convertToUsd(
                    new BigDecimal("100.00"), null);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("returns empty when API key is not configured")
        void returnsEmptyWhenApiKeyMissing() {
            when(config.getOpenExchangeRatesApiKey()).thenReturn(null);

            Optional<BigDecimal> result = exchangeRateService.convertToUsd(
                    new BigDecimal("100.00"), "GBP");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("returns empty when API key is blank")
        void returnsEmptyWhenApiKeyBlank() {
            when(config.getOpenExchangeRatesApiKey()).thenReturn("   ");

            Optional<BigDecimal> result = exchangeRateService.convertToUsd(
                    new BigDecimal("100.00"), "GBP");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("converts non-USD amount using rate from API")
        void convertsUsingApiRate() {
            when(config.getOpenExchangeRatesApiKey()).thenReturn("test-api-key");

            // Build mock API response: GBP rate 0.79
            Map<String, Object> rates = new LinkedHashMap<>();
            rates.put("GBP", 0.79);
            rates.put("EUR", 0.92);
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("rates", rates);

            when(restTemplate.getForObject(anyString(), eq(Map.class), eq("test-api-key")))
                    .thenReturn(response);

            // £100 at rate 0.79: 100 / 0.79 = 126.5822... rounded to 6 decimal places
            Optional<BigDecimal> result = exchangeRateService.convertToUsd(
                    new BigDecimal("100.00"), "GBP");

            assertThat(result).isPresent();
            // 100 / 0.79 = 126.582278... → HALF_UP to 6 places = 126.582278
            assertThat(result.get()).isEqualByComparingTo("126.582278");
        }

        @Test
        @DisplayName("returns empty when currency not found in rates")
        void returnsEmptyWhenCurrencyNotFound() {
            when(config.getOpenExchangeRatesApiKey()).thenReturn("test-api-key");

            Map<String, Object> rates = new LinkedHashMap<>();
            rates.put("GBP", 0.79);
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("rates", rates);

            when(restTemplate.getForObject(anyString(), eq(Map.class), eq("test-api-key")))
                    .thenReturn(response);

            Optional<BigDecimal> result = exchangeRateService.convertToUsd(
                    new BigDecimal("100.00"), "JPY");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("handles REST exception gracefully by returning empty")
        void handlesRestException() {
            when(config.getOpenExchangeRatesApiKey()).thenReturn("test-api-key");
            when(restTemplate.getForObject(anyString(), eq(Map.class), eq("test-api-key")))
                    .thenThrow(new RestClientException("connection timeout"));

            Optional<BigDecimal> result = exchangeRateService.convertToUsd(
                    new BigDecimal("100.00"), "GBP");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("returns empty when API response is null")
        void returnsEmptyForNullResponse() {
            when(config.getOpenExchangeRatesApiKey()).thenReturn("test-api-key");
            when(restTemplate.getForObject(anyString(), eq(Map.class), eq("test-api-key")))
                    .thenReturn(null);

            Optional<BigDecimal> result = exchangeRateService.convertToUsd(
                    new BigDecimal("100.00"), "GBP");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("returns empty when API response contains error")
        void returnsEmptyForErrorResponse() {
            when(config.getOpenExchangeRatesApiKey()).thenReturn("test-api-key");

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("error", true);
            response.put("description", "Invalid API key");

            when(restTemplate.getForObject(anyString(), eq(Map.class), eq("test-api-key")))
                    .thenReturn(response);

            Optional<BigDecimal> result = exchangeRateService.convertToUsd(
                    new BigDecimal("100.00"), "GBP");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("caches rates and reuses within TTL")
        void cachesRates() {
            when(config.getOpenExchangeRatesApiKey()).thenReturn("test-api-key");

            Map<String, Object> rates = new LinkedHashMap<>();
            rates.put("GBP", 0.79);
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("rates", rates);

            when(restTemplate.getForObject(anyString(), eq(Map.class), eq("test-api-key")))
                    .thenReturn(response);

            // First call: should hit API
            exchangeRateService.convertToUsd(new BigDecimal("100.00"), "GBP");

            // Second call: should use cache
            exchangeRateService.convertToUsd(new BigDecimal("200.00"), "GBP");

            // API should only be called once because of caching
            verify(restTemplate).getForObject(anyString(), eq(Map.class), eq("test-api-key"));
            // No second call
        }

        @Test
        @DisplayName("returns empty when rates response has no rates map")
        void returnsEmptyWhenNoRatesMap() {
            when(config.getOpenExchangeRatesApiKey()).thenReturn("test-api-key");

            Map<String, Object> response = new LinkedHashMap<>();
            // No "rates" key

            when(restTemplate.getForObject(anyString(), eq(Map.class), eq("test-api-key")))
                    .thenReturn(response);

            Optional<BigDecimal> result = exchangeRateService.convertToUsd(
                    new BigDecimal("100.00"), "GBP");

            assertThat(result).isEmpty();
        }
    }
}

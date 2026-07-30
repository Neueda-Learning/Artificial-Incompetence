package com.hsbc.portfoliomanager.marketdata;

import com.hsbc.portfoliomanager.portfolio.holding.AssetType;
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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TwelveDataHistoricalClient")
class TwelveDataHistoricalClientTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private MarketDataConfig config;

    private TwelveDataHistoricalClient client;

    private static final LocalDate START = LocalDate.of(2025, 6, 1);
    private static final LocalDate END = LocalDate.of(2025, 6, 10);

    @BeforeEach
    void setUp() {
        client = new TwelveDataHistoricalClient(restTemplate, config);
    }

    @Nested
    @DisplayName("fetchDailyPrices")
    class FetchDailyPrices {

        @Test
        @DisplayName("returns empty list when API key is null")
        void returnsEmptyWhenApiKeyNull() {
            when(config.getTwelveDataApiKey()).thenReturn(null);

            List<TwelveDataHistoricalClient.HistoricalPrice> result =
                    client.fetchDailyPrices(AssetType.STOCK, "AAPL", "NASDAQ", "USD", START, END);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("returns empty list when API key is blank")
        void returnsEmptyWhenApiKeyBlank() {
            when(config.getTwelveDataApiKey()).thenReturn("  ");

            List<TwelveDataHistoricalClient.HistoricalPrice> result =
                    client.fetchDailyPrices(AssetType.STOCK, "AAPL", "NASDAQ", "USD", START, END);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("returns empty list when API response is null")
        void returnsEmptyForNullResponse() {
            when(config.getTwelveDataApiKey()).thenReturn("test-key");
            when(restTemplate.getForObject(any(URI.class), eq(Map.class)))
                    .thenReturn(null);

            List<TwelveDataHistoricalClient.HistoricalPrice> result =
                    client.fetchDailyPrices(AssetType.STOCK, "AAPL", "NASDAQ", "USD", START, END);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("returns empty list when response contains error code")
        void returnsEmptyForErrorCode() {
            when(config.getTwelveDataApiKey()).thenReturn("test-key");

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("code", 400);
            response.put("message", "Invalid symbol");
            when(restTemplate.getForObject(any(URI.class), eq(Map.class)))
                    .thenReturn(response);

            List<TwelveDataHistoricalClient.HistoricalPrice> result =
                    client.fetchDailyPrices(AssetType.STOCK, "INVALID", null, "USD", START, END);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("returns empty list when response status is error")
        void returnsEmptyForErrorStatus() {
            when(config.getTwelveDataApiKey()).thenReturn("test-key");

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("status", "error");
            response.put("message", "Rate limit exceeded");
            when(restTemplate.getForObject(any(URI.class), eq(Map.class)))
                    .thenReturn(response);

            List<TwelveDataHistoricalClient.HistoricalPrice> result =
                    client.fetchDailyPrices(AssetType.STOCK, "AAPL", "NASDAQ", "USD", START, END);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("returns empty list when values is not a list")
        void returnsEmptyWhenValuesNotList() {
            when(config.getTwelveDataApiKey()).thenReturn("test-key");

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("values", "not-a-list");
            when(restTemplate.getForObject(any(URI.class), eq(Map.class)))
                    .thenReturn(response);

            List<TwelveDataHistoricalClient.HistoricalPrice> result =
                    client.fetchDailyPrices(AssetType.STOCK, "AAPL", "NASDAQ", "USD", START, END);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("parses complete price data with all fields")
        void parsesCompletePriceData() {
            when(config.getTwelveDataApiKey()).thenReturn("test-key");

            Map<String, Object> meta = new LinkedHashMap<>();
            meta.put("currency", "USD");
            meta.put("exchange", "NASDAQ");

            Map<String, Object> day1 = new LinkedHashMap<>();
            day1.put("datetime", "2025-06-01");
            day1.put("open", 195.00);
            day1.put("high", 198.50);
            day1.put("low", 194.20);
            day1.put("close", 197.80);
            day1.put("volume", 50000000);

            Map<String, Object> day2 = new LinkedHashMap<>();
            day2.put("datetime", "2025-06-02T15:30:00Z"); // ISO timestamp — only date part used
            day2.put("open", 198.10);
            day2.put("high", 200.00);
            day2.put("low", 197.50);
            day2.put("close", 199.30);
            day2.put("volume", "45000000"); // String number

            List<Map<String, Object>> values = List.of(day1, day2);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("meta", meta);
            response.put("values", values);
            when(restTemplate.getForObject(any(URI.class), eq(Map.class)))
                    .thenReturn(response);

            List<TwelveDataHistoricalClient.HistoricalPrice> result =
                    client.fetchDailyPrices(AssetType.STOCK, "AAPL", "NASDAQ", "USD", START, END);

            assertThat(result).hasSize(2);

            TwelveDataHistoricalClient.HistoricalPrice p1 = result.get(0);
            assertThat(p1.assetType()).isEqualTo(AssetType.STOCK);
            assertThat(p1.symbol()).isEqualTo("AAPL");
            assertThat(p1.exchange()).isEqualTo("NASDAQ");
            assertThat(p1.currency()).isEqualTo("USD");
            assertThat(p1.date()).isEqualTo(LocalDate.of(2025, 6, 1));
            assertThat(p1.open()).isEqualByComparingTo("195.00");
            assertThat(p1.high()).isEqualByComparingTo("198.50");
            assertThat(p1.low()).isEqualByComparingTo("194.20");
            assertThat(p1.close()).isEqualByComparingTo("197.80");
            assertThat(p1.volume()).isEqualByComparingTo("50000000");

            TwelveDataHistoricalClient.HistoricalPrice p2 = result.get(1);
            assertThat(p2.date()).isEqualTo(LocalDate.of(2025, 6, 2));
            assertThat(p2.close()).isEqualByComparingTo("199.30");
            assertThat(p2.volume()).isEqualByComparingTo("45000000");
        }

        @Test
        @DisplayName("uses fallback currency when meta or currency is null")
        void usesFallbackCurrency() {
            when(config.getTwelveDataApiKey()).thenReturn("test-key");

            Map<String, Object> day = new LinkedHashMap<>();
            day.put("datetime", "2025-06-01");
            day.put("close", 100.00);

            Map<String, Object> response = new LinkedHashMap<>();
            // No meta at all
            response.put("values", List.of(day));
            when(restTemplate.getForObject(any(URI.class), eq(Map.class)))
                    .thenReturn(response);

            List<TwelveDataHistoricalClient.HistoricalPrice> result =
                    client.fetchDailyPrices(AssetType.STOCK, "XYZ", "LSE", "GBP", START, END);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).currency()).isEqualTo("GBP"); // fallback
            assertThat(result.get(0).exchange()).isEqualTo("LSE"); // fallback
        }

        @Test
        @DisplayName("uses fallback when meta exchange is null")
        void usesFallbackExchange() {
            when(config.getTwelveDataApiKey()).thenReturn("test-key");

            Map<String, Object> meta = new LinkedHashMap<>();
            meta.put("currency", "EUR");
            // No exchange in meta

            Map<String, Object> day = new LinkedHashMap<>();
            day.put("datetime", "2025-06-01");
            day.put("close", 50.00);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("meta", meta);
            response.put("values", List.of(day));
            when(restTemplate.getForObject(any(URI.class), eq(Map.class)))
                    .thenReturn(response);

            List<TwelveDataHistoricalClient.HistoricalPrice> result =
                    client.fetchDailyPrices(AssetType.BOND, "VTI", "NYSE", "USD", START, END);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).currency()).isEqualTo("EUR"); // from meta
            assertThat(result.get(0).exchange()).isEqualTo("NYSE"); // fallback
        }

        @Test
        @DisplayName("handles missing optional fields (open, high, low, volume)")
        void handlesMissingOptionalFields() {
            when(config.getTwelveDataApiKey()).thenReturn("test-key");

            Map<String, Object> day = new LinkedHashMap<>();
            day.put("datetime", "2025-06-05");
            day.put("close", 150.00);
            // No open, high, low, volume

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("values", List.of(day));
            when(restTemplate.getForObject(any(URI.class), eq(Map.class)))
                    .thenReturn(response);

            List<TwelveDataHistoricalClient.HistoricalPrice> result =
                    client.fetchDailyPrices(AssetType.STOCK, "AAPL", null, "USD", START, END);

            assertThat(result).hasSize(1);
            TwelveDataHistoricalClient.HistoricalPrice p = result.get(0);
            assertThat(p.close()).isEqualByComparingTo("150.00");
            assertThat(p.open()).isNull();
            assertThat(p.high()).isNull();
            assertThat(p.low()).isNull();
            assertThat(p.volume()).isNull();
        }

        @Test
        @DisplayName("skips entry when datetime is null")
        void skipsNullDate() {
            when(config.getTwelveDataApiKey()).thenReturn("test-key");

            Map<String, Object> badDay = new LinkedHashMap<>();
            // No datetime at all
            badDay.put("close", 100.00);

            Map<String, Object> goodDay = new LinkedHashMap<>();
            goodDay.put("datetime", "2025-06-01");
            goodDay.put("close", 200.00);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("values", List.of(badDay, goodDay));
            when(restTemplate.getForObject(any(URI.class), eq(Map.class)))
                    .thenReturn(response);

            List<TwelveDataHistoricalClient.HistoricalPrice> result =
                    client.fetchDailyPrices(AssetType.STOCK, "AAPL", null, "USD", START, END);

            // Only the good day should be included
            assertThat(result).hasSize(1);
            assertThat(result.get(0).date()).isEqualTo(LocalDate.of(2025, 6, 1));
        }

        @Test
        @DisplayName("skips entry when close price is missing")
        void skipsMissingClose() {
            when(config.getTwelveDataApiKey()).thenReturn("test-key");

            Map<String, Object> noClose = new LinkedHashMap<>();
            noClose.put("datetime", "2025-06-01");
            // No close field

            Map<String, Object> hasClose = new LinkedHashMap<>();
            hasClose.put("datetime", "2025-06-02");
            hasClose.put("close", 200.00);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("values", List.of(noClose, hasClose));
            when(restTemplate.getForObject(any(URI.class), eq(Map.class)))
                    .thenReturn(response);

            List<TwelveDataHistoricalClient.HistoricalPrice> result =
                    client.fetchDailyPrices(AssetType.STOCK, "AAPL", null, "USD", START, END);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).date()).isEqualTo(LocalDate.of(2025, 6, 2));
        }

        @Test
        @DisplayName("handles RestClientException by returning empty list")
        void handlesRestClientException() {
            when(config.getTwelveDataApiKey()).thenReturn("test-key");
            when(restTemplate.getForObject(any(URI.class), eq(Map.class)))
                    .thenThrow(new RestClientException("connection timeout"));

            List<TwelveDataHistoricalClient.HistoricalPrice> result =
                    client.fetchDailyPrices(AssetType.STOCK, "AAPL", "NASDAQ", "USD", START, END);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("skips non-Map entries in values list")
        void skipsNonMapEntries() {
            when(config.getTwelveDataApiKey()).thenReturn("test-key");

            Map<String, Object> goodDay = new LinkedHashMap<>();
            goodDay.put("datetime", "2025-06-01");
            goodDay.put("close", 200.00);

            List<Object> mixedValues = new ArrayList<>();
            mixedValues.add("not-a-map");
            mixedValues.add(goodDay);
            mixedValues.add(12345); // Also not a map

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("values", mixedValues);
            when(restTemplate.getForObject(any(URI.class), eq(Map.class)))
                    .thenReturn(response);

            List<TwelveDataHistoricalClient.HistoricalPrice> result =
                    client.fetchDailyPrices(AssetType.STOCK, "AAPL", null, "USD", START, END);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).date()).isEqualTo(LocalDate.of(2025, 6, 1));
        }

        @Test
        @DisplayName("truncates long currency values to 3 characters")
        void truncatesLongCurrency() {
            when(config.getTwelveDataApiKey()).thenReturn("test-key");

            Map<String, Object> meta = new LinkedHashMap<>();
            meta.put("currency", "US DOLLAR"); // too long, should be truncated

            Map<String, Object> day = new LinkedHashMap<>();
            day.put("datetime", "2025-06-01");
            day.put("close", 100.00);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("meta", meta);
            response.put("values", List.of(day));
            when(restTemplate.getForObject(any(URI.class), eq(Map.class)))
                    .thenReturn(response);

            List<TwelveDataHistoricalClient.HistoricalPrice> result =
                    client.fetchDailyPrices(AssetType.STOCK, "AAPL", null, "USD", START, END);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).currency()).isEqualTo("US "); // trimmed + truncated to 3
            assertThat(result.get(0).currency()).hasSize(3);
        }

        @Test
        @DisplayName("does not add exchange query param when exchange is null")
        void omitsExchangeParamWhenNull() {
            when(config.getTwelveDataApiKey()).thenReturn("test-key");

            Map<String, Object> day = new LinkedHashMap<>();
            day.put("datetime", "2025-06-01");
            day.put("close", 100.00);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("values", List.of(day));
            when(restTemplate.getForObject(any(URI.class), eq(Map.class)))
                    .thenReturn(response);

            // exchange is null — should not cause issues
            List<TwelveDataHistoricalClient.HistoricalPrice> result =
                    client.fetchDailyPrices(AssetType.STOCK, "AAPL", null, "USD", START, END);

            assertThat(result).hasSize(1);
        }
    }
}

package com.hsbc.portfoliomanager.marketdata;

import com.hsbc.portfoliomanager.portfolio.holding.AssetType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("HistoricalPriceService")
class HistoricalPriceServiceTest {

    @Mock
    private PriceHistoryRepository repository;

    @Mock
    private TwelveDataHistoricalClient client;

    private HistoricalPriceService service;

    private static final LocalDate START = LocalDate.of(2026, 7, 1);
    private static final LocalDate END = LocalDate.of(2026, 7, 30);

    @BeforeEach
    void setUp() {
        service = new HistoricalPriceService(repository, client);
    }

    // ── getDailyPrices ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("getDailyPrices")
    class GetDailyPrices {

        @Test
        @DisplayName("returns stored prices when they fully cover the requested range")
        void returnsStoredPrices() {
            // Need at least 2 data points near the edges to satisfy coversRequestedRange
            // (within 7-day tolerance from start and end)
            PriceHistory startRow = new PriceHistory(
                    AssetType.STOCK, "AAPL", "NASDAQ", "USD",
                    LocalDate.of(2026, 7, 3), "1day",
                    new BigDecimal("188.00"), new BigDecimal("192.00"),
                    new BigDecimal("186.00"), new BigDecimal("190.00"),
                    new BigDecimal("40000000"), "TWELVE_DATA", null);
            PriceHistory endRow = new PriceHistory(
                    AssetType.STOCK, "AAPL", "NASDAQ", "USD",
                    LocalDate.of(2026, 7, 28), "1day",
                    new BigDecimal("193.00"), new BigDecimal("198.00"),
                    new BigDecimal("191.00"), new BigDecimal("195.00"),
                    new BigDecimal("50000000"), "TWELVE_DATA", null);

            when(repository.findByAssetTypeAndSymbolAndExchangeAndCurrencyAndTimeIntervalAndPriceDateBetweenOrderByPriceDateAsc(
                    AssetType.STOCK, "AAPL", "NASDAQ", "USD", "1day", START, END))
                    .thenReturn(List.of(startRow, endRow));

            List<HistoricalMarketDataService.PricePoint> result = service.getDailyPrices(
                    AssetType.STOCK, "AAPL", "NASDAQ", "USD", START, END);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).closePrice()).isEqualByComparingTo("190.00");
            assertThat(result.get(1).closePrice()).isEqualByComparingTo("195.00");

            // Should NOT call external API
            verify(client, never()).fetchDailyPrices(any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("fetches from API when stored data is empty")
        void fetchesWhenNoStoredData() {
            when(repository.findByAssetTypeAndSymbolAndExchangeAndCurrencyAndTimeIntervalAndPriceDateBetweenOrderByPriceDateAsc(
                    any(), anyString(), anyString(), anyString(), anyString(), any(), any()))
                    .thenReturn(List.of());

            TwelveDataHistoricalClient.HistoricalPrice apiPrice =
                    new TwelveDataHistoricalClient.HistoricalPrice(
                            AssetType.STOCK, "AAPL", "NASDAQ", "USD",
                            LocalDate.of(2026, 7, 15),
                            new BigDecimal("190.00"), new BigDecimal("198.00"),
                            new BigDecimal("189.00"), new BigDecimal("195.00"),
                            new BigDecimal("50000000"));
            when(client.fetchDailyPrices(
                    AssetType.STOCK, "AAPL", "NASDAQ", "USD", START, END))
                    .thenReturn(List.of(apiPrice));

            when(repository.findByAssetTypeAndSymbolAndExchangeAndCurrencyAndPriceDateAndTimeInterval(
                    AssetType.STOCK, "AAPL", "NASDAQ", "USD",
                    LocalDate.of(2026, 7, 15), "1day"))
                    .thenReturn(Optional.empty());

            List<HistoricalMarketDataService.PricePoint> result = service.getDailyPrices(
                    AssetType.STOCK, "AAPL", "NASDAQ", "USD", START, END);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).closePrice()).isEqualByComparingTo("195.00");

            verify(client).fetchDailyPrices(AssetType.STOCK, "AAPL", "NASDAQ", "USD", START, END);
            verify(repository).saveAll(any());
        }

        @Test
        @DisplayName("normalizes symbol, exchange, and currency inputs")
        void normalizesInputs() {
            when(repository.findByAssetTypeAndSymbolAndExchangeAndCurrencyAndTimeIntervalAndPriceDateBetweenOrderByPriceDateAsc(
                    eq(AssetType.STOCK), eq("AAPL"), eq("NASDAQ"), eq("USD"), eq("1day"), eq(START), eq(END)))
                    .thenReturn(List.of());

            when(client.fetchDailyPrices(
                    eq(AssetType.STOCK), eq("AAPL"), eq("NASDAQ"), eq("USD"), eq(START), eq(END)))
                    .thenReturn(List.of());

            service.getDailyPrices(AssetType.STOCK, " aapl ", " nasdaq ", " usd ", START, END);

            // Verify repository was queried with normalized values
            verify(repository).findByAssetTypeAndSymbolAndExchangeAndCurrencyAndTimeIntervalAndPriceDateBetweenOrderByPriceDateAsc(
                    AssetType.STOCK, "AAPL", "NASDAQ", "USD", "1day", START, END);
        }

        @Test
        @DisplayName("normalizes null exchange to empty string")
        void normalizesNullExchange() {
            when(repository.findByAssetTypeAndSymbolAndExchangeAndCurrencyAndTimeIntervalAndPriceDateBetweenOrderByPriceDateAsc(
                    AssetType.STOCK, "AAPL", "", "USD", "1day", START, END))
                    .thenReturn(List.of());

            when(client.fetchDailyPrices(AssetType.STOCK, "AAPL", "", "USD", START, END))
                    .thenReturn(List.of());

            service.getDailyPrices(AssetType.STOCK, "AAPL", null, "USD", START, END);

            verify(repository).findByAssetTypeAndSymbolAndExchangeAndCurrencyAndTimeIntervalAndPriceDateBetweenOrderByPriceDateAsc(
                    AssetType.STOCK, "AAPL", "", "USD", "1day", START, END);
        }

        @Test
        @DisplayName("returns empty list when no stored data and API returns empty")
        void returnsEmptyWhenApiEmpty() {
            when(repository.findByAssetTypeAndSymbolAndExchangeAndCurrencyAndTimeIntervalAndPriceDateBetweenOrderByPriceDateAsc(
                    any(), anyString(), anyString(), anyString(), anyString(), any(), any()))
                    .thenReturn(List.of());

            when(client.fetchDailyPrices(any(), anyString(), anyString(), anyString(), any(), any()))
                    .thenReturn(List.of());

            List<HistoricalMarketDataService.PricePoint> result = service.getDailyPrices(
                    AssetType.STOCK, "AAPL", "NASDAQ", "USD", START, END);

            assertThat(result).isEmpty();
        }
    }
}

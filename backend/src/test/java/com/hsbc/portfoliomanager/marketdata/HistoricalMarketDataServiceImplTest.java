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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("HistoricalMarketDataServiceImpl")
class HistoricalMarketDataServiceImplTest {

    @Mock
    private HistoricalPriceService historicalPriceService;

    @Mock
    private HistoricalExchangeRateService historicalExchangeRateService;

    private HistoricalMarketDataServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new HistoricalMarketDataServiceImpl(historicalPriceService, historicalExchangeRateService);
    }

    // ── getDailyPrices ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("getDailyPrices")
    class GetDailyPrices {

        @Test
        @DisplayName("delegates to HistoricalPriceService with all parameters")
        void delegatesToHistoricalPriceService() {
            LocalDate start = LocalDate.of(2026, 7, 1);
            LocalDate end = LocalDate.of(2026, 7, 30);

            List<HistoricalMarketDataService.PricePoint> expected = List.of(
                    new HistoricalMarketDataService.PricePoint("AAPL", "USD", LocalDate.of(2026, 7, 15),
                            new BigDecimal("195.00"))
            );

            when(historicalPriceService.getDailyPrices(
                    AssetType.STOCK, "AAPL", "NASDAQ", "USD", start, end))
                    .thenReturn(expected);

            List<HistoricalMarketDataService.PricePoint> result = service.getDailyPrices(
                    AssetType.STOCK, "AAPL", "NASDAQ", "USD", start, end);

            assertThat(result).isSameAs(expected);
            verify(historicalPriceService).getDailyPrices(
                    AssetType.STOCK, "AAPL", "NASDAQ", "USD", start, end);
        }

        @Test
        @DisplayName("returns empty list when historical price service returns empty")
        void returnsEmptyWhenNoPrices() {
            when(historicalPriceService.getDailyPrices(
                    any(), any(), any(), any(), any(), any()))
                    .thenReturn(List.of());

            List<HistoricalMarketDataService.PricePoint> result = service.getDailyPrices(
                    AssetType.STOCK, "MSFT", "NASDAQ", "USD",
                    LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 30));

            assertThat(result).isEmpty();
        }
    }

    // ── getRateToUsd ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getRateToUsd")
    class GetRateToUsd {

        @Test
        @DisplayName("delegates to HistoricalExchangeRateService")
        void delegatesToHistoricalExchangeRateService() {
            LocalDate date = LocalDate.of(2026, 7, 15);
            BigDecimal expected = new BigDecimal("1.2658");

            when(historicalExchangeRateService.getRateToUsd("GBP", date))
                    .thenReturn(Optional.of(expected));

            Optional<BigDecimal> result = service.getRateToUsd("GBP", date);

            assertThat(result).isPresent();
            assertThat(result.get()).isEqualByComparingTo("1.2658");
            verify(historicalExchangeRateService).getRateToUsd("GBP", date);
        }

        @Test
        @DisplayName("returns empty when rate unavailable")
        void returnsEmptyWhenRateUnavailable() {
            when(historicalExchangeRateService.getRateToUsd(eq("XYZ"), any(LocalDate.class)))
                    .thenReturn(Optional.empty());

            Optional<BigDecimal> result = service.getRateToUsd("XYZ", LocalDate.of(2026, 7, 15));

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("returns BigDecimal.ONE for USD (via delegation)")
        void returnsOneForUsd() {
            LocalDate date = LocalDate.of(2026, 7, 15);
            when(historicalExchangeRateService.getRateToUsd("USD", date))
                    .thenReturn(Optional.of(BigDecimal.ONE));

            Optional<BigDecimal> result = service.getRateToUsd("USD", date);

            assertThat(result).isPresent();
            assertThat(result.get()).isEqualByComparingTo("1");
        }
    }
}

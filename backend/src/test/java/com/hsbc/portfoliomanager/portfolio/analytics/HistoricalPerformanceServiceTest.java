package com.hsbc.portfoliomanager.portfolio.analytics;

import com.hsbc.portfoliomanager.marketdata.HistoricalMarketDataService;
import com.hsbc.portfoliomanager.marketdata.HistoricalMarketDataService.PricePoint;
import com.hsbc.portfoliomanager.portfolio.holding.AssetType;
import com.hsbc.portfoliomanager.portfolio.holding.PortfolioItem;
import com.hsbc.portfoliomanager.portfolio.holding.PortfolioItemRepository;
import com.hsbc.portfoliomanager.portfolio.transaction.TransactionRecord;
import com.hsbc.portfoliomanager.portfolio.transaction.TransactionRepository;
import com.hsbc.portfoliomanager.portfolio.transaction.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("HistoricalPerformanceService")
class HistoricalPerformanceServiceTest {

    @Mock
    private PortfolioItemRepository portfolioItemRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private HistoricalMarketDataService historicalMarketDataService;

    private HistoricalPerformanceService service;

    @BeforeEach
    void setUp() {
        service = new HistoricalPerformanceService(
                portfolioItemRepository,
                transactionRepository,
                historicalMarketDataService
        );
    }

    @Test
    @DisplayName("calculates daily performance from transaction cost and cached market history")
    void calculatesDailyPerformance() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        TransactionRecord buy = new TransactionRecord(
                TransactionType.BUY,
                AssetType.STOCK,
                "MSFT",
                new BigDecimal("5"),
                new BigDecimal("300"),
                "USD",
                today.minusDays(3).atStartOfDay().toInstant(ZoneOffset.UTC)
        );
        PortfolioItem holding = new PortfolioItem(
                AssetType.STOCK,
                "MSFT",
                "Microsoft Corp.",
                "NASDAQ",
                new BigDecimal("5"),
                "USD"
        );

        when(transactionRepository.findAllByOrderByTransactedAtAsc()).thenReturn(List.of(buy));
        when(portfolioItemRepository.findAll()).thenReturn(List.of(holding));
        when(historicalMarketDataService.getDailyPrices(
                eq(AssetType.STOCK),
                eq("MSFT"),
                eq("NASDAQ"),
                eq("USD"),
                any(LocalDate.class),
                any(LocalDate.class)
        )).thenReturn(List.of(
                new PricePoint("MSFT", "USD", today.minusDays(2), new BigDecimal("320")),
                new PricePoint("MSFT", "USD", today.minusDays(1), new BigDecimal("330"))
        ));
        when(historicalMarketDataService.getRateToUsd(eq("USD"), any(LocalDate.class)))
                .thenReturn(Optional.of(BigDecimal.ONE));

        HistoricalPerformanceResponse response = service.calculate("1W");

        assertThat(response.status()).isEqualTo("COMPLETE");
        assertThat(response.points()).hasSize(2);
        assertThat(response.points().get(0).marketValue()).isEqualByComparingTo("1600");
        assertThat(response.points().get(0).costBasis()).isEqualByComparingTo("1500");
        assertThat(response.points().get(0).profitLoss()).isEqualByComparingTo("100");
        assertThat(response.points().get(1).marketValue()).isEqualByComparingTo("1650");
        assertThat(response.points().get(1).returnPercentage()).isEqualByComparingTo("10.0000");
        assertThat(response.assets()).hasSize(1);
        assertThat(response.assets().get(0).symbol()).isEqualTo("MSFT");
        assertThat(response.assets().get(0).points()).hasSize(2);
        assertThat(response.assets().get(0).points().get(0).marketValue())
                .isEqualByComparingTo("1600");
    }

    @Test
    @DisplayName("returns separate historical performance series for every stock")
    void calculatesSeparateSeriesForEveryStock() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        TransactionRecord appleBuy = new TransactionRecord(
                TransactionType.BUY,
                AssetType.STOCK,
                "AAPL",
                new BigDecimal("2"),
                new BigDecimal("180"),
                "USD",
                today.minusDays(3).atStartOfDay().toInstant(ZoneOffset.UTC)
        );
        TransactionRecord microsoftBuy = new TransactionRecord(
                TransactionType.BUY,
                AssetType.STOCK,
                "MSFT",
                new BigDecimal("3"),
                new BigDecimal("300"),
                "USD",
                today.minusDays(3).atStartOfDay().toInstant(ZoneOffset.UTC)
        );

        when(transactionRepository.findAllByOrderByTransactedAtAsc())
                .thenReturn(List.of(appleBuy, microsoftBuy));
        when(portfolioItemRepository.findAll()).thenReturn(List.of(
                new PortfolioItem(
                        AssetType.STOCK,
                        "AAPL",
                        "Apple Inc.",
                        "NASDAQ",
                        new BigDecimal("2"),
                        "USD"
                ),
                new PortfolioItem(
                        AssetType.STOCK,
                        "MSFT",
                        "Microsoft Corp.",
                        "NASDAQ",
                        new BigDecimal("3"),
                        "USD"
                )
        ));
        when(historicalMarketDataService.getDailyPrices(
                eq(AssetType.STOCK),
                eq("AAPL"),
                eq("NASDAQ"),
                eq("USD"),
                any(LocalDate.class),
                any(LocalDate.class)
        )).thenReturn(List.of(
                new PricePoint("AAPL", "USD", today.minusDays(1), new BigDecimal("200"))
        ));
        when(historicalMarketDataService.getDailyPrices(
                eq(AssetType.STOCK),
                eq("MSFT"),
                eq("NASDAQ"),
                eq("USD"),
                any(LocalDate.class),
                any(LocalDate.class)
        )).thenReturn(List.of(
                new PricePoint("MSFT", "USD", today.minusDays(1), new BigDecimal("320"))
        ));
        when(historicalMarketDataService.getRateToUsd(eq("USD"), any(LocalDate.class)))
                .thenReturn(Optional.of(BigDecimal.ONE));

        HistoricalPerformanceResponse response = service.calculate("1W");

        assertThat(response.points()).hasSize(1);
        assertThat(response.points().get(0).marketValue()).isEqualByComparingTo("1360");
        assertThat(response.assets()).extracting(HistoricalPerformanceResponse.AssetSeries::symbol)
                .containsExactly("AAPL", "MSFT");
        assertThat(response.assets().get(0).points().get(0).marketValue())
                .isEqualByComparingTo("400");
        assertThat(response.assets().get(1).points().get(0).marketValue())
                .isEqualByComparingTo("960");
    }

    @Test
    @DisplayName("returns unavailable without transaction history")
    void unavailableWithoutTransactions() {
        when(transactionRepository.findAllByOrderByTransactedAtAsc()).thenReturn(List.of());

        HistoricalPerformanceResponse response = service.calculate("1M");

        assertThat(response.status()).isEqualTo("UNAVAILABLE");
        assertThat(response.points()).isEmpty();
        assertThat(response.assets()).isEmpty();
        assertThat(response.missingData()).containsExactly("TRANSACTION_HISTORY");
        verify(historicalMarketDataService, never()).getDailyPrices(
                any(), any(), any(), any(), any(), any()
        );
    }

    @Test
    @DisplayName("reports the purchase date when all available prices predate the position")
    void reportsMissingPriceOnPurchaseDate() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        TransactionRecord buy = new TransactionRecord(
                TransactionType.BUY,
                AssetType.STOCK,
                "MSFT",
                new BigDecimal("10"),
                new BigDecimal("400"),
                "USD",
                today.atStartOfDay().toInstant(ZoneOffset.UTC)
        );
        PortfolioItem holding = new PortfolioItem(
                AssetType.STOCK,
                "MSFT",
                "Microsoft Corp.",
                "NASDAQ",
                new BigDecimal("10"),
                "USD"
        );

        when(transactionRepository.findAllByOrderByTransactedAtAsc()).thenReturn(List.of(buy));
        when(portfolioItemRepository.findAll()).thenReturn(List.of(holding));
        when(historicalMarketDataService.getDailyPrices(
                eq(AssetType.STOCK),
                eq("MSFT"),
                eq("NASDAQ"),
                eq("USD"),
                any(LocalDate.class),
                any(LocalDate.class)
        )).thenReturn(List.of(
                new PricePoint("MSFT", "USD", today.minusDays(1), new BigDecimal("389.10"))
        ));

        HistoricalPerformanceResponse response = service.calculate("1M");

        assertThat(response.status()).isEqualTo("UNAVAILABLE");
        assertThat(response.points()).isEmpty();
        assertThat(response.missingData()).containsExactly("MSFT:" + today);
    }
}

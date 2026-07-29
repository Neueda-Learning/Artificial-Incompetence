package com.hsbc.portfoliomanager.portfolio.analytics;

import com.hsbc.portfoliomanager.marketdata.MarketDataService;
import com.hsbc.portfoliomanager.marketdata.MarketDataService.PriceData;
import com.hsbc.portfoliomanager.portfolio.holding.AssetType;
import com.hsbc.portfoliomanager.portfolio.holding.PortfolioItem;
import com.hsbc.portfoliomanager.portfolio.holding.PortfolioItemRepository;
import com.hsbc.portfoliomanager.portfolio.transaction.TransactionRecord;
import com.hsbc.portfoliomanager.portfolio.transaction.TransactionRepository;
import com.hsbc.portfoliomanager.portfolio.transaction.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AnalyticsService")
class AnalyticsServiceTest {

    @Mock
    private PortfolioItemRepository portfolioItemRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private MarketDataService marketDataService;

    private AnalyticsService analyticsService;

    private static final Instant NOW = Instant.now().truncatedTo(ChronoUnit.SECONDS);

    @BeforeEach
    void setUp() {
        analyticsService = new AnalyticsService(
                portfolioItemRepository, transactionRepository, marketDataService);
    }

    // ── US-06: Portfolio Value ────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/portfolio/value — calculateCurrentValue")
    class CurrentValue {

        @Test
        @DisplayName("returns COMPLETE with market values when all prices available")
        void allPricesAvailable() {
            PortfolioItem aapl = new PortfolioItem(AssetType.STOCK, "AAPL", new BigDecimal("10"));
            PortfolioItem tsla = new PortfolioItem(AssetType.STOCK, "TSLA", new BigDecimal("5"));

            when(portfolioItemRepository.findAll()).thenReturn(List.of(aapl, tsla));
            when(marketDataService.getCurrentPrice("AAPL"))
                    .thenReturn(Optional.of(new PriceData("AAPL", new BigDecimal("195.00"), "USD", NOW)));
            when(marketDataService.getCurrentPrice("TSLA"))
                    .thenReturn(Optional.of(new PriceData("TSLA", new BigDecimal("250.00"), "USD", NOW)));

            PortfolioValueResponse response = analyticsService.calculateCurrentValue();

            assertThat(response.status()).isEqualTo("COMPLETE");
            assertThat(response.currency()).isEqualTo("USD");
            assertThat(response.missingPrices()).isEmpty();
            assertThat(response.assets()).hasSize(2);

            // AAPL: 10 × 195 = 1950
            PortfolioValueResponse.AssetValue aaplValue = response.assets().get(0);
            assertThat(aaplValue.marketValue()).isEqualByComparingTo("1950.00");

            // TSLA: 5 × 250 = 1250
            PortfolioValueResponse.AssetValue tslaValue = response.assets().get(1);
            assertThat(tslaValue.marketValue()).isEqualByComparingTo("1250.00");
        }

        @Test
        @DisplayName("returns PARTIAL when some prices are missing")
        void somePricesMissing() {
            PortfolioItem aapl = new PortfolioItem(AssetType.STOCK, "AAPL", new BigDecimal("10"));
            PortfolioItem msft = new PortfolioItem(AssetType.STOCK, "MSFT", new BigDecimal("5"));

            when(portfolioItemRepository.findAll()).thenReturn(List.of(aapl, msft));
            when(marketDataService.getCurrentPrice("AAPL"))
                    .thenReturn(Optional.of(new PriceData("AAPL", new BigDecimal("195.00"), "USD", NOW)));
            when(marketDataService.getCurrentPrice("MSFT"))
                    .thenReturn(Optional.empty());

            PortfolioValueResponse response = analyticsService.calculateCurrentValue();

            assertThat(response.status()).isEqualTo("PARTIAL");
            assertThat(response.missingPrices()).containsExactly("MSFT");
            assertThat(response.assets()).hasSize(2);

            // AAPL should have a price
            assertThat(response.assets().get(0).currentPrice()).isNotNull();
            // MSFT should have null price
            assertThat(response.assets().get(1).currentPrice()).isNull();
            assertThat(response.assets().get(1).marketValue()).isNull();
        }

        @Test
        @DisplayName("returns UNAVAILABLE when all prices are missing")
        void allPricesMissing() {
            PortfolioItem aapl = new PortfolioItem(AssetType.STOCK, "AAPL", new BigDecimal("10"));

            when(portfolioItemRepository.findAll()).thenReturn(List.of(aapl));
            when(marketDataService.getCurrentPrice(anyString())).thenReturn(Optional.empty());

            PortfolioValueResponse response = analyticsService.calculateCurrentValue();

            assertThat(response.status()).isEqualTo("UNAVAILABLE");
            assertThat(response.missingPrices()).containsExactly("AAPL");
        }

        @Test
        @DisplayName("returns COMPLETE with empty list when no holdings")
        void emptyPortfolio() {
            when(portfolioItemRepository.findAll()).thenReturn(List.of());

            PortfolioValueResponse response = analyticsService.calculateCurrentValue();

            assertThat(response.status()).isEqualTo("COMPLETE");
            assertThat(response.assets()).isEmpty();
            assertThat(response.missingPrices()).isEmpty();
        }

        @Test
        @DisplayName("converts non-USD price to USD using exchange rate")
        void currencyConversion() {
            PortfolioItem vod = new PortfolioItem(AssetType.STOCK, "VOD", new BigDecimal("100"));

            when(portfolioItemRepository.findAll()).thenReturn(List.of(vod));
            // Price in GBP
            when(marketDataService.getCurrentPrice("VOD"))
                    .thenReturn(Optional.of(new PriceData("VOD", new BigDecimal("0.79"), "GBP", NOW)));
            // GBP rate: 0.79 → USD = 0.79 / 0.79 = 1.00? No...
            // Actually, open exchange rates base is USD. If GBP rate is 0.79,
            // $1 = £0.79, so £1 = $1/0.79 = $1.2658
            // £0.79 → 0.79 / 0.79 = $1.00
            when(marketDataService.convertToUsd(new BigDecimal("0.79"), "GBP"))
                    .thenReturn(Optional.of(new BigDecimal("1.00")));

            PortfolioValueResponse response = analyticsService.calculateCurrentValue();

            assertThat(response.status()).isEqualTo("COMPLETE");
            PortfolioValueResponse.AssetValue vodValue = response.assets().get(0);
            // 100 shares × $1.00 = $100.00
            assertThat(vodValue.marketValue()).isEqualByComparingTo("100.00");
            assertThat(vodValue.currency()).isEqualTo("USD");
        }
    }

    // ── US-07: Portfolio Performance ──────────────────────────────────────

    @Nested
    @DisplayName("GET /api/portfolio/performance — calculatePerformance")
    class Performance {

        @Test
        @DisplayName("calculates total cost, current value, P&L, and return % for a single asset")
        void singleAssetWithGain() {
            PortfolioItem aapl = new PortfolioItem(AssetType.STOCK, "AAPL", new BigDecimal("10"));

            TransactionRecord buy = new TransactionRecord(
                    TransactionType.BUY, AssetType.STOCK, "AAPL",
                    new BigDecimal("10"), new BigDecimal("180.50"), "USD", NOW.minusSeconds(86400));

            when(portfolioItemRepository.findAll()).thenReturn(List.of(aapl));
            when(transactionRepository.findByTransactionTypeOrderByTransactedAtDesc(TransactionType.BUY))
                    .thenReturn(List.of(buy));
            when(marketDataService.getCurrentPrice("AAPL"))
                    .thenReturn(Optional.of(new PriceData("AAPL", new BigDecimal("195.00"), "USD", NOW)));

            PortfolioPerformanceResponse response = analyticsService.calculatePerformance();

            assertThat(response.status()).isEqualTo("COMPLETE");
            assertThat(response.currency()).isEqualTo("USD");

            // Total cost: 10 × 180.50 = 1805.00
            assertThat(response.totalCost()).isEqualByComparingTo("1805.00");
            // Current value: 10 × 195.00 = 1950.00
            assertThat(response.currentValue()).isEqualByComparingTo("1950.00");
            // Unrealized P&L: 1950 - 1805 = 145.00
            assertThat(response.unrealizedProfitLoss()).isEqualByComparingTo("145.00");
            // Return %: 145 / 1805 × 100 = 8.0332...
            assertThat(response.returnPercentage().doubleValue())
                    .isCloseTo(8.03, within(0.01));

            // Per-asset
            assertThat(response.assets()).hasSize(1);
            PortfolioPerformanceResponse.AssetPerformance asset = response.assets().get(0);
            assertThat(asset.symbol()).isEqualTo("AAPL");
            assertThat(asset.averageCost()).isEqualByComparingTo("180.50");
            assertThat(asset.costBasis()).isEqualByComparingTo("1805.00");
            assertThat(asset.currentValue()).isEqualByComparingTo("1950.00");
            assertThat(asset.unrealizedProfitLoss()).isEqualByComparingTo("145.00");
            assertThat(asset.allocationPercentage()).isEqualByComparingTo("100.00");
            assertThat(asset.returnPercentage().doubleValue())
                    .isCloseTo(8.03, within(0.01));
        }

        @Test
        @DisplayName("calculates weighted average cost for multiple purchases of same stock")
        void weightedAverageCost() {
            PortfolioItem aapl = new PortfolioItem(AssetType.STOCK, "AAPL", new BigDecimal("15"));

            // First purchase: 10 shares @ $180 = $1800
            TransactionRecord buy1 = new TransactionRecord(
                    TransactionType.BUY, AssetType.STOCK, "AAPL",
                    new BigDecimal("10"), new BigDecimal("180.00"), "USD", NOW.minusSeconds(172800));
            // Second purchase: 5 shares @ $190 = $950
            TransactionRecord buy2 = new TransactionRecord(
                    TransactionType.BUY, AssetType.STOCK, "AAPL",
                    new BigDecimal("5"), new BigDecimal("190.00"), "USD", NOW.minusSeconds(86400));

            // Weighted avg: (1800 + 950) / 15 = 2750 / 15 = 183.333333

            when(portfolioItemRepository.findAll()).thenReturn(List.of(aapl));
            when(transactionRepository.findByTransactionTypeOrderByTransactedAtDesc(TransactionType.BUY))
                    .thenReturn(List.of(buy1, buy2));
            when(marketDataService.getCurrentPrice("AAPL"))
                    .thenReturn(Optional.of(new PriceData("AAPL", new BigDecimal("200.00"), "USD", NOW)));

            PortfolioPerformanceResponse response = analyticsService.calculatePerformance();

            // Average cost should be ~183.333333
            PortfolioPerformanceResponse.AssetPerformance asset = response.assets().get(0);
            assertThat(asset.averageCost().doubleValue()).isCloseTo(183.33, within(0.01));

            // Cost basis: 15 × 183.333... = 2750.00
            assertThat(asset.costBasis().doubleValue()).isCloseTo(2750.00, within(0.01));

            // Current value: 15 × 200 = 3000.00
            assertThat(asset.currentValue()).isEqualByComparingTo("3000.00");

            // P&L: 3000 - 2750 = 250.00
            assertThat(asset.unrealizedProfitLoss().doubleValue()).isCloseTo(250.00, within(0.01));

            // Return %: 250 / 2750 × 100 = 9.0909...
            assertThat(asset.returnPercentage().doubleValue()).isCloseTo(9.09, within(0.01));
        }

        @Test
        @DisplayName("shows unrealized loss when current price is below average cost")
        void unrealizedLoss() {
            PortfolioItem aapl = new PortfolioItem(AssetType.STOCK, "AAPL", new BigDecimal("10"));

            TransactionRecord buy = new TransactionRecord(
                    TransactionType.BUY, AssetType.STOCK, "AAPL",
                    new BigDecimal("10"), new BigDecimal("200.00"), "USD", NOW.minusSeconds(86400));

            when(portfolioItemRepository.findAll()).thenReturn(List.of(aapl));
            when(transactionRepository.findByTransactionTypeOrderByTransactedAtDesc(TransactionType.BUY))
                    .thenReturn(List.of(buy));
            when(marketDataService.getCurrentPrice("AAPL"))
                    .thenReturn(Optional.of(new PriceData("AAPL", new BigDecimal("180.00"), "USD", NOW)));

            PortfolioPerformanceResponse response = analyticsService.calculatePerformance();

            // Cost: 2000, Value: 1800, Loss: -200
            assertThat(response.totalCost()).isEqualByComparingTo("2000.00");
            assertThat(response.currentValue()).isEqualByComparingTo("1800.00");
            assertThat(response.unrealizedProfitLoss()).isEqualByComparingTo("-200.00");

            // Return should be negative
            assertThat(response.returnPercentage().doubleValue()).isCloseTo(-10.00, within(0.01));
        }

        @Test
        @DisplayName("handles empty portfolio without division by zero")
        void emptyPortfolio() {
            when(portfolioItemRepository.findAll()).thenReturn(List.of());

            PortfolioPerformanceResponse response = analyticsService.calculatePerformance();

            assertThat(response.status()).isEqualTo("COMPLETE");
            assertThat(response.totalCost()).isEqualByComparingTo("0");
            assertThat(response.currentValue()).isEqualByComparingTo("0");
            assertThat(response.unrealizedProfitLoss()).isEqualByComparingTo("0");
            assertThat(response.returnPercentage()).isEqualByComparingTo("0");
            assertThat(response.assets()).isEmpty();
            assertThat(response.missingPrices()).isEmpty();
        }

        @Test
        @DisplayName("handles holdings with no buy transactions (cost = 0)")
        void noTransactionHistory() {
            PortfolioItem aapl = new PortfolioItem(AssetType.STOCK, "AAPL", new BigDecimal("10"));

            when(portfolioItemRepository.findAll()).thenReturn(List.of(aapl));
            when(transactionRepository.findByTransactionTypeOrderByTransactedAtDesc(TransactionType.BUY))
                    .thenReturn(List.of());  // no transactions
            when(marketDataService.getCurrentPrice("AAPL"))
                    .thenReturn(Optional.of(new PriceData("AAPL", new BigDecimal("195.00"), "USD", NOW)));

            PortfolioPerformanceResponse response = analyticsService.calculatePerformance();

            // No cost basis, so return should be 0 (no division by zero)
            assertThat(response.totalCost()).isEqualByComparingTo("0");
            assertThat(response.currentValue()).isEqualByComparingTo("1950.00");
            assertThat(response.unrealizedProfitLoss()).isEqualByComparingTo("1950.00");
            assertThat(response.returnPercentage()).isEqualByComparingTo("0");

            PortfolioPerformanceResponse.AssetPerformance asset = response.assets().get(0);
            assertThat(asset.averageCost()).isNull();
            assertThat(asset.returnPercentage()).isEqualByComparingTo("0");
        }

        @Test
        @DisplayName("calculates allocation percentages for multiple assets")
        void allocationPercentages() {
            PortfolioItem aapl = new PortfolioItem(AssetType.STOCK, "AAPL", new BigDecimal("10"));
            PortfolioItem tsla = new PortfolioItem(AssetType.STOCK, "TSLA", new BigDecimal("4"));

            TransactionRecord buyAapl = new TransactionRecord(
                    TransactionType.BUY, AssetType.STOCK, "AAPL",
                    new BigDecimal("10"), new BigDecimal("180.00"), "USD", NOW);
            TransactionRecord buyTsla = new TransactionRecord(
                    TransactionType.BUY, AssetType.STOCK, "TSLA",
                    new BigDecimal("4"), new BigDecimal("250.00"), "USD", NOW);

            when(portfolioItemRepository.findAll()).thenReturn(List.of(aapl, tsla));
            when(transactionRepository.findByTransactionTypeOrderByTransactedAtDesc(TransactionType.BUY))
                    .thenReturn(List.of(buyAapl, buyTsla));
            when(marketDataService.getCurrentPrice("AAPL"))
                    .thenReturn(Optional.of(new PriceData("AAPL", new BigDecimal("200.00"), "USD", NOW)));
            when(marketDataService.getCurrentPrice("TSLA"))
                    .thenReturn(Optional.of(new PriceData("TSLA", new BigDecimal("250.00"), "USD", NOW)));

            PortfolioPerformanceResponse response = analyticsService.calculatePerformance();

            // AAPL value: 10 × 200 = 2000
            // TSLA value: 4 × 250 = 1000
            // Total value: 3000
            // AAPL allocation: 2000/3000 × 100 = 66.6667%
            // TSLA allocation: 1000/3000 × 100 = 33.3333%

            PortfolioPerformanceResponse.AssetPerformance aaplPerf = response.assets().get(0);
            PortfolioPerformanceResponse.AssetPerformance tslaPerf = response.assets().get(1);

            assertThat(aaplPerf.allocationPercentage().doubleValue()).isCloseTo(66.67, within(0.01));
            assertThat(tslaPerf.allocationPercentage().doubleValue()).isCloseTo(33.33, within(0.01));
        }

        @Test
        @DisplayName("marks status as PARTIAL when some prices are missing")
        void partialPrices() {
            PortfolioItem aapl = new PortfolioItem(AssetType.STOCK, "AAPL", new BigDecimal("10"));
            PortfolioItem msft = new PortfolioItem(AssetType.STOCK, "MSFT", new BigDecimal("5"));

            TransactionRecord buyAapl = new TransactionRecord(
                    TransactionType.BUY, AssetType.STOCK, "AAPL",
                    new BigDecimal("10"), new BigDecimal("180.00"), "USD", NOW);
            TransactionRecord buyMsft = new TransactionRecord(
                    TransactionType.BUY, AssetType.STOCK, "MSFT",
                    new BigDecimal("5"), new BigDecimal("400.00"), "USD", NOW);

            when(portfolioItemRepository.findAll()).thenReturn(List.of(aapl, msft));
            when(transactionRepository.findByTransactionTypeOrderByTransactedAtDesc(TransactionType.BUY))
                    .thenReturn(List.of(buyAapl, buyMsft));
            when(marketDataService.getCurrentPrice("AAPL"))
                    .thenReturn(Optional.of(new PriceData("AAPL", new BigDecimal("195.00"), "USD", NOW)));
            when(marketDataService.getCurrentPrice("MSFT"))
                    .thenReturn(Optional.empty());

            PortfolioPerformanceResponse response = analyticsService.calculatePerformance();

            assertThat(response.status()).isEqualTo("PARTIAL");
            assertThat(response.missingPrices()).containsExactly("MSFT");

            // MSFT asset should have null for price-dependent fields
            PortfolioPerformanceResponse.AssetPerformance msftPerf = response.assets().get(1);
            assertThat(msftPerf.currentPrice()).isNull();
            assertThat(msftPerf.currentValue()).isNull();
            assertThat(msftPerf.unrealizedProfitLoss()).isNull();
        }

        @Test
        @DisplayName("priceUpdatedAt reflects the latest market data timestamp")
        void priceUpdatedAtReflectsLatestData() {
            PortfolioItem aapl = new PortfolioItem(AssetType.STOCK, "AAPL", new BigDecimal("10"));
            PortfolioItem tsla = new PortfolioItem(AssetType.STOCK, "TSLA", new BigDecimal("5"));

            Instant earlier = NOW.minusSeconds(60);
            Instant later = NOW;

            when(portfolioItemRepository.findAll()).thenReturn(List.of(aapl, tsla));
            when(transactionRepository.findByTransactionTypeOrderByTransactedAtDesc(TransactionType.BUY))
                    .thenReturn(List.of());
            when(marketDataService.getCurrentPrice("AAPL"))
                    .thenReturn(Optional.of(new PriceData("AAPL", new BigDecimal("195.00"), "USD", earlier)));
            when(marketDataService.getCurrentPrice("TSLA"))
                    .thenReturn(Optional.of(new PriceData("TSLA", new BigDecimal("250.00"), "USD", later)));

            PortfolioPerformanceResponse response = analyticsService.calculatePerformance();

            assertThat(response.priceUpdatedAt()).isEqualTo(later);
        }
    }
}

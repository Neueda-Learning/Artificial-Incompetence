package com.hsbc.portfoliomanager.portfolio.activity;

import com.hsbc.portfoliomanager.portfolio.holding.AssetType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PortfolioActivityService")
class PortfolioActivityServiceTest {

    @Mock
    private PortfolioActivityRepository repository;

    private PortfolioActivityService activityService;

    private static final Instant NOW = Instant.now();

    @BeforeEach
    void setUp() {
        activityService = new PortfolioActivityService(repository);
    }

    // ── findAll ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("returns empty list when no activities")
        void returnsEmptyList() {
            when(repository.findAllByOrderByOccurredAtDescIdDesc()).thenReturn(List.of());

            List<PortfolioActivityResponse> result = activityService.findAll();

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("returns activities sorted newest first")
        void returnsActivitiesSorted() {
            PortfolioActivity older = new PortfolioActivity(
                    PortfolioActivityAction.ADDED, AssetType.STOCK, "AAPL",
                    new BigDecimal("10"), new BigDecimal("180.00"), "USD",
                    null, NOW.minusSeconds(3600));
            PortfolioActivity newer = new PortfolioActivity(
                    PortfolioActivityAction.ADDED, AssetType.STOCK, "TSLA",
                    new BigDecimal("4"), new BigDecimal("250.00"), "USD",
                    null, NOW);

            when(repository.findAllByOrderByOccurredAtDescIdDesc())
                    .thenReturn(List.of(newer, older));

            List<PortfolioActivityResponse> result = activityService.findAll();

            assertThat(result).hasSize(2);
            assertThat(result.get(0).symbol()).isEqualTo("TSLA");
            assertThat(result.get(1).symbol()).isEqualTo("AAPL");
        }

        @Test
        @DisplayName("maps all entity fields to response")
        void mapsFieldsCorrectly() {
            PortfolioActivity activity = new PortfolioActivity(
                    PortfolioActivityAction.ADDED, AssetType.STOCK, "MSFT",
                    new BigDecimal("5"), new BigDecimal("400.00"), "USD",
                    null, NOW);

            when(repository.findAllByOrderByOccurredAtDescIdDesc())
                    .thenReturn(List.of(activity));

            List<PortfolioActivityResponse> result = activityService.findAll();

            PortfolioActivityResponse response = result.get(0);
            assertThat(response.action()).isEqualTo(PortfolioActivityAction.ADDED);
            assertThat(response.assetType()).isEqualTo(AssetType.STOCK);
            assertThat(response.symbol()).isEqualTo("MSFT");
            assertThat(response.quantity()).isEqualByComparingTo("5");
            assertThat(response.pricePerUnit()).isEqualByComparingTo("400.00");
            assertThat(response.currency()).isEqualTo("USD");
            assertThat(response.occurredAt()).isEqualTo(NOW);
            assertThat(response.remainingQuantity()).isNull();
        }
    }

    // ── recordAdded ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("recordAdded")
    class RecordAdded {

        @Test
        @DisplayName("persists ADDED activity with all fields populated")
        void persistsAddedActivity() {
            when(repository.save(any(PortfolioActivity.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            activityService.recordAdded(
                    AssetType.STOCK, "AAPL",
                    new BigDecimal("10"), new BigDecimal("195.00"),
                    "USD", NOW);

            ArgumentCaptor<PortfolioActivity> captor =
                    ArgumentCaptor.forClass(PortfolioActivity.class);
            verify(repository).save(captor.capture());

            PortfolioActivity saved = captor.getValue();
            assertThat(saved.getAction()).isEqualTo(PortfolioActivityAction.ADDED);
            assertThat(saved.getAssetType()).isEqualTo(AssetType.STOCK);
            assertThat(saved.getSymbol()).isEqualTo("AAPL");
            assertThat(saved.getQuantity()).isEqualByComparingTo("10");
            assertThat(saved.getPricePerUnit()).isEqualByComparingTo("195.00");
            assertThat(saved.getCurrency()).isEqualTo("USD");
            assertThat(saved.getOccurredAt()).isEqualTo(NOW);
            assertThat(saved.getRemainingQuantity()).isNull();
        }

        @Test
        @DisplayName("persists ADDED activity for non-STOCK asset")
        void persistsNonStockAddedActivity() {
            when(repository.save(any(PortfolioActivity.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            activityService.recordAdded(
                    AssetType.CASH, "EUR",
                    new BigDecimal("5000"), new BigDecimal("1.08"),
                    "EUR", NOW);

            ArgumentCaptor<PortfolioActivity> captor =
                    ArgumentCaptor.forClass(PortfolioActivity.class);
            verify(repository).save(captor.capture());

            PortfolioActivity saved = captor.getValue();
            assertThat(saved.getAssetType()).isEqualTo(AssetType.CASH);
            assertThat(saved.getSymbol()).isEqualTo("EUR");
            assertThat(saved.getCurrency()).isEqualTo("EUR");
            assertThat(saved.getRemainingQuantity()).isNull();
        }
    }

    // ── recordRemoved ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("recordRemoved")
    class RecordRemoved {

        @Test
        @DisplayName("persists REMOVED activity with remaining quantity (partial removal)")
        void persistsPartialRemovalActivity() {
            activityService.recordRemoved(
                    AssetType.STOCK, "AAPL",
                    new BigDecimal("7"), "USD",
                    new BigDecimal("3"), NOW);

            ArgumentCaptor<PortfolioActivity> captor =
                    ArgumentCaptor.forClass(PortfolioActivity.class);
            verify(repository).save(captor.capture());

            PortfolioActivity saved = captor.getValue();
            assertThat(saved.getAction()).isEqualTo(PortfolioActivityAction.REMOVED);
            assertThat(saved.getAssetType()).isEqualTo(AssetType.STOCK);
            assertThat(saved.getSymbol()).isEqualTo("AAPL");
            assertThat(saved.getQuantity()).isEqualByComparingTo("7");
            assertThat(saved.getCurrency()).isEqualTo("USD");
            assertThat(saved.getOccurredAt()).isEqualTo(NOW);
            assertThat(saved.getRemainingQuantity()).isEqualByComparingTo("3");
            assertThat(saved.getPricePerUnit()).isNull(); // REMOVED has no price
        }

        @Test
        @DisplayName("persists REMOVED activity with zero remaining (full removal)")
        void persistsFullRemovalActivity() {
            activityService.recordRemoved(
                    AssetType.STOCK, "TSLA",
                    new BigDecimal("4"), "USD",
                    BigDecimal.ZERO, NOW);

            ArgumentCaptor<PortfolioActivity> captor =
                    ArgumentCaptor.forClass(PortfolioActivity.class);
            verify(repository).save(captor.capture());

            PortfolioActivity saved = captor.getValue();
            assertThat(saved.getAction()).isEqualTo(PortfolioActivityAction.REMOVED);
            assertThat(saved.getQuantity()).isEqualByComparingTo("4");
            assertThat(saved.getRemainingQuantity()).isEqualByComparingTo("0");
        }
    }
}

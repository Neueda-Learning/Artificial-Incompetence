package com.hsbc.portfoliomanager.portfolio.holding;

import com.hsbc.portfoliomanager.portfolio.activity.PortfolioActivityService;
import com.hsbc.portfoliomanager.portfolio.transaction.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("PortfolioService")
class PortfolioServiceTest {

    @Mock
    private PortfolioItemRepository repository;

    @Mock
    private AssetMetadataClient assetMetadataClient;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private PortfolioActivityService activityService;

    private PortfolioService portfolioService;

    @BeforeEach
    void setUp() {
        portfolioService = new PortfolioService(
                repository, assetMetadataClient, transactionRepository, activityService);
    }

    // Create a spy PortfolioItem with a stubbed id, for tests that call replaceQuantity()
    private static PortfolioItem itemWithId(Long id, AssetType type, String symbol,
                                             BigDecimal qty, String currency) {
        PortfolioItem item = spy(new PortfolioItem(type, symbol, qty, currency));
        when(item.getId()).thenReturn(id);
        return item;
    }

    // Full metadata version
    private static PortfolioItem itemWithMeta(Long id, AssetType type, String symbol,
                                               String company, String exchange,
                                               BigDecimal qty, String currency) {
        PortfolioItem item = spy(new PortfolioItem(type, symbol, company, exchange, qty, currency));
        when(item.getId()).thenReturn(id);
        return item;
    }

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("returns empty list when no holdings exist")
        void returnsEmptyList() {
            when(repository.findAll()).thenReturn(List.of());

            List<PortfolioItemResponse> result = portfolioService.findAll();

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("maps all holdings to response DTOs")
        void returnsAllHoldings() {
            PortfolioItem aapl = itemWithMeta(1L, AssetType.STOCK, "AAPL", "Apple Inc.", "NASDAQ",
                    new BigDecimal("10"), "USD");
            PortfolioItem tsla = itemWithMeta(2L, AssetType.STOCK, "TSLA", "Tesla Inc.", "NASDAQ",
                    new BigDecimal("4"), "USD");

            when(repository.findAll()).thenReturn(List.of(aapl, tsla));

            List<PortfolioItemResponse> result = portfolioService.findAll();

            assertThat(result).hasSize(2);
            assertThat(result.get(0).symbol()).isEqualTo("AAPL");
            assertThat(result.get(0).quantity()).isEqualByComparingTo("10");
            assertThat(result.get(1).symbol()).isEqualTo("TSLA");
            assertThat(result.get(1).companyName()).isEqualTo("Tesla Inc.");
        }
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("normalizes symbol, fetches metadata, and creates new holding")
        void createsNewHolding() {
            CreatePortfolioItemRequest request = new CreatePortfolioItemRequest(
                    AssetType.STOCK, " aapl ", new BigDecimal("10"));

            AssetMetadata metadata = new AssetMetadata("AAPL", "Apple Inc.", "NASDAQ", "USD");
            when(assetMetadataClient.findBySymbol("AAPL")).thenReturn(metadata);

            when(repository.findByAssetTypeAndSymbolAndCurrency(
                    AssetType.STOCK, "AAPL", "USD")).thenReturn(Optional.empty());

            PortfolioItem savedItem = itemWithMeta(1L, AssetType.STOCK, "AAPL", "Apple Inc.", "NASDAQ",
                    new BigDecimal("10"), "USD");
            when(repository.save(any(PortfolioItem.class))).thenReturn(savedItem);

            PortfolioItemResponse response = portfolioService.create(request);

            assertThat(response.symbol()).isEqualTo("AAPL");
            assertThat(response.companyName()).isEqualTo("Apple Inc.");
            assertThat(response.exchange()).isEqualTo("NASDAQ");
            assertThat(response.currency()).isEqualTo("USD");
            assertThat(response.quantity()).isEqualByComparingTo("10");

            verify(assetMetadataClient).findBySymbol("AAPL");
        }

        @Test
        @DisplayName("adds quantity when same asset already exists")
        void addsQuantityToExisting() {
            CreatePortfolioItemRequest request = new CreatePortfolioItemRequest(
                    AssetType.STOCK, "AAPL", new BigDecimal("5"));

            AssetMetadata metadata = new AssetMetadata("AAPL", "Apple Inc.", "NASDAQ", "USD");
            when(assetMetadataClient.findBySymbol("AAPL")).thenReturn(metadata);

            PortfolioItem existing = itemWithMeta(1L, AssetType.STOCK, "AAPL", "Apple Inc.", "NASDAQ",
                    new BigDecimal("10"), "USD");
            when(repository.findByAssetTypeAndSymbolAndCurrency(
                    AssetType.STOCK, "AAPL", "USD")).thenReturn(Optional.of(existing));

            when(repository.save(existing)).thenReturn(existing);

            PortfolioItemResponse response = portfolioService.create(request);

            assertThat(existing.getQuantity()).isEqualByComparingTo("15");
            assertThat(response.quantity()).isEqualByComparingTo("15");
        }
    }

    @Nested
    @DisplayName("updateQuantity")
    class UpdateQuantity {

        @Test
        @DisplayName("reduces quantity and records removal activity for partial exit")
        void reducesQuantity() {
            Long itemId = 42L;
            UpdatePortfolioItemQuantityRequest request = new UpdatePortfolioItemQuantityRequest(
                    new BigDecimal("3"));

            PortfolioItem target = itemWithId(itemId, AssetType.STOCK, "AAPL",
                    new BigDecimal("10"), "USD");

            when(repository.findById(itemId)).thenReturn(Optional.of(target));
            when(repository.findAllByAssetTypeAndSymbol(AssetType.STOCK, "AAPL"))
                    .thenReturn(List.of(target));
            when(repository.save(target)).thenReturn(target);

            PortfolioItemResponse response = portfolioService.updateQuantity(itemId, request);

            assertThat(response.quantity()).isEqualByComparingTo("3");
            assertThat(target.getQuantity()).isEqualByComparingTo("3");

            verify(activityService).recordRemoved(
                    eq(AssetType.STOCK), eq("AAPL"), eq(new BigDecimal("7")),
                    eq("USD"), eq(new BigDecimal("3")), any(Instant.class));
        }

        @Test
        @DisplayName("consolidates legacy duplicate rows during partial exit")
        void consolidatesLegacyDuplicates() {
            Long targetId = 10L;
            UpdatePortfolioItemQuantityRequest request = new UpdatePortfolioItemQuantityRequest(
                    new BigDecimal("2"));

            PortfolioItem target = itemWithId(targetId, AssetType.STOCK, "AAPL",
                    new BigDecimal("8"), "USD");
            PortfolioItem duplicate = itemWithId(11L, AssetType.STOCK, "AAPL",
                    new BigDecimal("2"), "USD");

            when(repository.findById(targetId)).thenReturn(Optional.of(target));
            when(repository.findAllByAssetTypeAndSymbol(AssetType.STOCK, "AAPL"))
                    .thenReturn(List.of(target, duplicate));
            when(repository.save(target)).thenReturn(target);

            portfolioService.updateQuantity(targetId, request);

            verify(repository).delete(duplicate);
            verify(activityService).recordRemoved(
                    eq(AssetType.STOCK), eq("AAPL"), eq(new BigDecimal("8")),
                    eq("USD"), eq(new BigDecimal("2")), any(Instant.class));
        }

        @Test
        @DisplayName("does not record removal activity when quantity unchanged")
        void noRemovalActivityWhenUnchanged() {
            Long itemId = 5L;
            UpdatePortfolioItemQuantityRequest request = new UpdatePortfolioItemQuantityRequest(
                    new BigDecimal("10"));

            PortfolioItem target = itemWithId(itemId, AssetType.STOCK, "AAPL",
                    new BigDecimal("10"), "USD");

            when(repository.findById(itemId)).thenReturn(Optional.of(target));
            when(repository.findAllByAssetTypeAndSymbol(AssetType.STOCK, "AAPL"))
                    .thenReturn(List.of(target));
            when(repository.save(target)).thenReturn(target);

            portfolioService.updateQuantity(itemId, request);

            verify(activityService, never()).recordRemoved(
                    any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("throws PortfolioItemNotFoundException when target does not exist")
        void throwsWhenNotFound() {
            when(repository.findById(99L)).thenReturn(Optional.empty());

            UpdatePortfolioItemQuantityRequest request = new UpdatePortfolioItemQuantityRequest(
                    new BigDecimal("5"));

            assertThatThrownBy(() -> portfolioService.updateQuantity(99L, request))
                    .isInstanceOf(PortfolioItemNotFoundException.class)
                    .hasMessageContaining("99");
        }

        @Test
        @DisplayName("does not record activity when removed quantity is zero (consolidation only)")
        void noActivityWhenConsolidatingExactQuantity() {
            Long targetId = 1L;
            UpdatePortfolioItemQuantityRequest request = new UpdatePortfolioItemQuantityRequest(
                    new BigDecimal("10"));

            PortfolioItem target = itemWithId(targetId, AssetType.STOCK, "AAPL",
                    new BigDecimal("5"), "USD");
            PortfolioItem duplicate = itemWithId(2L, AssetType.STOCK, "AAPL",
                    new BigDecimal("5"), "USD");

            when(repository.findById(targetId)).thenReturn(Optional.of(target));
            when(repository.findAllByAssetTypeAndSymbol(AssetType.STOCK, "AAPL"))
                    .thenReturn(List.of(target, duplicate));
            when(repository.save(target)).thenReturn(target);

            portfolioService.updateQuantity(targetId, request);

            verify(repository).delete(duplicate);
            verify(activityService, never()).recordRemoved(
                    any(), any(), any(), any(), any(), any());
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("deletes all matching holdings and records removal activity")
        void fullRemoval() {
            Long itemId = 20L;
            PortfolioItem target = itemWithId(itemId, AssetType.STOCK, "AAPL",
                    new BigDecimal("10"), "USD");
            PortfolioItem duplicate = itemWithId(21L, AssetType.STOCK, "AAPL",
                    new BigDecimal("3"), "USD");

            when(repository.findById(itemId)).thenReturn(Optional.of(target));
            when(repository.findAllByAssetTypeAndSymbol(AssetType.STOCK, "AAPL"))
                    .thenReturn(List.of(target, duplicate));

            portfolioService.delete(itemId);

            verify(repository).deleteAll(List.of(target, duplicate));
            verify(transactionRepository).deleteByAssetTypeAndSymbol(AssetType.STOCK, "AAPL");
            verify(activityService).recordRemoved(
                    eq(AssetType.STOCK), eq("AAPL"), eq(new BigDecimal("13")),
                    eq("USD"), eq(BigDecimal.ZERO), any(Instant.class));
        }

        @Test
        @DisplayName("throws PortfolioItemNotFoundException when target does not exist")
        void throwsWhenNotFound() {
            when(repository.findById(404L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> portfolioService.delete(404L))
                    .isInstanceOf(PortfolioItemNotFoundException.class)
                    .hasMessageContaining("404");
        }

        @Test
        @DisplayName("records full quantity as removed when no duplicates")
        void singleRowRemoval() {
            Long itemId = 30L;
            PortfolioItem target = itemWithId(itemId, AssetType.STOCK, "TSLA",
                    new BigDecimal("4"), "USD");

            when(repository.findById(itemId)).thenReturn(Optional.of(target));
            when(repository.findAllByAssetTypeAndSymbol(AssetType.STOCK, "TSLA"))
                    .thenReturn(List.of(target));

            portfolioService.delete(itemId);

            verify(activityService).recordRemoved(
                    eq(AssetType.STOCK), eq("TSLA"), eq(new BigDecimal("4")),
                    eq("USD"), eq(BigDecimal.ZERO), any(Instant.class));
            verify(transactionRepository).deleteByAssetTypeAndSymbol(AssetType.STOCK, "TSLA");
            verify(repository).deleteAll(List.of(target));
        }
    }
}

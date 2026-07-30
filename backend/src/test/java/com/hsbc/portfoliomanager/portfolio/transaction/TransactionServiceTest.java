package com.hsbc.portfoliomanager.portfolio.transaction;

import com.hsbc.portfoliomanager.portfolio.activity.PortfolioActivityAction;
import com.hsbc.portfoliomanager.portfolio.activity.PortfolioActivityService;
import com.hsbc.portfoliomanager.portfolio.activity.PortfolioLedgerEntry;
import com.hsbc.portfoliomanager.portfolio.holding.AssetMetadata;
import com.hsbc.portfoliomanager.portfolio.holding.AssetMetadataClient;
import com.hsbc.portfoliomanager.portfolio.holding.AssetType;
import com.hsbc.portfoliomanager.portfolio.holding.PortfolioItem;
import com.hsbc.portfoliomanager.portfolio.holding.PortfolioItemRepository;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransactionService")
class TransactionServiceTest {

    @Mock
    private PortfolioItemRepository portfolioItemRepository;

    @Mock
    private ExchangeRateClient exchangeRateClient;

    @Mock
    private AssetMetadataClient assetMetadataClient;

    @Mock
    private PortfolioActivityService activityService;

    private TransactionService transactionService;

    private static final Instant NOW = Instant.now();

    @BeforeEach
    void setUp() {
        transactionService = new TransactionService(
                portfolioItemRepository,
                exchangeRateClient,
                assetMetadataClient,
                activityService
        );
    }

    // ── Create (BUY) ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("rejects SELL transaction type")
        void rejectsSell() {
            CreateTransactionRequest request = new CreateTransactionRequest(
                    TransactionType.SELL, AssetType.STOCK, "AAPL",
                    new BigDecimal("10"), new BigDecimal("195.00"), "USD", NOW);

            assertThatThrownBy(() -> transactionService.create(request))
                    .isInstanceOf(UnsupportedTransactionTypeException.class)
                    .hasMessageContaining("Only BUY transactions are supported");
        }

        @Test
        @DisplayName("creates stock transaction using Twelve Data metadata")
        void createsStockWithMetadataResolution() {
            CreateTransactionRequest request = new CreateTransactionRequest(
                    TransactionType.BUY, AssetType.STOCK, " aapl ",
                    new BigDecimal("10"), new BigDecimal("195.00"), "usd", NOW);

            AssetMetadata metadata = new AssetMetadata("AAPL", "Apple Inc.", "NASDAQ", "USD");
            when(assetMetadataClient.findBySymbol("AAPL")).thenReturn(metadata);

            PortfolioLedgerEntry ledgerEntry = new PortfolioLedgerEntry(
                    PortfolioActivityAction.ADDED, AssetType.STOCK, "AAPL",
                    new BigDecimal("10"), new BigDecimal("195.00"), "USD", NOW);
            when(activityService.recordAdded(
                    eq(AssetType.STOCK), eq("AAPL"), eq(new BigDecimal("10")),
                    eq(new BigDecimal("195.00")), eq("USD"), eq(NOW)))
                    .thenReturn(ledgerEntry);

            when(portfolioItemRepository.findByAssetTypeAndSymbolAndCurrency(
                    AssetType.STOCK, "AAPL", "USD")).thenReturn(Optional.empty());

            TransactionResponse response = transactionService.create(request);

            // Verify symbol was normalized (trimmed + uppercased)
            verify(assetMetadataClient).findBySymbol("AAPL");

            // Verify activity was recorded
            verify(activityService).recordAdded(
                    AssetType.STOCK, "AAPL", new BigDecimal("10"),
                    new BigDecimal("195.00"), "USD", NOW);

            // Verify response
            assertThat(response.symbol()).isEqualTo("AAPL");
            assertThat(response.assetType()).isEqualTo(AssetType.STOCK);
            assertThat(response.currency()).isEqualTo("USD");

            // Stock: should NOT call exchange rate validation
            verify(exchangeRateClient, never()).isKnownCurrency(anyString());
        }

        @Test
        @DisplayName("adds quantity to existing holding when same asset already held")
        void addsToExistingHolding() {
            CreateTransactionRequest request = new CreateTransactionRequest(
                    TransactionType.BUY, AssetType.STOCK, "AAPL",
                    new BigDecimal("5"), new BigDecimal("200.00"), null, NOW);

            AssetMetadata metadata = new AssetMetadata("AAPL", "Apple Inc.", "NASDAQ", "USD");
            when(assetMetadataClient.findBySymbol("AAPL")).thenReturn(metadata);

            PortfolioItem existingItem = new PortfolioItem(AssetType.STOCK, "AAPL", new BigDecimal("10"), "USD");
            when(portfolioItemRepository.findByAssetTypeAndSymbolAndCurrency(
                    AssetType.STOCK, "AAPL", "USD")).thenReturn(Optional.of(existingItem));

            PortfolioLedgerEntry ledgerEntry = new PortfolioLedgerEntry(
                    PortfolioActivityAction.ADDED, AssetType.STOCK, "AAPL",
                    new BigDecimal("5"), new BigDecimal("200.00"), "USD", NOW);
            when(activityService.recordAdded(
                    eq(AssetType.STOCK), eq("AAPL"), eq(new BigDecimal("5")),
                    eq(new BigDecimal("200.00")), eq("USD"), eq(NOW)))
                    .thenReturn(ledgerEntry);

            transactionService.create(request);

            // Existing item should have quantity added, not a new item saved
            assertThat(existingItem.getQuantity()).isEqualByComparingTo("15"); // 10 + 5
            verify(portfolioItemRepository, never()).save(any(PortfolioItem.class));
        }

        @Test
        @DisplayName("creates new holding when no existing position for that symbol")
        void createsNewHolding() {
            CreateTransactionRequest request = new CreateTransactionRequest(
                    TransactionType.BUY, AssetType.STOCK, "TSLA",
                    new BigDecimal("4"), new BigDecimal("250.00"), null, NOW);

            AssetMetadata metadata = new AssetMetadata("TSLA", "Tesla Inc.", "NASDAQ", "USD");
            when(assetMetadataClient.findBySymbol("TSLA")).thenReturn(metadata);

            when(portfolioItemRepository.findByAssetTypeAndSymbolAndCurrency(
                    AssetType.STOCK, "TSLA", "USD")).thenReturn(Optional.empty());

            PortfolioLedgerEntry ledgerEntry = new PortfolioLedgerEntry(
                    PortfolioActivityAction.ADDED, AssetType.STOCK, "TSLA",
                    new BigDecimal("4"), new BigDecimal("250.00"), "USD", NOW);
            when(activityService.recordAdded(
                    eq(AssetType.STOCK), eq("TSLA"), eq(new BigDecimal("4")),
                    eq(new BigDecimal("250.00")), eq("USD"), eq(NOW)))
                    .thenReturn(ledgerEntry);

            transactionService.create(request);

            // New PortfolioItem should be saved with metadata
            ArgumentCaptor<PortfolioItem> itemCaptor = ArgumentCaptor.forClass(PortfolioItem.class);
            verify(portfolioItemRepository).save(itemCaptor.capture());
            PortfolioItem savedItem = itemCaptor.getValue();
            assertThat(savedItem.getSymbol()).isEqualTo("TSLA");
            assertThat(savedItem.getCompanyName()).isEqualTo("Tesla Inc.");
            assertThat(savedItem.getExchange()).isEqualTo("NASDAQ");
            assertThat(savedItem.getQuantity()).isEqualByComparingTo("4");
            assertThat(savedItem.getCurrency()).isEqualTo("USD");
        }

        @Test
        @DisplayName("validates currency for non-STOCK assets against exchange rate source")
        void validatesNonStockCurrency() {
            CreateTransactionRequest request = new CreateTransactionRequest(
                    TransactionType.BUY, AssetType.CASH, "EUR",
                    new BigDecimal("1000"), new BigDecimal("1.00"), "EUR", NOW);

            when(exchangeRateClient.isKnownCurrency("EUR")).thenReturn(true);

            PortfolioLedgerEntry ledgerEntry = new PortfolioLedgerEntry(
                    PortfolioActivityAction.ADDED, AssetType.CASH, "EUR",
                    new BigDecimal("1000"), new BigDecimal("1.00"), "EUR", NOW);
            when(activityService.recordAdded(
                    eq(AssetType.CASH), eq("EUR"), eq(new BigDecimal("1000")),
                    eq(new BigDecimal("1.00")), eq("EUR"), eq(NOW)))
                    .thenReturn(ledgerEntry);

            when(portfolioItemRepository.findByAssetTypeAndSymbolAndCurrency(
                    AssetType.CASH, "EUR", "EUR")).thenReturn(Optional.empty());

            TransactionResponse response = transactionService.create(request);

            verify(exchangeRateClient).isKnownCurrency("EUR");
            assertThat(response.symbol()).isEqualTo("EUR");
            assertThat(response.currency()).isEqualTo("EUR");
        }

        @Test
        @DisplayName("throws UnknownCurrencyException for unrecognized non-STOCK currency")
        void rejectsUnknownCurrency() {
            CreateTransactionRequest request = new CreateTransactionRequest(
                    TransactionType.BUY, AssetType.CASH, "XXX",
                    new BigDecimal("1000"), new BigDecimal("1.00"), "XXX", NOW);

            when(exchangeRateClient.isKnownCurrency("XXX")).thenReturn(false);

            assertThatThrownBy(() -> transactionService.create(request))
                    .isInstanceOf(UnknownCurrencyException.class)
                    .hasMessageContaining("XXX");

            verify(activityService, never()).recordAdded(
                    any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("defaults missing currency to USD")
        void defaultsCurrencyToUsd() {
            CreateTransactionRequest request = new CreateTransactionRequest(
                    TransactionType.BUY, AssetType.CASH, "USDHOLDING",
                    new BigDecimal("5000"), new BigDecimal("1.00"), "", NOW);

            when(exchangeRateClient.isKnownCurrency("USD")).thenReturn(true);

            PortfolioLedgerEntry ledgerEntry = new PortfolioLedgerEntry(
                    PortfolioActivityAction.ADDED, AssetType.CASH, "USDHOLDING",
                    new BigDecimal("5000"), new BigDecimal("1.00"), "USD", NOW);
            when(activityService.recordAdded(
                    eq(AssetType.CASH), eq("USDHOLDING"), eq(new BigDecimal("5000")),
                    eq(new BigDecimal("1.00")), eq("USD"), eq(NOW)))
                    .thenReturn(ledgerEntry);

            when(portfolioItemRepository.findByAssetTypeAndSymbolAndCurrency(
                    AssetType.CASH, "USDHOLDING", "USD")).thenReturn(Optional.empty());

            TransactionResponse response = transactionService.create(request);

            assertThat(response.currency()).isEqualTo("USD");
            verify(exchangeRateClient).isKnownCurrency("USD");
        }

        @Test
        @DisplayName("uses metadata canonical symbol instead of user-submitted casing")
        void usesMetadataCanonicalSymbol() {
            CreateTransactionRequest request = new CreateTransactionRequest(
                    TransactionType.BUY, AssetType.STOCK, "aapl",
                    new BigDecimal("1"), new BigDecimal("195.00"), null, NOW);

            // Metadata returns canonical symbol in uppercase with additional info
            AssetMetadata metadata = new AssetMetadata("AAPL", "Apple Inc.", "NASDAQ", "USD");
            when(assetMetadataClient.findBySymbol("AAPL")).thenReturn(metadata);

            when(portfolioItemRepository.findByAssetTypeAndSymbolAndCurrency(
                    AssetType.STOCK, "AAPL", "USD")).thenReturn(Optional.empty());

            PortfolioLedgerEntry ledgerEntry = new PortfolioLedgerEntry(
                    PortfolioActivityAction.ADDED, AssetType.STOCK, "AAPL",
                    new BigDecimal("1"), new BigDecimal("195.00"), "USD", NOW);
            when(activityService.recordAdded(
                    eq(AssetType.STOCK), eq("AAPL"), eq(new BigDecimal("1")),
                    eq(new BigDecimal("195.00")), eq("USD"), eq(NOW)))
                    .thenReturn(ledgerEntry);

            TransactionResponse response = transactionService.create(request);

            assertThat(response.symbol()).isEqualTo("AAPL");
        }
    }

    // ── findByType ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("findByType")
    class FindByType {

        @Test
        @DisplayName("returns empty list when no purchases exist")
        void returnsEmptyList() {
            when(activityService.findPurchasesNewestFirst())
                    .thenReturn(List.of());

            List<TransactionResponse> result = transactionService.findByType(TransactionType.BUY);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("returns purchases mapped to response DTOs sorted newest first")
        void returnsTransactionHistory() {
            Instant earlier = NOW.minusSeconds(3600);
            PortfolioLedgerEntry entry1 = new PortfolioLedgerEntry(
                    PortfolioActivityAction.ADDED, AssetType.STOCK, "AAPL",
                    new BigDecimal("10"), new BigDecimal("195.00"), "USD", earlier);
            PortfolioLedgerEntry entry2 = new PortfolioLedgerEntry(
                    PortfolioActivityAction.ADDED, AssetType.STOCK, "TSLA",
                    new BigDecimal("4"), new BigDecimal("250.00"), "USD", NOW);

            when(activityService.findPurchasesNewestFirst())
                    .thenReturn(List.of(entry2, entry1)); // newest first

            List<TransactionResponse> result = transactionService.findByType(TransactionType.BUY);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).symbol()).isEqualTo("TSLA");
            assertThat(result.get(1).symbol()).isEqualTo("AAPL");
            assertThat(result.get(0).purchasedAt()).isEqualTo(NOW);
        }

        @Test
        @DisplayName("returns empty for non-BUY types without querying repository")
        void returnsEmptyForSellType() {
            List<TransactionResponse> result = transactionService.findByType(TransactionType.SELL);

            assertThat(result).isEmpty();
            verify(activityService, never()).findPurchasesNewestFirst();
        }
    }
}

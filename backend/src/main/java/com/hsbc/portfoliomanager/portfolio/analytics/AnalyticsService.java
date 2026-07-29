package com.hsbc.portfoliomanager.portfolio.analytics;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.hsbc.portfoliomanager.marketdata.MarketDataService;
import com.hsbc.portfoliomanager.portfolio.holding.PortfolioItem;
import com.hsbc.portfoliomanager.portfolio.holding.PortfolioItemRepository;
import com.hsbc.portfoliomanager.portfolio.transaction.TransactionRecord;
import com.hsbc.portfoliomanager.portfolio.transaction.TransactionRepository;
import com.hsbc.portfoliomanager.portfolio.transaction.TransactionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class AnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsService.class);

    private final PortfolioItemRepository portfolioItemRepository;
    private final TransactionRepository transactionRepository;
    private final MarketDataService marketDataService;

    AnalyticsService(PortfolioItemRepository portfolioItemRepository,
                     TransactionRepository transactionRepository,
                     MarketDataService marketDataService) {
        this.portfolioItemRepository = portfolioItemRepository;
        this.transactionRepository = transactionRepository;
        this.marketDataService = marketDataService;
    }

    @Transactional(readOnly = true)
    PortfolioValueResponse calculateCurrentValue() {
        List<PortfolioItem> items = portfolioItemRepository.findAll();

        if (items.isEmpty()) {
            return new PortfolioValueResponse(
                    "USD", null, "COMPLETE", List.of(), List.of());
        }

        List<PortfolioValueResponse.AssetValue> assets = new ArrayList<>();
        List<String> missingPrices = new ArrayList<>();
        Instant latestUpdate = null;

        for (PortfolioItem item : items) {
            var priceDataOpt = marketDataService.getCurrentPrice(item.getSymbol());

            if (priceDataOpt.isEmpty()) {
                missingPrices.add(item.getSymbol());
                assets.add(new PortfolioValueResponse.AssetValue(
                        item.getSymbol(),
                        item.getAssetType().name(),
                        item.getQuantity(),
                        null,
                        null,
                        null
                ));
                continue;
            }

            var priceData = priceDataOpt.get();
            BigDecimal currentPrice = priceData.price();
            String currency = priceData.currency();

            // Convert to USD if needed
            if (!"USD".equalsIgnoreCase(currency)) {
                var convertedOpt = marketDataService.convertToUsd(currentPrice, currency);
                if (convertedOpt.isPresent()) {
                    currentPrice = convertedOpt.get();
                    currency = "USD";
                }
                // If conversion fails, keep original price and currency
            }

            BigDecimal marketValue = currentPrice.multiply(item.getQuantity());

            if (latestUpdate == null || priceData.updatedAt().isAfter(latestUpdate)) {
                latestUpdate = priceData.updatedAt();
            }

            assets.add(new PortfolioValueResponse.AssetValue(
                    item.getSymbol(),
                    item.getAssetType().name(),
                    item.getQuantity(),
                    currentPrice,
                    marketValue,
                    currency
            ));
        }

        String status;
        if (missingPrices.isEmpty()) {
            status = "COMPLETE";
        } else if (assets.stream().anyMatch(a -> a.currentPrice() != null)) {
            status = "PARTIAL";
        } else {
            status = "UNAVAILABLE";
        }

        return new PortfolioValueResponse("USD", latestUpdate, status, assets, missingPrices);
    }

    @Transactional(readOnly = true)
    PortfolioPerformanceResponse calculatePerformance() {
        List<PortfolioItem> holdings = portfolioItemRepository.findAll();

        if (holdings.isEmpty()) {
            return new PortfolioPerformanceResponse(
                    "USD",
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    "COMPLETE",
                    null,
                    List.of(),
                    List.of()
            );
        }

        // Load all buy transactions once
        List<TransactionRecord> allBuys =
                transactionRepository.findByTransactionTypeOrderByTransactedAtDesc(TransactionType.BUY);

        // Group transactions by symbol for weighted average cost calculation
        Map<String, List<TransactionRecord>> buysBySymbol = new HashMap<>();
        for (TransactionRecord tx : allBuys) {
            buysBySymbol.computeIfAbsent(tx.getSymbol(), k -> new ArrayList<>()).add(tx);
        }

        List<PortfolioPerformanceResponse.AssetPerformance> assetPerformances = new ArrayList<>();
        List<String> missingPrices = new ArrayList<>();
        Instant latestUpdate = null;

        BigDecimal portfolioTotalCost = BigDecimal.ZERO;
        BigDecimal portfolioTotalValue = BigDecimal.ZERO;

        for (PortfolioItem holding : holdings) {
            String symbol = holding.getSymbol();

            // Calculate weighted average cost
            BigDecimal averageCost = calculateWeightedAverageCost(buysBySymbol.get(symbol));
            BigDecimal costBasis = BigDecimal.ZERO;
            if (averageCost != null) {
                costBasis = averageCost.multiply(holding.getQuantity());
            }

            // Get current price
            var priceDataOpt = marketDataService.getCurrentPrice(symbol);

            if (priceDataOpt.isEmpty()) {
                missingPrices.add(symbol);

                // Include what we know even without current price
                if (averageCost != null) {
                    portfolioTotalCost = portfolioTotalCost.add(costBasis);
                }

                assetPerformances.add(new PortfolioPerformanceResponse.AssetPerformance(
                        symbol,
                        holding.getQuantity(),
                        averageCost,
                        null, // current price unknown
                        costBasis,
                        null, // current value unknown
                        null, // P&L unknown
                        null, // return % unknown
                        null  // allocation % unknown
                ));
                continue;
            }

            var priceData = priceDataOpt.get();
            BigDecimal currentPrice = priceData.price();
            String currency = priceData.currency();

            // Convert to USD if needed
            if (!"USD".equalsIgnoreCase(currency)) {
                var convertedOpt = marketDataService.convertToUsd(currentPrice, currency);
                if (convertedOpt.isPresent()) {
                    currentPrice = convertedOpt.get();
                }
            }

            // Also convert the average cost if the transactions are in a different currency
            BigDecimal avgCostInUsd = averageCost;
            if (averageCost != null && !"USD".equalsIgnoreCase(currency)) {
                var convertedCostOpt = marketDataService.convertToUsd(averageCost, currency);
                if (convertedCostOpt.isPresent()) {
                    avgCostInUsd = convertedCostOpt.get();
                    costBasis = avgCostInUsd.multiply(holding.getQuantity());
                }
            }

            BigDecimal currentValue = currentPrice.multiply(holding.getQuantity());

            BigDecimal unrealizedPnl = BigDecimal.ZERO;
            BigDecimal returnPct = BigDecimal.ZERO;

            if (averageCost != null && avgCostInUsd.compareTo(BigDecimal.ZERO) > 0) {
                unrealizedPnl = currentValue.subtract(costBasis);
                returnPct = unrealizedPnl
                        .multiply(BigDecimal.valueOf(100))
                        .divide(costBasis, 4, RoundingMode.HALF_UP);
            }

            if (latestUpdate == null || priceData.updatedAt().isAfter(latestUpdate)) {
                latestUpdate = priceData.updatedAt();
            }

            portfolioTotalCost = portfolioTotalCost.add(costBasis);
            portfolioTotalValue = portfolioTotalValue.add(currentValue);

            assetPerformances.add(new PortfolioPerformanceResponse.AssetPerformance(
                    symbol,
                    holding.getQuantity(),
                    avgCostInUsd,
                    currentPrice,
                    costBasis,
                    currentValue,
                    unrealizedPnl,
                    returnPct,
                    null // allocation % calculated after we know total
            ));
        }

        // Calculate allocation percentages
        if (portfolioTotalValue.compareTo(BigDecimal.ZERO) > 0) {
            List<PortfolioPerformanceResponse.AssetPerformance> updatedAssets = new ArrayList<>();
            for (var asset : assetPerformances) {
                BigDecimal allocation = null;
                if (asset.currentValue() != null) {
                    allocation = asset.currentValue()
                            .multiply(BigDecimal.valueOf(100))
                            .divide(portfolioTotalValue, 4, RoundingMode.HALF_UP);
                }
                updatedAssets.add(new PortfolioPerformanceResponse.AssetPerformance(
                        asset.symbol(),
                        asset.quantity(),
                        asset.averageCost(),
                        asset.currentPrice(),
                        asset.costBasis(),
                        asset.currentValue(),
                        asset.unrealizedProfitLoss(),
                        asset.returnPercentage(),
                        allocation
                ));
            }
            assetPerformances = updatedAssets;
        }

        // Portfolio-level P&L and return
        BigDecimal totalUnrealizedPnl = portfolioTotalValue.subtract(portfolioTotalCost);
        BigDecimal totalReturnPct = BigDecimal.ZERO;
        if (portfolioTotalCost.compareTo(BigDecimal.ZERO) > 0) {
            totalReturnPct = totalUnrealizedPnl
                    .multiply(BigDecimal.valueOf(100))
                    .divide(portfolioTotalCost, 4, RoundingMode.HALF_UP);
        }

        String status = missingPrices.isEmpty() ? "COMPLETE" : "PARTIAL";

        return new PortfolioPerformanceResponse(
                "USD",
                portfolioTotalCost,
                portfolioTotalValue,
                totalUnrealizedPnl,
                totalReturnPct,
                status,
                latestUpdate,
                assetPerformances,
                missingPrices
        );
    }

    /**
     * Calculate weighted average cost per unit for a list of buy transactions.
     * Weighted average = total spent / total quantity bought.
     */
    private BigDecimal calculateWeightedAverageCost(List<TransactionRecord> buys) {
        if (buys == null || buys.isEmpty()) {
            return null;
        }

        BigDecimal totalCost = BigDecimal.ZERO;
        BigDecimal totalQuantity = BigDecimal.ZERO;

        for (TransactionRecord tx : buys) {
            BigDecimal txCost = tx.getPricePerUnit().multiply(tx.getQuantity());
            totalCost = totalCost.add(txCost);
            totalQuantity = totalQuantity.add(tx.getQuantity());
        }

        if (totalQuantity.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }

        return totalCost.divide(totalQuantity, 6, RoundingMode.HALF_UP);
    }
}

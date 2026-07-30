package com.hsbc.portfoliomanager.portfolio.analytics;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.hsbc.portfoliomanager.marketdata.MarketDataService;
import com.hsbc.portfoliomanager.portfolio.activity.PortfolioActivityAction;
import com.hsbc.portfoliomanager.portfolio.activity.PortfolioActivityService;
import com.hsbc.portfoliomanager.portfolio.activity.PortfolioLedgerEntry;
import com.hsbc.portfoliomanager.portfolio.holding.AssetType;
import com.hsbc.portfoliomanager.portfolio.holding.PortfolioItem;
import com.hsbc.portfoliomanager.portfolio.holding.PortfolioItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class AnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsService.class);

    private final PortfolioItemRepository portfolioItemRepository;
    private final PortfolioActivityService activityService;
    private final MarketDataService marketDataService;

    /**
     * 中文：注入持仓仓库、交易仓库和当前市场数据服务。
     * English: Injects the holding repository, transaction repository, and current market data service.
     */
    AnalyticsService(PortfolioItemRepository portfolioItemRepository,
                     PortfolioActivityService activityService,
                     MarketDataService marketDataService) {
        this.portfolioItemRepository = portfolioItemRepository;
        this.activityService = activityService;
        this.marketDataService = marketDataService;
    }

    /**
     * 中文：使用当前价格计算每项持仓的市值，并标记缺失价格的数据状态。
     * English: Uses current prices to calculate each holding's market value and reports missing-price status.
     */
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
            var priceDataOpt = getCurrentPrice(item);

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

    /**
     * 中文：根据购买历史和当前市场价格计算组合及单项资产的绩效指标。
     * English: Calculates portfolio-level and asset-level performance from purchase history and current market prices.
     */
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
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    "COMPLETE",
                    null,
                    List.of(),
                    List.of()
            );
        }

        Map<AssetKey, CostState> costsByAsset = calculateCostStates(
                activityService.findLedgerOldestFirst()
        );

        List<PortfolioPerformanceResponse.AssetPerformance> assetPerformances = new ArrayList<>();
        List<String> missingPrices = new ArrayList<>();
        Instant latestUpdate = null;

        BigDecimal portfolioTotalCost = BigDecimal.ZERO;
        BigDecimal portfolioTotalValue = BigDecimal.ZERO;
        BigDecimal portfolioPreviousCloseValue = BigDecimal.ZERO;
        boolean completeDayChange = true;

        for (PortfolioItem holding : holdings) {
            String symbol = holding.getSymbol();

            CostState costState = costsByAsset.get(new AssetKey(
                    holding.getAssetType(),
                    holding.getSymbol(),
                    holding.getCurrency()
            ));
            BigDecimal averageCost = averageCost(costState);
            BigDecimal costBasis = BigDecimal.ZERO;
            if (averageCost != null) {
                costBasis = averageCost.multiply(holding.getQuantity());
            }

            // Get current price
            var priceDataOpt = getCurrentPrice(holding);

            if (priceDataOpt.isEmpty()) {
                missingPrices.add(symbol);
                completeDayChange = false;

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
            BigDecimal previousClose = priceData.previousClose();
            String currency = priceData.currency();

            // Convert to USD if needed
            if (!"USD".equalsIgnoreCase(currency)) {
                var convertedOpt = marketDataService.convertToUsd(currentPrice, currency);
                if (convertedOpt.isPresent()) {
                    currentPrice = convertedOpt.get();
                } else {
                    completeDayChange = false;
                }

                if (previousClose != null) {
                    var convertedPreviousClose =
                            marketDataService.convertToUsd(previousClose, currency);
                    if (convertedPreviousClose.isPresent()) {
                        previousClose = convertedPreviousClose.get();
                    } else {
                        completeDayChange = false;
                    }
                }
            }

            if (previousClose == null) {
                completeDayChange = false;
            } else {
                portfolioPreviousCloseValue = portfolioPreviousCloseValue.add(
                        previousClose.multiply(holding.getQuantity())
                );
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

        BigDecimal dayChange = null;
        BigDecimal dayChangePercentage = null;
        if (completeDayChange) {
            dayChange = portfolioTotalValue.subtract(portfolioPreviousCloseValue);
            dayChangePercentage = portfolioPreviousCloseValue.signum() == 0
                    ? BigDecimal.ZERO
                    : dayChange
                            .multiply(BigDecimal.valueOf(100))
                            .divide(portfolioPreviousCloseValue, 4, RoundingMode.HALF_UP);
        }

        String status = missingPrices.isEmpty() ? "COMPLETE" : "PARTIAL";

        return new PortfolioPerformanceResponse(
                "USD",
                portfolioTotalCost,
                portfolioTotalValue,
                totalUnrealizedPnl,
                totalReturnPct,
                dayChange,
                dayChangePercentage,
                status,
                latestUpdate,
                assetPerformances,
                missingPrices
        );
    }

    /**
     * 中文：按时间顺序重放新增与移除流水，计算每项当前持仓的剩余数量和成本。
     * English: Replays added and removed ledger entries chronologically to calculate remaining quantity and cost.
     */
    private Map<AssetKey, CostState> calculateCostStates(List<PortfolioLedgerEntry> ledger) {
        Map<AssetKey, CostState> result = new HashMap<>();
        for (PortfolioLedgerEntry entry : ledger) {
            AssetKey key = new AssetKey(entry.assetType(), entry.symbol(), entry.currency());
            CostState state = result.computeIfAbsent(key, ignored -> new CostState());

            if (entry.action() == PortfolioActivityAction.ADDED) {
                state.quantity = state.quantity.add(entry.quantity());
                if (entry.pricePerUnit() == null) {
                    state.costKnown = false;
                } else if (state.costKnown) {
                    state.cost = state.cost.add(
                            entry.pricePerUnit().multiply(entry.quantity())
                    );
                }
                continue;
            }

            if (state.quantity.signum() <= 0) {
                continue;
            }
            BigDecimal removed = entry.quantity().min(state.quantity);
            if (state.costKnown) {
                BigDecimal averageCost = state.cost.divide(
                        state.quantity,
                        12,
                        RoundingMode.HALF_UP
                );
                state.cost = state.cost.subtract(averageCost.multiply(removed));
            }
            state.quantity = state.quantity.subtract(removed);
            if (state.quantity.signum() == 0
                    || (entry.remainingQuantity() != null
                        && entry.remainingQuantity().signum() == 0)) {
                state.quantity = BigDecimal.ZERO;
                state.cost = BigDecimal.ZERO;
                state.costKnown = true;
            }
        }
        return result;
    }

    /**
     * 中文：由重建后的剩余成本和数量计算当前平均成本。
     * English: Calculates current average cost from the reconstructed remaining cost and quantity.
     */
    private BigDecimal averageCost(CostState state) {
        if (state == null || !state.costKnown || state.quantity.signum() <= 0) {
            return null;
        }
        return state.cost.divide(state.quantity, 6, RoundingMode.HALF_UP);
    }

    /**
     * 中文：有交易所元数据时按“代码 + 交易所”查询当前价格，否则保持原有代码查询方式。
     * English: Queries current price by symbol and exchange when metadata exists, otherwise uses symbol-only lookup.
     */
    private java.util.Optional<MarketDataService.PriceData> getCurrentPrice(PortfolioItem item) {
        if (item.getExchange() == null || item.getExchange().isBlank()) {
            return marketDataService.getCurrentPrice(item.getSymbol());
        }
        return marketDataService.getCurrentPrice(item.getSymbol(), item.getExchange());
    }

    private record AssetKey(
            AssetType assetType,
            String symbol,
            String currency
    ) {
    }

    private static final class CostState {
        private BigDecimal quantity = BigDecimal.ZERO;
        private BigDecimal cost = BigDecimal.ZERO;
        private boolean costKnown = true;
    }
}

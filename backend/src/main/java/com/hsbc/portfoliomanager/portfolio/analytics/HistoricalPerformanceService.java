package com.hsbc.portfoliomanager.portfolio.analytics;

import com.hsbc.portfoliomanager.marketdata.HistoricalMarketDataService;
import com.hsbc.portfoliomanager.marketdata.HistoricalMarketDataService.PricePoint;
import com.hsbc.portfoliomanager.portfolio.holding.AssetType;
import com.hsbc.portfoliomanager.portfolio.holding.PortfolioItem;
import com.hsbc.portfoliomanager.portfolio.holding.PortfolioItemRepository;
import com.hsbc.portfoliomanager.portfolio.transaction.TransactionRecord;
import com.hsbc.portfoliomanager.portfolio.transaction.TransactionRepository;
import com.hsbc.portfoliomanager.portfolio.transaction.TransactionType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

@Service
class HistoricalPerformanceService {

    private static final String REPORTING_CURRENCY = "USD";
    private static final int PRICE_LOOKBACK_DAYS = 7;

    private final PortfolioItemRepository portfolioItemRepository;
    private final TransactionRepository transactionRepository;
    private final HistoricalMarketDataService historicalMarketDataService;

    /**
     * 中文：注入持仓、交易和历史市场数据依赖。
     * English: Injects holding, transaction, and historical market data dependencies.
     */
    HistoricalPerformanceService(
            PortfolioItemRepository portfolioItemRepository,
            TransactionRepository transactionRepository,
            HistoricalMarketDataService historicalMarketDataService
    ) {
        this.portfolioItemRepository = portfolioItemRepository;
        this.transactionRepository = transactionRepository;
        this.historicalMarketDataService = historicalMarketDataService;
    }

    /**
     * 中文：按时间范围重建每日持仓，并计算历史市值、成本和收益率。
     * English: Reconstructs daily holdings and calculates historical value, cost, and return for a time range.
     */
    HistoricalPerformanceResponse calculate(String requestedRange) {
        String range = normalizeRange(requestedRange);
        LocalDate endDate = LocalDate.now(ZoneOffset.UTC);
        List<TransactionRecord> transactions = transactionRepository.findAllByOrderByTransactedAtAsc();

        if (transactions.isEmpty()) {
            LocalDate startDate = startDate(range, endDate, null);
            return new HistoricalPerformanceResponse(
                    REPORTING_CURRENCY,
                    range,
                    startDate,
                    endDate,
                    "UNAVAILABLE",
                    List.of(),
                    List.of("TRANSACTION_HISTORY")
            );
        }

        LocalDate earliestTransactionDate = transactionDate(transactions.get(0));
        LocalDate startDate = startDate(range, endDate, earliestTransactionDate);
        LocalDate fetchStartDate = startDate.minusDays(PRICE_LOOKBACK_DAYS);

        Map<AssetKey, String> exchanges = exchangesByAsset();
        Set<AssetKey> assets = new LinkedHashSet<>();
        for (TransactionRecord transaction : transactions) {
            assets.add(new AssetKey(
                    transaction.getAssetType(),
                    transaction.getSymbol(),
                    transaction.getCurrency()
            ));
        }

        Map<AssetKey, NavigableMap<LocalDate, PricePoint>> priceSeries = new HashMap<>();
        Set<LocalDate> valuationDates = new LinkedHashSet<>();
        Set<String> missingData = new LinkedHashSet<>();

        for (AssetKey asset : assets) {
            List<PricePoint> prices = historicalMarketDataService.getDailyPrices(
                    asset.assetType(),
                    asset.symbol(),
                    exchanges.getOrDefault(asset, ""),
                    asset.currency(),
                    fetchStartDate,
                    endDate
            );
            if (prices.isEmpty()) {
                missingData.add(asset.symbol() + ":PRICE_HISTORY");
                continue;
            }

            NavigableMap<LocalDate, PricePoint> byDate = new TreeMap<>();
            prices.forEach(price -> {
                byDate.put(price.date(), price);
                if (!price.date().isBefore(startDate) && !price.date().isAfter(endDate)) {
                    valuationDates.add(price.date());
                }
            });
            priceSeries.put(asset, byDate);

            /*
             * Historical prices may exist for the requested range but all of them can
             * still be earlier than the first purchase. In that case there is no date
             * on which both a position and a price exist, so report the first date
             * that actually requires a valuation instead of returning an empty
             * missingData list.
             */
            LocalDate firstRequiredDate = firstBuyDate(asset, transactions);
            if (firstRequiredDate != null) {
                firstRequiredDate = firstRequiredDate.isBefore(startDate)
                        ? startDate
                        : firstRequiredDate;
                if (byDate.ceilingEntry(firstRequiredDate) == null) {
                    missingData.add(asset.symbol() + ":" + firstRequiredDate);
                }
            }
        }

        List<LocalDate> sortedDates = valuationDates.stream().sorted().toList();
        Map<AssetKey, PositionState> positions = new HashMap<>();
        List<HistoricalPerformanceResponse.PerformancePoint> points = new ArrayList<>();
        int transactionIndex = 0;

        for (LocalDate valuationDate : sortedDates) {
            while (transactionIndex < transactions.size()
                    && !transactionDate(transactions.get(transactionIndex)).isAfter(valuationDate)) {
                applyTransaction(
                        positions,
                        transactions.get(transactionIndex),
                        missingData
                );
                transactionIndex++;
            }

            BigDecimal marketValue = BigDecimal.ZERO;
            BigDecimal costBasis = BigDecimal.ZERO;
            boolean hasPosition = false;
            boolean completeCostBasis = true;
            boolean completeMarketValue = true;

            for (Map.Entry<AssetKey, PositionState> entry : positions.entrySet()) {
                AssetKey asset = entry.getKey();
                PositionState position = entry.getValue();
                if (position.quantity.signum() <= 0) {
                    continue;
                }
                hasPosition = true;

                if (!position.costKnown) {
                    completeCostBasis = false;
                } else {
                    costBasis = costBasis.add(position.costBasisUsd);
                }

                NavigableMap<LocalDate, PricePoint> prices = priceSeries.get(asset);
                Map.Entry<LocalDate, PricePoint> priceEntry =
                        prices == null ? null : prices.floorEntry(valuationDate);
                if (priceEntry == null) {
                    completeMarketValue = false;
                    missingData.add(asset.symbol() + ":" + valuationDate);
                    continue;
                }

                PricePoint price = priceEntry.getValue();
                var rate = historicalMarketDataService.getRateToUsd(
                        price.currency(),
                        valuationDate
                );
                if (rate.isEmpty()) {
                    completeMarketValue = false;
                    missingData.add(price.currency() + "/USD:" + valuationDate);
                    continue;
                }

                marketValue = marketValue.add(
                        price.closePrice()
                                .multiply(position.quantity)
                                .multiply(rate.get())
                );
            }

            if (!hasPosition) {
                continue;
            }

            BigDecimal pointCostBasis = completeCostBasis ? costBasis : null;
            BigDecimal pointMarketValue = completeMarketValue ? marketValue : null;
            BigDecimal profitLoss = null;
            BigDecimal returnPercentage = null;

            if (pointCostBasis != null && pointMarketValue != null) {
                profitLoss = pointMarketValue.subtract(pointCostBasis);
                returnPercentage = pointCostBasis.signum() == 0
                        ? BigDecimal.ZERO
                        : profitLoss.multiply(BigDecimal.valueOf(100))
                                .divide(pointCostBasis, 4, RoundingMode.HALF_UP);
            }

            points.add(new HistoricalPerformanceResponse.PerformancePoint(
                    valuationDate,
                    pointMarketValue,
                    pointCostBasis,
                    profitLoss,
                    returnPercentage
            ));
        }

        String status;
        if (points.isEmpty()) {
            status = "UNAVAILABLE";
        } else if (missingData.isEmpty()) {
            status = "COMPLETE";
        } else {
            status = "PARTIAL";
        }

        return new HistoricalPerformanceResponse(
                REPORTING_CURRENCY,
                range,
                startDate,
                endDate,
                status,
                points,
                List.copyOf(missingData)
        );
    }

    /**
     * 中文：将一笔交易应用到指定资产的历史持仓数量和美元成本状态。
     * English: Applies a transaction to an asset's historical quantity and USD cost state.
     */
    private void applyTransaction(
            Map<AssetKey, PositionState> positions,
            TransactionRecord transaction,
            Set<String> missingData
    ) {
        AssetKey key = new AssetKey(
                transaction.getAssetType(),
                transaction.getSymbol(),
                transaction.getCurrency()
        );
        PositionState position = positions.computeIfAbsent(key, ignored -> new PositionState());

        if (transaction.getTransactionType() == TransactionType.BUY) {
            LocalDate date = transactionDate(transaction);
            var rate = historicalMarketDataService.getRateToUsd(transaction.getCurrency(), date);
            position.quantity = position.quantity.add(transaction.getQuantity());
            if (rate.isEmpty()) {
                position.costKnown = false;
                missingData.add(transaction.getCurrency() + "/USD:" + date);
                return;
            }
            position.costBasisUsd = position.costBasisUsd.add(
                    transaction.getPricePerUnit()
                            .multiply(transaction.getQuantity())
                            .multiply(rate.get())
            );
            return;
        }

        if (position.quantity.signum() <= 0) {
            missingData.add(transaction.getSymbol() + ":INVALID_SELL_HISTORY");
            position.costKnown = false;
            return;
        }

        BigDecimal soldQuantity = transaction.getQuantity().min(position.quantity);
        if (position.costKnown) {
            BigDecimal averageCost = position.costBasisUsd.divide(
                    position.quantity,
                    12,
                    RoundingMode.HALF_UP
            );
            position.costBasisUsd = position.costBasisUsd.subtract(
                    averageCost.multiply(soldQuantity)
            );
        }
        position.quantity = position.quantity.subtract(soldQuantity);
    }

    /**
     * 中文：建立资产与交易所的映射，为历史价格查询提供交易所参数。
     * English: Builds an asset-to-exchange mapping for historical price queries.
     */
    private Map<AssetKey, String> exchangesByAsset() {
        Map<AssetKey, String> result = new HashMap<>();
        for (PortfolioItem item : portfolioItemRepository.findAll()) {
            result.put(
                    new AssetKey(item.getAssetType(), item.getSymbol(), item.getCurrency()),
                    item.getExchange() == null ? "" : item.getExchange()
            );
        }
        return result;
    }

    /**
     * 中文：将交易时间转换为 UTC 日期。
     * English: Converts a transaction timestamp into a UTC calendar date.
     */
    private static LocalDate transactionDate(TransactionRecord transaction) {
        return transaction.getTransactedAt().atZone(ZoneOffset.UTC).toLocalDate();
    }

    /**
     * 中文：查找指定资产最早的买入日期。
     * English: Finds the earliest purchase date for the specified asset.
     */
    private static LocalDate firstBuyDate(
            AssetKey asset,
            List<TransactionRecord> transactions
    ) {
        return transactions.stream()
                .filter(transaction -> transaction.getTransactionType() == TransactionType.BUY)
                .filter(transaction -> new AssetKey(
                        transaction.getAssetType(),
                        transaction.getSymbol(),
                        transaction.getCurrency()
                ).equals(asset))
                .map(HistoricalPerformanceService::transactionDate)
                .min(LocalDate::compareTo)
                .orElse(null);
    }

    /**
     * 中文：标准化并校验历史绩效时间范围参数。
     * English: Normalizes and validates a historical performance range parameter.
     */
    private static String normalizeRange(String requestedRange) {
        String normalized = requestedRange == null
                ? "1M"
                : requestedRange.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "1W", "1M", "3M", "1Y", "ALL" -> normalized;
            default -> throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "range must be one of: 1W, 1M, 3M, 1Y, ALL"
            );
        };
    }

    /**
     * 中文：根据时间范围和最早交易日期计算查询起始日期。
     * English: Calculates the query start date from the requested range and earliest transaction date.
     */
    private static LocalDate startDate(
            String range,
            LocalDate endDate,
            LocalDate earliestTransactionDate
    ) {
        return switch (range) {
            case "1W" -> endDate.minusWeeks(1);
            case "1M" -> endDate.minusMonths(1);
            case "3M" -> endDate.minusMonths(3);
            case "1Y" -> endDate.minusYears(1);
            case "ALL" -> earliestTransactionDate == null ? endDate : earliestTransactionDate;
            default -> throw new IllegalStateException("Unexpected range: " + range);
        };
    }

    private record AssetKey(
            AssetType assetType,
            String symbol,
            String currency
    ) {
    }

    private static final class PositionState {
        private BigDecimal quantity = BigDecimal.ZERO;
        private BigDecimal costBasisUsd = BigDecimal.ZERO;
        private boolean costKnown = true;
    }
}

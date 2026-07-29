package com.hsbc.portfoliomanager.marketdata;

import com.hsbc.portfoliomanager.portfolio.holding.AssetType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
class HistoricalPriceService {

    private static final String DAILY_INTERVAL = "1day";
    private static final int MARKET_CLOSURE_TOLERANCE_DAYS = 7;

    private final PriceHistoryRepository repository;
    private final TwelveDataHistoricalClient client;

    HistoricalPriceService(PriceHistoryRepository repository, TwelveDataHistoricalClient client) {
        this.repository = repository;
        this.client = client;
    }

    @Transactional
    List<HistoricalMarketDataService.PricePoint> getDailyPrices(
            AssetType assetType,
            String symbol,
            String exchange,
            String currency,
            LocalDate startDate,
            LocalDate endDate
    ) {
        String normalizedSymbol = symbol.trim().toUpperCase(Locale.ROOT);
        String normalizedExchange = normalize(exchange);
        String normalizedCurrency = currency.trim().toUpperCase(Locale.ROOT);

        List<PriceHistory> stored = repository
                .findByAssetTypeAndSymbolAndExchangeAndCurrencyAndTimeIntervalAndPriceDateBetweenOrderByPriceDateAsc(
                        assetType,
                        normalizedSymbol,
                        normalizedExchange,
                        normalizedCurrency,
                        DAILY_INTERVAL,
                        startDate,
                        endDate
                );

        Map<LocalDate, HistoricalMarketDataService.PricePoint> points = new LinkedHashMap<>();
        stored.forEach(price -> points.put(price.getPriceDate(), toPoint(price)));

        if (!coversRequestedRange(stored, startDate, endDate)) {
            List<TwelveDataHistoricalClient.HistoricalPrice> fetched = client.fetchDailyPrices(
                    assetType,
                    normalizedSymbol,
                    normalizedExchange,
                    normalizedCurrency,
                    startDate,
                    endDate
            );

            List<PriceHistory> newRows = new ArrayList<>();
            for (TwelveDataHistoricalClient.HistoricalPrice price : fetched) {
                repository.findByAssetTypeAndSymbolAndExchangeAndCurrencyAndPriceDateAndTimeInterval(
                        price.assetType(),
                        price.symbol(),
                        price.exchange(),
                        price.currency(),
                        price.date(),
                        DAILY_INTERVAL
                ).ifPresentOrElse(
                        existing -> points.put(existing.getPriceDate(), toPoint(existing)),
                        () -> {
                            PriceHistory row = new PriceHistory(
                                    price.assetType(),
                                    price.symbol(),
                                    price.exchange(),
                                    price.currency(),
                                    price.date(),
                                    DAILY_INTERVAL,
                                    price.open(),
                                    price.high(),
                                    price.low(),
                                    price.close(),
                                    price.volume(),
                                    "TWELVE_DATA",
                                    null
                            );
                            newRows.add(row);
                            points.put(price.date(), toPoint(row));
                        }
                );
            }
            repository.saveAll(newRows);
        }

        return points.values().stream()
                .filter(point -> !point.date().isBefore(startDate) && !point.date().isAfter(endDate))
                .sorted(Comparator.comparing(HistoricalMarketDataService.PricePoint::date))
                .toList();
    }

    private boolean coversRequestedRange(
            List<PriceHistory> stored,
            LocalDate startDate,
            LocalDate endDate
    ) {
        if (stored.isEmpty()) {
            return false;
        }
        LocalDate first = stored.get(0).getPriceDate();
        LocalDate last = stored.get(stored.size() - 1).getPriceDate();
        return !first.isAfter(startDate.plusDays(MARKET_CLOSURE_TOLERANCE_DAYS))
                && !last.isBefore(endDate.minusDays(MARKET_CLOSURE_TOLERANCE_DAYS));
    }

    private static HistoricalMarketDataService.PricePoint toPoint(PriceHistory price) {
        return new HistoricalMarketDataService.PricePoint(
                price.getSymbol(),
                price.getCurrency(),
                price.getPriceDate(),
                price.getClosePrice()
        );
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}

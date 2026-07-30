package com.hsbc.portfoliomanager.marketdata;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
class TwelveDataPriceService {

    private static final Logger log = LoggerFactory.getLogger(TwelveDataPriceService.class);

    private static final String QUOTE_URL =
            "https://api.twelvedata.com/quote?symbol={symbol}&apikey={apikey}";
    private static final String EXCHANGE_QUOTE_URL =
            "https://api.twelvedata.com/quote?symbol={symbol}&exchange={exchange}&apikey={apikey}";

    private final RestTemplate restTemplate;
    private final MarketDataConfig config;

    private final Map<String, CachedPrice> cache = new ConcurrentHashMap<>();
    private static final long CACHE_TTL_MILLIS = 60_000; // 1 minute cache

    /**
     * 中文：注入市场数据 HTTP 客户端和 API 配置。
     * English: Injects the market data HTTP client and API configuration.
     */
    TwelveDataPriceService(RestTemplate restTemplate, MarketDataConfig config) {
        this.restTemplate = restTemplate;
        this.config = config;
    }

    /**
     * 中文：优先从短期缓存读取价格，未命中时调用 Twelve Data quote 接口。
     * English: Reads a price from the short-lived cache or calls the Twelve Data quote endpoint on a cache miss.
     */
    Optional<MarketDataService.PriceData> fetchPrice(String symbol) {
        return fetchPrice(symbol, null);
    }

    /**
     * 中文：按资产代码和可选交易所查询价格，并使用组合键缓存结果。
     * English: Fetches price by symbol and optional exchange, caching it with a composite key.
     */
    Optional<MarketDataService.PriceData> fetchPrice(String symbol, String exchange) {
        String cacheKey = exchange == null || exchange.isBlank()
                ? symbol
                : symbol + "|" + exchange;
        // Check cache first
        CachedPrice cached = cache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            log.debug("Cache hit for symbol: {}", symbol);
            return Optional.of(cached.priceData);
        }

        String apiKey = config.getTwelveDataApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("TwelveData API key is not configured");
            return Optional.empty();
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = exchange == null || exchange.isBlank()
                    ? restTemplate.getForObject(QUOTE_URL, Map.class, symbol, apiKey)
                    : restTemplate.getForObject(
                            EXCHANGE_QUOTE_URL,
                            Map.class,
                            symbol,
                            exchange,
                            apiKey
                    );

            if (response == null) {
                log.warn("Empty response from TwelveData for symbol: {}", symbol);
                return Optional.empty();
            }

            // Check for API-level errors
            if (response.containsKey("code") && response.containsKey("message")) {
                log.warn("TwelveData API error for {}: {}", symbol, response.get("message"));
                return Optional.empty();
            }

            Object priceValue = response.get("close");
            Object previousCloseValue = response.get("previous_close");
            String currency = (String) response.get("currency");

            if (priceValue == null) {
                log.warn("No price data in TwelveData response for symbol: {}", symbol);
                return Optional.empty();
            }

            BigDecimal price = new BigDecimal(priceValue.toString());
            BigDecimal previousClose = previousCloseValue == null
                    ? null
                    : new BigDecimal(previousCloseValue.toString());
            if (currency == null) {
                currency = "USD"; // default assumption
            }

            MarketDataService.PriceData priceData = new MarketDataService.PriceData(
                    symbol,
                    price,
                    previousClose,
                    currency,
                    Instant.now()
            );

            cache.put(cacheKey, new CachedPrice(priceData, Instant.now().toEpochMilli()));

            return Optional.of(priceData);

        } catch (RestClientException e) {
            log.error("Failed to fetch price from TwelveData for symbol {}: {}", symbol, e.getMessage());
            return Optional.empty();
        } catch (NumberFormatException e) {
            log.error("Failed to parse price for symbol {}: {}", symbol, e.getMessage());
            return Optional.empty();
        }
    }

    private record CachedPrice(MarketDataService.PriceData priceData, long fetchedAtMillis) {
        /**
         * 中文：判断当前价格缓存是否已经超过有效期。
         * English: Determines whether the cached current price has exceeded its time-to-live.
         */
        boolean isExpired() {
            return System.currentTimeMillis() - fetchedAtMillis > CACHE_TTL_MILLIS;
        }
    }
}

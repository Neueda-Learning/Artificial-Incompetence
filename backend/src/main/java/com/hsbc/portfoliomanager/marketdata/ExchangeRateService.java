package com.hsbc.portfoliomanager.marketdata;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
class ExchangeRateService {

    private static final Logger log = LoggerFactory.getLogger(ExchangeRateService.class);

    private static final String LATEST_URL =
            "https://openexchangerates.org/api/latest.json?app_id={appId}";

    private final RestTemplate restTemplate;
    private final MarketDataConfig config;

    private volatile CachedRates cachedRates;
    private static final long CACHE_TTL_MILLIS = 300_000; // 5 minutes

    /**
     * 中文：注入市场数据 HTTP 客户端和汇率 API 配置。
     * English: Injects the market data HTTP client and exchange-rate API configuration.
     */
    ExchangeRateService(RestTemplate restTemplate, MarketDataConfig config) {
        this.restTemplate = restTemplate;
        this.config = config;
    }

    /**
     * 中文：使用以 USD 为基准的最新汇率，将指定金额转换为美元。
     * English: Converts an amount into USD using the latest USD-based exchange rates.
     */
    Optional<BigDecimal> convertToUsd(BigDecimal amount, String fromCurrency) {
        if (fromCurrency == null) {
            return Optional.empty();
        }

        String upper = fromCurrency.toUpperCase();
        if ("USD".equals(upper)) {
            return Optional.of(amount);
        }

        Optional<Map<String, BigDecimal>> rates = getRates();
        if (rates.isEmpty()) {
            return Optional.empty();
        }

        BigDecimal rate = rates.get().get(upper);
        if (rate == null) {
            log.warn("No exchange rate found for currency: {}", upper);
            return Optional.empty();
        }

        // Open Exchange Rates free tier: base is always USD
        // To convert FROM other currency TO USD: divide by the rate
        // Example: £100 at GBP rate 0.79 → 100 / 0.79 = $126.58
        return Optional.of(amount.divide(rate, 6, RoundingMode.HALF_UP));
    }

    /**
     * 中文：优先返回有效缓存，否则从 Open Exchange Rates 获取最新汇率表。
     * English: Returns valid cached rates or fetches the latest rate table from Open Exchange Rates.
     */
    private Optional<Map<String, BigDecimal>> getRates() {
        CachedRates cached = this.cachedRates;
        if (cached != null && !cached.isExpired()) {
            return Optional.of(cached.rates);
        }

        String apiKey = config.getOpenExchangeRatesApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Open Exchange Rates API key is not configured");
            return Optional.empty();
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(
                    LATEST_URL, Map.class, apiKey);

            if (response == null) {
                log.warn("Empty response from Open Exchange Rates");
                return Optional.empty();
            }

            if (response.containsKey("error")) {
                log.warn("Open Exchange Rates API error: {}", response.get("description"));
                return Optional.empty();
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> rawRates = (Map<String, Object>) response.get("rates");
            if (rawRates == null) {
                log.warn("No rates in Open Exchange Rates response");
                return Optional.empty();
            }

            Map<String, BigDecimal> rates = new ConcurrentHashMap<>();
            rawRates.forEach((currency, value) -> {
                if (value instanceof Number num) {
                    rates.put(currency, BigDecimal.valueOf(num.doubleValue()));
                }
            });

            this.cachedRates = new CachedRates(rates, System.currentTimeMillis());
            return Optional.of(rates);

        } catch (RestClientException e) {
            log.error("Failed to fetch exchange rates: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private record CachedRates(Map<String, BigDecimal> rates, long fetchedAtMillis) {
        /**
         * 中文：判断汇率缓存是否已经超过有效期。
         * English: Determines whether the cached exchange rates have exceeded their time-to-live.
         */
        boolean isExpired() {
            return System.currentTimeMillis() - fetchedAtMillis > CACHE_TTL_MILLIS;
        }
    }
}

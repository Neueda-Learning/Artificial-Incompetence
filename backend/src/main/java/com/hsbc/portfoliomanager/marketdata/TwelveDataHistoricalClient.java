package com.hsbc.portfoliomanager.marketdata;

import com.hsbc.portfoliomanager.portfolio.holding.AssetType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
class TwelveDataHistoricalClient {

    private static final Logger log = LoggerFactory.getLogger(TwelveDataHistoricalClient.class);
    private static final String TIME_SERIES_URL = "https://api.twelvedata.com/time_series";

    private final RestTemplate restTemplate;
    private final MarketDataConfig config;

    /**
     * 中文：注入 HTTP 客户端和 Twelve Data 配置。
     * English: Injects the HTTP client and Twelve Data configuration.
     */
    TwelveDataHistoricalClient(RestTemplate restTemplate, MarketDataConfig config) {
        this.restTemplate = restTemplate;
        this.config = config;
    }

    /**
     * 中文：调用 Twelve Data time_series 接口并解析指定区间的每日价格。
     * English: Calls the Twelve Data time_series endpoint and parses daily prices for a date range.
     */
    List<HistoricalPrice> fetchDailyPrices(
            AssetType assetType,
            String symbol,
            String exchange,
            String fallbackCurrency,
            LocalDate startDate,
            LocalDate endDate
    ) {
        String apiKey = config.getTwelveDataApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Twelve Data API key is not configured for historical prices");
            return List.of();
        }

        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(TIME_SERIES_URL)
                .queryParam("symbol", symbol)
                .queryParam("interval", "1day")
                .queryParam("start_date", startDate)
                .queryParam("end_date", endDate)
                .queryParam("order", "ASC")
                .queryParam("format", "JSON")
                .queryParam("apikey", apiKey);
        if (exchange != null && !exchange.isBlank()) {
            builder.queryParam("exchange", exchange);
        }
        URI uri = builder.build().encode().toUri();

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(uri, Map.class);
            if (response == null || response.containsKey("code") || "error".equals(response.get("status"))) {
                log.warn("Twelve Data historical request failed for {}: {}",
                        symbol, response == null ? "empty response" : response.get("message"));
                return List.of();
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> meta = (Map<String, Object>) response.get("meta");
            String responseCurrency = normalizedValue(
                    meta == null ? null : meta.get("currency"),
                    fallbackCurrency,
                    3
            );
            String responseExchange = normalizedValue(
                    meta == null ? null : meta.get("exchange"),
                    exchange,
                    50
            );

            Object rawValues = response.get("values");
            if (!(rawValues instanceof List<?> values)) {
                return List.of();
            }

            List<HistoricalPrice> result = new ArrayList<>();
            for (Object rawValue : values) {
                if (!(rawValue instanceof Map<?, ?> value)) {
                    continue;
                }
                LocalDate date = parseDate(value.get("datetime"));
                BigDecimal close = decimal(value.get("close"));
                if (date == null || close == null) {
                    continue;
                }
                result.add(new HistoricalPrice(
                        assetType,
                        symbol,
                        responseExchange,
                        responseCurrency,
                        date,
                        decimal(value.get("open")),
                        decimal(value.get("high")),
                        decimal(value.get("low")),
                        close,
                        decimal(value.get("volume"))
                ));
            }
            return result;
        } catch (RestClientException | IllegalArgumentException exception) {
            log.warn("Unable to fetch Twelve Data history for {}: {}", symbol, exception.getMessage());
            return List.of();
        }
    }

    /**
     * 中文：从第三方响应值中解析 ISO 日期，格式无效时返回空值。
     * English: Parses an ISO date from an upstream value, returning null when the format is invalid.
     */
    private static LocalDate parseDate(Object value) {
        if (value == null) {
            return null;
        }
        String text = value.toString();
        if (text.length() < 10) {
            return null;
        }
        return LocalDate.parse(text.substring(0, 10));
    }

    /**
     * 中文：将第三方响应值转换为高精度小数，缺失时返回空值。
     * English: Converts an upstream response value to a decimal, returning null when absent.
     */
    private static BigDecimal decimal(Object value) {
        if (value == null || value.toString().isBlank()) {
            return null;
        }
        return new BigDecimal(value.toString());
    }

    /**
     * 中文：优先使用接口返回值，否则使用备用值，并标准化和限制长度。
     * English: Prefers the API value, falls back when needed, then normalizes and limits its length.
     */
    private static String normalizedValue(Object preferred, String fallback, int maxLength) {
        String value = preferred == null || preferred.toString().isBlank()
                ? fallback
                : preferred.toString();
        if (value == null) {
            value = "";
        }
        value = value.trim().toUpperCase(Locale.ROOT);
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    record HistoricalPrice(
            AssetType assetType,
            String symbol,
            String exchange,
            String currency,
            LocalDate date,
            BigDecimal open,
            BigDecimal high,
            BigDecimal low,
            BigDecimal close,
            BigDecimal volume
    ) {
    }
}

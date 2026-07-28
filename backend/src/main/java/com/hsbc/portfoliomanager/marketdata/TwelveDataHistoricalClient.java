package com.hsbc.portfoliomanager.marketdata;

import com.hsbc.portfoliomanager.portfolio.AssetType;
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

    TwelveDataHistoricalClient(RestTemplate restTemplate, MarketDataConfig config) {
        this.restTemplate = restTemplate;
        this.config = config;
    }

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

    private static BigDecimal decimal(Object value) {
        if (value == null || value.toString().isBlank()) {
            return null;
        }
        return new BigDecimal(value.toString());
    }

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

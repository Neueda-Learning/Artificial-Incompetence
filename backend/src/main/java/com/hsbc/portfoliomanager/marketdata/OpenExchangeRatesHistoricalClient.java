package com.hsbc.portfoliomanager.marketdata;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

@Component
class OpenExchangeRatesHistoricalClient {

    private static final Logger log = LoggerFactory.getLogger(OpenExchangeRatesHistoricalClient.class);
    private static final String HISTORICAL_URL =
            "https://openexchangerates.org/api/historical/{date}.json";

    private final RestTemplate restTemplate;
    private final MarketDataConfig config;

    /**
     * 中文：注入 HTTP 客户端和 Open Exchange Rates 配置。
     * English: Injects the HTTP client and Open Exchange Rates configuration.
     */
    OpenExchangeRatesHistoricalClient(RestTemplate restTemplate, MarketDataConfig config) {
        this.restTemplate = restTemplate;
        this.config = config;
    }

    /**
     * 中文：查询指定日期的历史汇率，并将 USD 基准汇率换算成目标币种兑 USD。
     * English: Fetches a dated rate and inverts the USD-base quote into the currency-to-USD rate.
     */
    Optional<HistoricalUsdRate> fetchRateToUsd(String fromCurrency, LocalDate date) {
        String apiKey = config.getOpenExchangeRatesApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Open Exchange Rates API key is not configured for historical rates");
            return Optional.empty();
        }

        URI uri = UriComponentsBuilder.fromUriString(HISTORICAL_URL)
                .queryParam("app_id", apiKey)
                .queryParam("symbols", fromCurrency)
                .buildAndExpand(date)
                .encode()
                .toUri();

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(uri, Map.class);
            if (response == null || Boolean.TRUE.equals(response.get("error"))) {
                log.warn("Open Exchange Rates historical request failed for {} on {}",
                        fromCurrency, date);
                return Optional.empty();
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> rates = (Map<String, Object>) response.get("rates");
            if (rates == null || rates.get(fromCurrency) == null) {
                return Optional.empty();
            }

            BigDecimal usdToCurrency = new BigDecimal(rates.get(fromCurrency).toString());
            if (usdToCurrency.signum() <= 0) {
                return Optional.empty();
            }

            BigDecimal currencyToUsd = BigDecimal.ONE.divide(
                    usdToCurrency,
                    12,
                    RoundingMode.HALF_UP
            );
            Instant sourceTimestamp = response.get("timestamp") instanceof Number timestamp
                    ? Instant.ofEpochSecond(timestamp.longValue())
                    : null;

            return Optional.of(new HistoricalUsdRate(
                    fromCurrency,
                    date,
                    currencyToUsd,
                    sourceTimestamp
            ));
        } catch (RestClientException | IllegalArgumentException exception) {
            log.warn("Unable to fetch historical exchange rate for {} on {}: {}",
                    fromCurrency, date, exception.getMessage());
            return Optional.empty();
        }
    }

    record HistoricalUsdRate(
            String fromCurrency,
            LocalDate date,
            BigDecimal rateToUsd,
            Instant sourceTimestamp
    ) {
    }
}

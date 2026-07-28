package com.hsbc.portfoliomanager.marketdata;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class MarketDataConfig {

    @Value("${marketdata.twelvedata.api-key:}")
    private String twelveDataApiKey;

    @Value("${marketdata.openexchangerates.api-key:}")
    private String openExchangeRatesApiKey;

    @Bean
    public RestTemplate marketDataRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(10));
        return new RestTemplate(factory);
    }

    public String getTwelveDataApiKey() {
        return twelveDataApiKey;
    }

    public String getOpenExchangeRatesApiKey() {
        return openExchangeRatesApiKey;
    }
}

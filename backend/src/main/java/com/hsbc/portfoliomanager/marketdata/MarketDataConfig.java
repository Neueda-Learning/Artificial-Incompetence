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

    /**
     * 中文：创建带连接超时和读取超时设置的市场数据 HTTP 客户端。
     * English: Creates the market data HTTP client with connection and read timeouts.
     */
    @Bean
    public RestTemplate marketDataRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(10));
        return new RestTemplate(factory);
    }

    /**
     * 中文：返回 Twelve Data API 密钥。
     * English: Returns the configured Twelve Data API key.
     */
    public String getTwelveDataApiKey() {
        return twelveDataApiKey;
    }

    /**
     * 中文：返回 Open Exchange Rates API 密钥。
     * English: Returns the configured Open Exchange Rates API key.
     */
    public String getOpenExchangeRatesApiKey() {
        return openExchangeRatesApiKey;
    }
}

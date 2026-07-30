package com.hsbc.portfoliomanager.portfolio.holding;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("twelve-data")
record TwelveDataProperties(
        String baseUrl,
        String apiKey
) {
}

package com.hsbc.portfoliomanager.portfolio;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("twelve-data")
record TwelveDataProperties(
        String baseUrl,
        String apiKey
) {
}

package com.hsbc.portfoliomanager.portfolio;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Locale;

@Component
class TwelveDataAssetMetadataClient implements AssetMetadataClient {

    private final RestClient restClient;
    private final TwelveDataProperties properties;

    TwelveDataAssetMetadataClient(TwelveDataProperties properties) {
        this.restClient = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .build();
        this.properties = properties;
    }

    @Override
    public AssetMetadata findBySymbol(String symbol) {
        if (!StringUtils.hasText(properties.apiKey())) {
            throw new AssetMetadataLookupException(
                    "Twelve Data API key is not configured"
            );
        }

        try {
            TwelveDataQuoteResponse quote = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/quote")
                            .queryParam("symbol", symbol)
                            .build())
                    .header(
                            HttpHeaders.AUTHORIZATION,
                            "apikey " + properties.apiKey()
                    )
                    .retrieve()
                    .body(TwelveDataQuoteResponse.class);

            if (quote == null
                    || !StringUtils.hasText(quote.name())
                    || !StringUtils.hasText(quote.exchange())
                    || !StringUtils.hasText(quote.currency())) {
                throw new AssetMetadataLookupException(
                        "Twelve Data returned incomplete metadata for " + symbol
                );
            }

            return new AssetMetadata(
                    quote.name().trim(),
                    quote.exchange().trim().toUpperCase(Locale.ROOT),
                    quote.currency().trim().toUpperCase(Locale.ROOT)
            );
        } catch (AssetMetadataLookupException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new AssetMetadataLookupException(
                    "Unable to retrieve metadata for " + symbol,
                    exception
            );
        }
    }
}

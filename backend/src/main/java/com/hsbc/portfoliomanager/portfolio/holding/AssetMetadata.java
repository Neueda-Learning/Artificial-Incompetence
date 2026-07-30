package com.hsbc.portfoliomanager.portfolio.holding;

public record AssetMetadata(
        String symbol,
        String companyName,
        String exchange,
        String currency
) {
}

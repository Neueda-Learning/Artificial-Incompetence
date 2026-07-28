package com.hsbc.portfoliomanager.portfolio;

import java.math.BigDecimal;

record PortfolioItemResponse(
        Long id,
        AssetType assetType,
        String symbol,
        String companyName,
        String exchange,
        BigDecimal quantity,
        String currency
) {
    static PortfolioItemResponse from(PortfolioItem item) {
        return new PortfolioItemResponse(
                item.getId(),
                item.getAssetType(),
                item.getSymbol(),
                item.getCompanyName(),
                item.getExchange(),
                item.getQuantity(),
                item.getCurrency()
        );
    }
}

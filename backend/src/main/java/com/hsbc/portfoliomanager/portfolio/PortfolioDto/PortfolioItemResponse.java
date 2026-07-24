package com.hsbc.portfoliomanager.portfolio;

import java.math.BigDecimal;

record PortfolioItemResponse(
        Long id,
        AssetType assetType,
        String symbol,
        BigDecimal quantity
) {
    static PortfolioItemResponse from(PortfolioItem item) {
        return new PortfolioItemResponse(
                item.getId(),
                item.getAssetType(),
                item.getSymbol(),
                item.getQuantity()
        );
    }
}


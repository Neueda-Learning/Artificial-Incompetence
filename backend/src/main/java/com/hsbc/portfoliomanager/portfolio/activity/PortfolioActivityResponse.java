package com.hsbc.portfoliomanager.portfolio.activity;

import com.hsbc.portfoliomanager.portfolio.holding.AssetType;

import java.math.BigDecimal;
import java.time.Instant;

record PortfolioActivityResponse(
        Long id,
        PortfolioActivityAction action,
        AssetType assetType,
        String symbol,
        BigDecimal quantity,
        BigDecimal pricePerUnit,
        String currency,
        BigDecimal remainingQuantity,
        Instant occurredAt
) {
    static PortfolioActivityResponse from(PortfolioActivity activity) {
        return new PortfolioActivityResponse(
                activity.getId(),
                activity.getAction(),
                activity.getAssetType(),
                activity.getSymbol(),
                activity.getQuantity(),
                activity.getPricePerUnit(),
                activity.getCurrency(),
                activity.getRemainingQuantity(),
                activity.getOccurredAt()
        );
    }
}

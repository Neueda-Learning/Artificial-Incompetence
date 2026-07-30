package com.hsbc.portfoliomanager.portfolio.holding;

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
    /**
     * 中文：将数据库持仓实体转换为对外返回的 DTO。
     * English: Converts a persisted portfolio holding entity into its API response DTO.
     */
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

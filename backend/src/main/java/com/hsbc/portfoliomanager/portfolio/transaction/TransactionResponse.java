package com.hsbc.portfoliomanager.portfolio.transaction;

import com.hsbc.portfoliomanager.portfolio.holding.AssetType;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 中文：交易历史响应模型，字段与用户故事验收标准保持一致。
 * English: Response model for transaction history; fields align with user-story acceptance criteria.
 */
record TransactionResponse(
        Long id,
        AssetType assetType,
        String symbol,
        BigDecimal quantity,
        BigDecimal pricePerUnit,
        String currency,
        Instant purchasedAt
) {
    /**
     * 中文：将实体对象映射为对外响应，隔离持久化字段命名与 API 字段命名差异。
     * English: Maps entity to API response, decoupling persistence details from external contract.
     */
    static TransactionResponse from(TransactionRecord transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getAssetType(),
                transaction.getSymbol(),
                transaction.getQuantity(),
                transaction.getPricePerUnit(),
                transaction.getCurrency(),
                transaction.getTransactedAt()
        );
    }
}

package com.hsbc.portfoliomanager.portfolio.activity;

import com.hsbc.portfoliomanager.portfolio.holding.AssetType;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 中文：提供给交易、持仓和分析模块使用的只读投资组合流水，避免其他功能直接依赖 JPA 实体。
 * English: Read-only portfolio ledger entry shared with transaction, holding, and analytics features
 * without exposing the JPA entity.
 */
public record PortfolioLedgerEntry(
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
    /**
     * 中文：创建尚未持久化的流水值，主要用于业务计算和测试夹具。
     * English: Creates an unpersisted ledger value for business calculations and test fixtures.
     */
    public PortfolioLedgerEntry(
            PortfolioActivityAction action,
            AssetType assetType,
            String symbol,
            BigDecimal quantity,
            BigDecimal pricePerUnit,
            String currency,
            Instant occurredAt
    ) {
        this(
                null,
                action,
                assetType,
                symbol,
                quantity,
                pricePerUnit,
                currency,
                null,
                occurredAt
        );
    }

    /**
     * 中文：将内部活动实体转换成跨功能使用的只读流水。
     * English: Converts the internal activity entity into a read-only cross-feature ledger entry.
     */
    static PortfolioLedgerEntry from(PortfolioActivity activity) {
        return new PortfolioLedgerEntry(
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

package com.hsbc.portfoliomanager.portfolio;

import jakarta.persistence.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "portfolio_items")
class PortfolioItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_type", nullable = false, length = 20)
    private AssetType assetType;

    @Column(nullable = false, length = 20)
    private String symbol;

    @Column(name = "company_name", length = 255)
    private String companyName;

    @Column(length = 50)
    private String exchange;

    @Column(nullable = false, precision = 19, scale = 6)
    private BigDecimal quantity;

    @Column(nullable = false, length = 3)
    private String currency;

    protected PortfolioItem() {
    }

    /**
     * 中文：默认以 USD 创建持仓，兼容 US-01/US-02 的既有行为。
     * English: Creates a portfolio item with default USD currency to preserve existing behavior.
     */
    PortfolioItem(AssetType assetType, String symbol, BigDecimal quantity) {
        this(assetType, symbol, quantity, "USD");
    }

    /**
     * 中文：显式指定币种创建持仓，用于交易入账时与交易币种保持一致。
     * English: Creates a portfolio item with explicit currency so holdings stay aligned with transaction currency.
     */
    PortfolioItem(AssetType assetType, String symbol, BigDecimal quantity, String currency) {
        this(assetType, symbol, null, null, quantity, currency);
    }

    PortfolioItem(
            AssetType assetType,
            String symbol,
            String companyName,
            String exchange,
            BigDecimal quantity,
            String currency
    ) {
        this.assetType = assetType;
        this.symbol = symbol;
        this.companyName = companyName;
        this.exchange = exchange;
        this.quantity = quantity;
        this.currency = currency;
    }

    Long getId() {
        return id;
    }

    AssetType getAssetType() {
        return assetType;
    }

    String getSymbol() {
        return symbol;
    }

    String getCompanyName() {
        return companyName;
    }

    String getExchange() {
        return exchange;
    }

    BigDecimal getQuantity() {
        return quantity;
    }

    String getCurrency() {
        return currency;
    }

    /**
     * 中文：在同一资产重复买入时累加数量，保证持仓是“当前快照”而不是“逐笔明细”。
     * English: Adds quantity for repeated buys so portfolio_items remains a current-position snapshot.
     */
    void addQuantity(BigDecimal additionalQuantity) {
        this.quantity = this.quantity.add(additionalQuantity);
    }
}

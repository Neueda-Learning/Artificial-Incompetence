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
}

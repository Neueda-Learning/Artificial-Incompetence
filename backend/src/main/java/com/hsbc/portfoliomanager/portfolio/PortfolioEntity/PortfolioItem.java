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

    @Column(nullable = false, precision = 19, scale = 6)
    private BigDecimal quantity;

    @Column(nullable = false, length = 3)
    private String currency;

    protected PortfolioItem() {
    }

    PortfolioItem(AssetType assetType, String symbol, BigDecimal quantity) {
        this.assetType = assetType;
        this.symbol = symbol;
        this.quantity = quantity;
        this.currency = "USD";
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

    BigDecimal getQuantity() {
        return quantity;
    }

    String getCurrency() {
        return currency;
    }
}

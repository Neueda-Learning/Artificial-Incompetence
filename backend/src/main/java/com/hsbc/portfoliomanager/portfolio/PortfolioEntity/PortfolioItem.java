package com.hsbc.portfoliomanager.portfolio;

import jakarta.persistence.Entity;
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
    private AssetType assetType;

    private String symbol;

    private BigDecimal quantity;

    protected PortfolioItem() {
    }

    PortfolioItem(AssetType assetType, String symbol, BigDecimal quantity) {
        this.assetType = assetType;
        this.symbol = symbol;
        this.quantity = quantity;
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
}


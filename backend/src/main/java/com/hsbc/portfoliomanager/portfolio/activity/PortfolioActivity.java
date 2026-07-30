package com.hsbc.portfoliomanager.portfolio.activity;

import com.hsbc.portfoliomanager.portfolio.holding.AssetType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "portfolio_activities")
class PortfolioActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 10)
    private PortfolioActivityAction action;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_type", nullable = false, length = 20)
    private AssetType assetType;

    @Column(nullable = false, length = 20)
    private String symbol;

    @Column(nullable = false, precision = 19, scale = 6)
    private BigDecimal quantity;

    @Column(name = "price_per_unit", precision = 19, scale = 4)
    private BigDecimal pricePerUnit;

    @Column(length = 3)
    private String currency;

    @Column(name = "remaining_quantity", precision = 19, scale = 6)
    private BigDecimal remainingQuantity;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected PortfolioActivity() {
    }

    PortfolioActivity(
            PortfolioActivityAction action,
            AssetType assetType,
            String symbol,
            BigDecimal quantity,
            BigDecimal pricePerUnit,
            String currency,
            BigDecimal remainingQuantity,
            Instant occurredAt
    ) {
        this.action = action;
        this.assetType = assetType;
        this.symbol = symbol;
        this.quantity = quantity;
        this.pricePerUnit = pricePerUnit;
        this.currency = currency;
        this.remainingQuantity = remainingQuantity;
        this.occurredAt = occurredAt;
    }

    Long getId() {
        return id;
    }

    PortfolioActivityAction getAction() {
        return action;
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

    BigDecimal getPricePerUnit() {
        return pricePerUnit;
    }

    String getCurrency() {
        return currency;
    }

    BigDecimal getRemainingQuantity() {
        return remainingQuantity;
    }

    Instant getOccurredAt() {
        return occurredAt;
    }
}

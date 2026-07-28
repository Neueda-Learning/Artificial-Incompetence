package com.hsbc.portfoliomanager.transaction;

import com.hsbc.portfoliomanager.portfolio.AssetType;
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
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType transactionType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AssetType assetType;

    @Column(nullable = false)
    private String symbol;

    @Column(nullable = false, precision = 20, scale = 8)
    private BigDecimal quantity;

    @Column(nullable = false, precision = 20, scale = 4)
    private BigDecimal pricePerUnit;

    @Column(nullable = false)
    private String currency;

    @Column(nullable = false)
    private Instant purchasedAt;

    protected Transaction() {
    }

    public Transaction(TransactionType transactionType, AssetType assetType, String symbol,
                       BigDecimal quantity, BigDecimal pricePerUnit, String currency,
                       Instant purchasedAt) {
        this.transactionType = transactionType;
        this.assetType = assetType;
        this.symbol = symbol;
        this.quantity = quantity;
        this.pricePerUnit = pricePerUnit;
        this.currency = currency;
        this.purchasedAt = purchasedAt;
    }

    public Long getId() {
        return id;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public AssetType getAssetType() {
        return assetType;
    }

    public String getSymbol() {
        return symbol;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public BigDecimal getPricePerUnit() {
        return pricePerUnit;
    }

    public String getCurrency() {
        return currency;
    }

    public Instant getPurchasedAt() {
        return purchasedAt;
    }
}

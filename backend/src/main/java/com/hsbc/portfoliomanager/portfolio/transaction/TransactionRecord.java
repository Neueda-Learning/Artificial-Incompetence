package com.hsbc.portfoliomanager.portfolio.transaction;

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
@Table(name = "transactions")
public class TransactionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 10)
    private TransactionType transactionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_type", nullable = false, length = 20)
    private AssetType assetType;

    @Column(nullable = false, length = 20)
    private String symbol;

    @Column(nullable = false, precision = 19, scale = 6)
    private BigDecimal quantity;

    @Column(name = "price_per_unit", nullable = false, precision = 19, scale = 4)
    private BigDecimal pricePerUnit;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "transacted_at", nullable = false)
    private Instant transactedAt;

    protected TransactionRecord() {
    }

    /**
     * 中文：交易实体构造器，对应一笔不可变业务事件（购买/卖出），用于落库保存历史。
     * English: Transaction entity constructor representing one business event (buy/sell) persisted as history.
     */
    public TransactionRecord(
            TransactionType transactionType,
            AssetType assetType,
            String symbol,
            BigDecimal quantity,
            BigDecimal pricePerUnit,
            String currency,
            Instant transactedAt
    ) {
        this.transactionType = transactionType;
        this.assetType = assetType;
        this.symbol = symbol;
        this.quantity = quantity;
        this.pricePerUnit = pricePerUnit;
        this.currency = currency;
        this.transactedAt = transactedAt;
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

    public Instant getTransactedAt() {
        return transactedAt;
    }
}

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

    /**
     * 中文：JPA 专用无参构造器，用于从数据库记录还原交易实体。
     * English: JPA-only no-argument constructor used to restore a transaction entity from a database row.
     */
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

    /**
     * 中文：返回交易记录的数据库主键。
     * English: Returns the database identifier of this transaction record.
     */
    public Long getId() {
        return id;
    }

    /**
     * 中文：返回交易类型。
     * English: Returns the transaction type.
     */
    public TransactionType getTransactionType() {
        return transactionType;
    }

    /**
     * 中文：返回交易资产类型。
     * English: Returns the asset type involved in the transaction.
     */
    public AssetType getAssetType() {
        return assetType;
    }

    /**
     * 中文：返回交易资产代码。
     * English: Returns the symbol of the transacted asset.
     */
    public String getSymbol() {
        return symbol;
    }

    /**
     * 中文：返回本次交易数量。
     * English: Returns the quantity traded in this transaction.
     */
    public BigDecimal getQuantity() {
        return quantity;
    }

    /**
     * 中文：返回本次交易的每单位成交价格。
     * English: Returns the executed price per unit for this transaction.
     */
    public BigDecimal getPricePerUnit() {
        return pricePerUnit;
    }

    /**
     * 中文：返回本次交易的计价币种。
     * English: Returns the currency in which this transaction was priced.
     */
    public String getCurrency() {
        return currency;
    }

    /**
     * 中文：返回交易发生时间。
     * English: Returns the time at which the transaction occurred.
     */
    public Instant getTransactedAt() {
        return transactedAt;
    }
}

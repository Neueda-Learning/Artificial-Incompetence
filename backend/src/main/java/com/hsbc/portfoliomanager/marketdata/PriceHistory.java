package com.hsbc.portfoliomanager.marketdata;

import com.hsbc.portfoliomanager.portfolio.holding.AssetType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(
        name = "price_history",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_price_history_point",
                columnNames = {
                        "asset_type", "symbol", "exchange_name", "currency",
                        "price_date", "time_interval"
                }
        )
)
class PriceHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_type", nullable = false, length = 20)
    private AssetType assetType;

    @Column(nullable = false, length = 20)
    private String symbol;

    @Column(name = "exchange_name", nullable = false, length = 50)
    private String exchange;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "price_date", nullable = false)
    private LocalDate priceDate;

    @Column(name = "time_interval", nullable = false, length = 20)
    private String timeInterval;

    @Column(name = "open_price", precision = 19, scale = 6)
    private BigDecimal openPrice;

    @Column(name = "high_price", precision = 19, scale = 6)
    private BigDecimal highPrice;

    @Column(name = "low_price", precision = 19, scale = 6)
    private BigDecimal lowPrice;

    @Column(name = "close_price", nullable = false, precision = 19, scale = 6)
    private BigDecimal closePrice;

    @Column(precision = 24, scale = 6)
    private BigDecimal volume;

    @Column(nullable = false, length = 30)
    private String source;

    @Column(name = "source_updated_at")
    private Instant sourceUpdatedAt;

    @Column(name = "fetched_at", nullable = false)
    private Instant fetchedAt;

    /**
     * 中文：JPA 专用无参构造器，用于从数据库记录还原历史价格实体。
     * English: JPA-only no-argument constructor used to restore historical price entities.
     */
    protected PriceHistory() {
    }

    /**
     * 中文：使用完整行情字段创建一条待保存的历史价格记录。
     * English: Creates a historical price record from a complete set of market-data fields.
     */
    PriceHistory(
            AssetType assetType,
            String symbol,
            String exchange,
            String currency,
            LocalDate priceDate,
            String timeInterval,
            BigDecimal openPrice,
            BigDecimal highPrice,
            BigDecimal lowPrice,
            BigDecimal closePrice,
            BigDecimal volume,
            String source,
            Instant sourceUpdatedAt
    ) {
        this.assetType = assetType;
        this.symbol = symbol;
        this.exchange = exchange;
        this.currency = currency;
        this.priceDate = priceDate;
        this.timeInterval = timeInterval;
        this.openPrice = openPrice;
        this.highPrice = highPrice;
        this.lowPrice = lowPrice;
        this.closePrice = closePrice;
        this.volume = volume;
        this.source = source;
        this.sourceUpdatedAt = sourceUpdatedAt;
        this.fetchedAt = Instant.now();
    }

    /**
     * 中文：返回资产类型。
     * English: Returns the asset type.
     */
    AssetType getAssetType() {
        return assetType;
    }

    /**
     * 中文：返回资产代码。
     * English: Returns the asset symbol.
     */
    String getSymbol() {
        return symbol;
    }

    /**
     * 中文：返回交易所名称。
     * English: Returns the exchange name.
     */
    String getExchange() {
        return exchange;
    }

    /**
     * 中文：返回价格的计价币种。
     * English: Returns the price currency.
     */
    String getCurrency() {
        return currency;
    }

    /**
     * 中文：返回该行情点对应的日期。
     * English: Returns the date represented by this market-data point.
     */
    LocalDate getPriceDate() {
        return priceDate;
    }

    /**
     * 中文：返回该日期的收盘价格。
     * English: Returns the closing price for the date.
     */
    BigDecimal getClosePrice() {
        return closePrice;
    }
}

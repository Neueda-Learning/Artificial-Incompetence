package com.hsbc.portfoliomanager.marketdata;

import com.hsbc.portfoliomanager.portfolio.AssetType;
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

    protected PriceHistory() {
    }

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

    AssetType getAssetType() {
        return assetType;
    }

    String getSymbol() {
        return symbol;
    }

    String getExchange() {
        return exchange;
    }

    String getCurrency() {
        return currency;
    }

    LocalDate getPriceDate() {
        return priceDate;
    }

    BigDecimal getClosePrice() {
        return closePrice;
    }
}

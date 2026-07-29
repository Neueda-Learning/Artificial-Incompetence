package com.hsbc.portfoliomanager.marketdata;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
        name = "exchange_rate_history",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_exchange_rate_history_point",
                columnNames = {"base_currency", "quote_currency", "rate_date"}
        )
)
class ExchangeRateHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "base_currency", nullable = false, length = 3)
    private String baseCurrency;

    @Column(name = "quote_currency", nullable = false, length = 3)
    private String quoteCurrency;

    @Column(name = "rate_date", nullable = false)
    private LocalDate rateDate;

    @Column(name = "exchange_rate", nullable = false, precision = 24, scale = 12)
    private BigDecimal exchangeRate;

    @Column(nullable = false, length = 30)
    private String source;

    @Column(name = "source_timestamp")
    private Instant sourceTimestamp;

    @Column(name = "fetched_at", nullable = false)
    private Instant fetchedAt;

    /**
     * 中文：JPA 专用无参构造器，用于从数据库记录还原汇率实体。
     * English: JPA-only no-argument constructor used to restore exchange-rate entities.
     */
    protected ExchangeRateHistory() {
    }

    /**
     * 中文：使用币种、日期、汇率和来源信息创建历史汇率记录。
     * English: Creates a historical exchange-rate record with currency, date, rate, and source data.
     */
    ExchangeRateHistory(
            String baseCurrency,
            String quoteCurrency,
            LocalDate rateDate,
            BigDecimal exchangeRate,
            String source,
            Instant sourceTimestamp
    ) {
        this.baseCurrency = baseCurrency;
        this.quoteCurrency = quoteCurrency;
        this.rateDate = rateDate;
        this.exchangeRate = exchangeRate;
        this.source = source;
        this.sourceTimestamp = sourceTimestamp;
        this.fetchedAt = Instant.now();
    }

    /**
     * 中文：返回基础币种兑换为目标币种的历史汇率。
     * English: Returns the historical rate from the base currency to the quote currency.
     */
    BigDecimal getExchangeRate() {
        return exchangeRate;
    }
}

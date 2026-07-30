package com.hsbc.portfoliomanager.portfolio.holding;

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
public class PortfolioItem {

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

    /**
     * 中文：JPA 专用无参构造器，用于从数据库记录还原实体。
     * English: JPA-only no-argument constructor used to restore an entity from a database row.
     */
    protected PortfolioItem() {
    }

    /**
     * 中文：默认以 USD 创建持仓，兼容 US-01/US-02 的既有行为。
     * English: Creates a portfolio item with default USD currency to preserve existing behavior.
     */
    public PortfolioItem(AssetType assetType, String symbol, BigDecimal quantity) {
        this(assetType, symbol, quantity, "USD");
    }

    /**
     * 中文：显式指定币种创建持仓，用于交易入账时与交易币种保持一致。
     * English: Creates a portfolio item with explicit currency so holdings stay aligned with transaction currency.
     */
    public PortfolioItem(AssetType assetType, String symbol, BigDecimal quantity, String currency) {
        this(assetType, symbol, null, null, quantity, currency);
    }

    /**
     * 中文：使用完整的资产元数据创建持仓实体。
     * English: Creates a portfolio holding entity with complete asset metadata.
     */
    public PortfolioItem(
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

    /**
     * 中文：返回持仓的数据库主键。
     * English: Returns the database identifier of this portfolio holding.
     */
    public Long getId() {
        return id;
    }

    /**
     * 中文：返回资产类型。
     * English: Returns the asset type.
     */
    public AssetType getAssetType() {
        return assetType;
    }

    /**
     * 中文：返回标准化后的资产代码。
     * English: Returns the normalized asset symbol.
     */
    public String getSymbol() {
        return symbol;
    }

    /**
     * 中文：返回公司名称。
     * English: Returns the company name.
     */
    public String getCompanyName() {
        return companyName;
    }

    /**
     * 中文：返回资产所在交易所。
     * English: Returns the exchange on which the asset is traded.
     */
    public String getExchange() {
        return exchange;
    }

    /**
     * 中文：返回当前持仓数量。
     * English: Returns the current holding quantity.
     */
    public BigDecimal getQuantity() {
        return quantity;
    }

    /**
     * 中文：返回资产的计价币种。
     * English: Returns the trading currency of the asset.
     */
    public String getCurrency() {
        return currency;
    }

    /**
     * 中文：在同一资产重复买入时累加数量，保证持仓是“当前快照”而不是“逐笔明细”。
     * English: Adds quantity for repeated buys so portfolio_items remains a current-position snapshot.
     */
    public void addQuantity(BigDecimal additionalQuantity) {
        this.quantity = this.quantity.add(additionalQuantity);
    }

    /**
     * 中文：部分减仓后以新的总数量替换当前持仓数量。
     * English: Replaces the current holding quantity with the new total after a partial removal.
     */
    public void replaceQuantity(BigDecimal newQuantity) {
        this.quantity = newQuantity;
    }
}

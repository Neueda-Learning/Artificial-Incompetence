package com.hsbc.portfoliomanager.portfolio.holding;

public interface AssetMetadataClient {

    /**
     * 中文：根据资产代码查询公司名称、交易所和计价币种。
     * English: Looks up the company name, exchange, and trading currency for an asset symbol.
     */
    AssetMetadata findBySymbol(String symbol);
}

package com.hsbc.portfoliomanager.portfolio.holding;

interface AssetMetadataClient {

    AssetMetadata findBySymbol(String symbol);
}

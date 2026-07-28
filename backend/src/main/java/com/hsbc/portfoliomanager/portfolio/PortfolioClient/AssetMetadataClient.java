package com.hsbc.portfoliomanager.portfolio;

interface AssetMetadataClient {

    AssetMetadata findBySymbol(String symbol);
}

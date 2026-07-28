package com.hsbc.portfoliomanager.portfolio;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface PortfolioItemRepository extends JpaRepository<PortfolioItem, Long> {
    Optional<PortfolioItem> findByAssetTypeAndSymbolAndCurrency(AssetType assetType, String symbol, String currency);
}

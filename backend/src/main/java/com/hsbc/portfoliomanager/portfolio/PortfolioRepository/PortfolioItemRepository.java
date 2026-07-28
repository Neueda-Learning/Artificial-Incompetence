package com.hsbc.portfoliomanager.portfolio;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface PortfolioItemRepository extends JpaRepository<PortfolioItem, Long> {
    /**
     * 中文：按资产类型+代码+币种查找现有持仓，用于交易写入时执行“更新数量或新增持仓”的分支逻辑。
     * English: Finds an existing position by asset type + symbol + currency for upsert-like holding updates.
     */
    Optional<PortfolioItem> findByAssetTypeAndSymbolAndCurrency(AssetType assetType, String symbol, String currency);
}

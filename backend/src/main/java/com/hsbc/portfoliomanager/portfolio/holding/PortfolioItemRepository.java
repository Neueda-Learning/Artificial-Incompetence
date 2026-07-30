package com.hsbc.portfoliomanager.portfolio.holding;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PortfolioItemRepository extends JpaRepository<PortfolioItem, Long> {
    /**
     * 中文：按资产类型+代码+币种查找现有持仓，用于交易写入时执行“更新数量或新增持仓”的分支逻辑。
     * English: Finds an existing position by asset type + symbol + currency for upsert-like holding updates.
     */
    Optional<PortfolioItem> findByAssetTypeAndSymbolAndCurrency(AssetType assetType, String symbol, String currency);

    /**
     * 中文：查找同资产类型和代码的全部持仓行，用于兼容并合并旧版本产生的重复记录。
     * English: Finds all holding rows for an asset type and symbol so legacy duplicate rows can be consolidated.
     */
    List<PortfolioItem> findAllByAssetTypeAndSymbol(AssetType assetType, String symbol);
}

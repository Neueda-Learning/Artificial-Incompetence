package com.hsbc.portfoliomanager.portfolio.activity;

import com.hsbc.portfoliomanager.portfolio.holding.AssetType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
public class PortfolioActivityService {

    private final PortfolioActivityRepository repository;

    PortfolioActivityService(PortfolioActivityRepository repository) {
        this.repository = repository;
    }

    /**
     * 中文：返回数据库中的投资组合活动，按发生时间从新到旧排序。
     * English: Returns database-backed portfolio activity ordered newest first.
     */
    @Transactional(readOnly = true)
    List<PortfolioActivityResponse> findAll() {
        return repository.findAllByOrderByOccurredAtDescIdDesc().stream()
                .map(PortfolioActivityResponse::from)
                .toList();
    }

    /**
     * 中文：在新增资产事务中记录买入活动。
     * English: Records an added activity inside the asset-purchase transaction.
     */
    @Transactional
    public PortfolioLedgerEntry recordAdded(
            AssetType assetType,
            String symbol,
            BigDecimal quantity,
            BigDecimal pricePerUnit,
            String currency,
            Instant occurredAt
    ) {
        PortfolioActivity saved = repository.save(new PortfolioActivity(
                PortfolioActivityAction.ADDED,
                assetType,
                symbol,
                quantity,
                pricePerUnit,
                currency,
                null,
                occurredAt
        ));
        return PortfolioLedgerEntry.from(saved);
    }

    /**
     * 中文：在减仓或清仓事务中记录移除活动及操作后的剩余数量。
     * English: Records a removal activity and the resulting quantity inside the holding transaction.
     */
    @Transactional
    public void recordRemoved(
            AssetType assetType,
            String symbol,
            BigDecimal quantity,
            String currency,
            BigDecimal remainingQuantity,
            Instant occurredAt
    ) {
        repository.save(new PortfolioActivity(
                PortfolioActivityAction.REMOVED,
                assetType,
                symbol,
                quantity,
                null,
                currency,
                remainingQuantity,
                occurredAt
        ));
    }

    /**
     * 中文：返回全部流水并按发生时间正序排列，用于重建当前成本和历史表现。
     * English: Returns the complete ledger in chronological order for current-cost and historical reconstruction.
     */
    @Transactional(readOnly = true)
    public List<PortfolioLedgerEntry> findLedgerOldestFirst() {
        return repository.findAllByOrderByOccurredAtAscIdAsc().stream()
                .map(PortfolioLedgerEntry::from)
                .toList();
    }

    /**
     * 中文：只返回新增/购买流水并按时间倒序排列，用于兼容购买历史接口。
     * English: Returns added/purchase entries newest first for the compatible purchase-history API.
     */
    @Transactional(readOnly = true)
    public List<PortfolioLedgerEntry> findPurchasesNewestFirst() {
        return repository.findAllByActionOrderByOccurredAtDescIdDesc(
                        PortfolioActivityAction.ADDED
                ).stream()
                .map(PortfolioLedgerEntry::from)
                .toList();
    }
}

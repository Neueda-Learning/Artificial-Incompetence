package com.hsbc.portfoliomanager.portfolio.holding;

import com.hsbc.portfoliomanager.portfolio.activity.PortfolioActivityService;
import com.hsbc.portfoliomanager.portfolio.transaction.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

@Service
class PortfolioService {

    private final PortfolioItemRepository repository;
    private final AssetMetadataClient assetMetadataClient;
    private final TransactionRepository transactionRepository;
    private final PortfolioActivityService activityService;

    /**
     * 中文：注入持仓仓库和资产元数据客户端。
     * English: Injects the holding repository and asset metadata client.
     */
    PortfolioService(
            PortfolioItemRepository repository,
            AssetMetadataClient assetMetadataClient,
            TransactionRepository transactionRepository,
            PortfolioActivityService activityService
    ) {
        this.repository = repository;
        this.assetMetadataClient = assetMetadataClient;
        this.transactionRepository = transactionRepository;
        this.activityService = activityService;
    }

    /**
     * 中文：只读查询全部持仓并转换为响应 DTO。
     * English: Retrieves all holdings in a read-only transaction and converts them into response DTOs.
     */
    @Transactional(readOnly = true)
    List<PortfolioItemResponse> findAll() {
        return repository.findAll().stream()
                .map(PortfolioItemResponse::from)
                .toList();
    }

    /**
     * 中文：标准化资产代码、查询公司元数据，并新增持仓或累加已有持仓数量。
     * English: Normalizes the symbol, retrieves company metadata, and creates or increments a holding.
     */
    @Transactional
    PortfolioItemResponse create(CreatePortfolioItemRequest request) {
        String symbol = request.symbol().trim().toUpperCase(Locale.ROOT);
        AssetMetadata metadata = assetMetadataClient.findBySymbol(symbol);

        PortfolioItem item = repository.findByAssetTypeAndSymbolAndCurrency(
                        request.assetType(),
                        metadata.symbol(),
                        metadata.currency()
                )
                .map(existing -> {
                    existing.addQuantity(request.quantity());
                    return existing;
                })
                .orElseGet(() -> new PortfolioItem(
                        request.assetType(),
                        metadata.symbol(),
                        metadata.companyName(),
                        metadata.exchange(),
                        request.quantity(),
                        metadata.currency()
                ));

        return PortfolioItemResponse.from(repository.save(item));
    }

    /**
     * 中文：部分减仓时更新该资产的剩余总数量，并合并旧版本遗留的重复持仓行。
     * English: Updates an asset's remaining total quantity after a partial removal and consolidates legacy duplicates.
     */
    @Transactional
    PortfolioItemResponse updateQuantity(Long id, UpdatePortfolioItemQuantityRequest request) {
        PortfolioItem target = repository.findById(id)
                .orElseThrow(() -> new PortfolioItemNotFoundException(id));

        List<PortfolioItem> matchingItems = repository.findAllByAssetTypeAndSymbol(
                target.getAssetType(),
                target.getSymbol()
        );
        BigDecimal previousQuantity = matchingItems.stream()
                .map(PortfolioItem::getQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal removedQuantity = previousQuantity.subtract(request.quantity());
        matchingItems.stream()
                .filter(item -> !item.getId().equals(target.getId()))
                .forEach(repository::delete);

        target.replaceQuantity(request.quantity());
        PortfolioItem saved = repository.save(target);
        if (removedQuantity.signum() > 0) {
            activityService.recordRemoved(
                    target.getAssetType(),
                    target.getSymbol(),
                    removedQuantity,
                    target.getCurrency(),
                    request.quantity(),
                    Instant.now()
            );
        }
        return PortfolioItemResponse.from(saved);
    }

    /**
     * 中文：清仓时在同一数据库事务中删除该资产的全部交易历史及当前持仓。
     * English: On full removal, deletes all transaction history and current holding rows in one database transaction.
     */
    @Transactional
    void delete(Long id) {
        PortfolioItem target = repository.findById(id)
                .orElseThrow(() -> new PortfolioItemNotFoundException(id));
        List<PortfolioItem> matchingItems = repository.findAllByAssetTypeAndSymbol(
                target.getAssetType(),
                target.getSymbol()
        );
        BigDecimal removedQuantity = matchingItems.stream()
                .map(PortfolioItem::getQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        activityService.recordRemoved(
                target.getAssetType(),
                target.getSymbol(),
                removedQuantity,
                target.getCurrency(),
                BigDecimal.ZERO,
                Instant.now()
        );

        transactionRepository.deleteByAssetTypeAndSymbol(
                target.getAssetType(),
                target.getSymbol()
        );
        repository.deleteAll(matchingItems);
    }
}

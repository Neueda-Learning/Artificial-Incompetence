package com.hsbc.portfoliomanager.portfolio.holding;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
class PortfolioService {

    private final PortfolioItemRepository repository;
    private final AssetMetadataClient assetMetadataClient;

    /**
     * 中文：注入持仓仓库和资产元数据客户端。
     * English: Injects the holding repository and asset metadata client.
     */
    PortfolioService(
            PortfolioItemRepository repository,
            AssetMetadataClient assetMetadataClient
    ) {
        this.repository = repository;
        this.assetMetadataClient = assetMetadataClient;
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
     * 中文：标准化资产代码、查询公司元数据并保存新持仓。
     * English: Normalizes the asset symbol, retrieves company metadata, and saves a new holding.
     */
    @Transactional
    PortfolioItemResponse create(CreatePortfolioItemRequest request) {
        String symbol = request.symbol().trim().toUpperCase(Locale.ROOT);
        AssetMetadata metadata = assetMetadataClient.findBySymbol(symbol);

        PortfolioItem item = new PortfolioItem(
                request.assetType(),
                symbol,
                metadata.companyName(),
                metadata.exchange(),
                request.quantity(),
                metadata.currency()
        );
        return PortfolioItemResponse.from(repository.save(item));
    }

    /**
     * 中文：验证持仓存在后按 ID 删除持仓。
     * English: Verifies that a holding exists and then deletes it by identifier.
     */
    @Transactional
    void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new PortfolioItemNotFoundException(id);
        }
        repository.deleteById(id);
    }
}

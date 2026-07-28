package com.hsbc.portfoliomanager.portfolio;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
class PortfolioService {

    private final PortfolioItemRepository repository;
    private final AssetMetadataClient assetMetadataClient;

    PortfolioService(
            PortfolioItemRepository repository,
            AssetMetadataClient assetMetadataClient
    ) {
        this.repository = repository;
        this.assetMetadataClient = assetMetadataClient;
    }

    @Transactional(readOnly = true)
    List<PortfolioItemResponse> findAll() {
        return repository.findAll().stream()
                .map(PortfolioItemResponse::from)
                .toList();
    }

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

    @Transactional
    void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new PortfolioItemNotFoundException(id);
        }
        repository.deleteById(id);
    }
}

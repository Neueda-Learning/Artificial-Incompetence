package com.hsbc.portfoliomanager.portfolio;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
class PortfolioService {

    private final PortfolioItemRepository repository;

    PortfolioService(PortfolioItemRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    List<PortfolioItemResponse> findAll() {
        return repository.findAll().stream()
                .map(PortfolioItemResponse::from)
                .toList();
    }

    @Transactional
    PortfolioItemResponse create(CreatePortfolioItemRequest request) {
        PortfolioItem item = new PortfolioItem(
                request.assetType(),
                request.symbol().trim().toUpperCase(Locale.ROOT),
                request.quantity()
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


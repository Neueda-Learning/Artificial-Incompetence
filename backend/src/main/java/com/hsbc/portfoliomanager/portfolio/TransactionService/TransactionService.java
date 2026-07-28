package com.hsbc.portfoliomanager.portfolio;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
class TransactionService {

    private final TransactionRepository transactionRepository;
    private final PortfolioItemRepository portfolioItemRepository;
    private final ExchangeRateClient exchangeRateClient;

    TransactionService(
            TransactionRepository transactionRepository,
            PortfolioItemRepository portfolioItemRepository,
            ExchangeRateClient exchangeRateClient
    ) {
        this.transactionRepository = transactionRepository;
        this.portfolioItemRepository = portfolioItemRepository;
        this.exchangeRateClient = exchangeRateClient;
    }

    /**
     * 中文：在一个事务中同时完成“写交易历史 + 更新当前持仓”，避免两张表状态不一致。
     * English: In one transaction, writes transaction history and updates current holdings atomically.
     */
    @Transactional
    TransactionResponse create(CreateTransactionRequest request) {
        if (request.transactionType() != TransactionType.BUY) {
            throw new UnsupportedTransactionTypeException(request.transactionType());
        }

        // 中文：统一标准化资产代码与币种，避免大小写和空格导致重复资产。
        // English: Normalize symbol and currency to avoid duplicate assets caused by casing/whitespace differences.
        String normalizedSymbol = request.symbol().trim().toUpperCase(Locale.ROOT);
        String normalizedCurrency = request.currency().trim().toUpperCase(Locale.ROOT);

        if (!exchangeRateClient.isKnownCurrency(normalizedCurrency)) {
            throw new UnknownCurrencyException(normalizedCurrency);
        }

        // 中文：先落交易历史，确保“发生过的交易”可追溯。
        // English: Persist transaction history first so executed events remain traceable.
        TransactionRecord transaction = new TransactionRecord(
                request.transactionType(),
                request.assetType(),
                normalizedSymbol,
                request.quantity(),
                request.pricePerUnit(),
                normalizedCurrency,
                request.purchasedAt()
        );
        TransactionRecord savedTransaction = transactionRepository.save(transaction);

        // 中文：再更新持仓快照；若已有同一资产则累加数量，否则创建新持仓行。
        // English: Then update holdings snapshot; add quantity if position exists, otherwise create a new position row.
        portfolioItemRepository
                .findByAssetTypeAndSymbolAndCurrency(request.assetType(), normalizedSymbol, normalizedCurrency)
                .ifPresentOrElse(
                        item -> item.addQuantity(request.quantity()),
                        () -> portfolioItemRepository.save(new PortfolioItem(
                                request.assetType(),
                                normalizedSymbol,
                                request.quantity(),
                                normalizedCurrency
                        ))
                );

        return TransactionResponse.from(savedTransaction);
    }

    /**
     * 中文：按交易类型返回历史记录，结果由数据库排序后再映射为响应 DTO。
     * English: Returns history by transaction type, using DB ordering and DTO mapping for API output.
     */
    @Transactional(readOnly = true)
    List<TransactionResponse> findByType(TransactionType type) {
        return transactionRepository.findByTransactionTypeOrderByTransactedAtDesc(type).stream()
                .map(TransactionResponse::from)
                .toList();
    }
}

package com.hsbc.portfoliomanager.portfolio.transaction;

import com.hsbc.portfoliomanager.portfolio.activity.PortfolioActivityService;
import com.hsbc.portfoliomanager.portfolio.holding.AssetMetadata;
import com.hsbc.portfoliomanager.portfolio.holding.AssetMetadataClient;
import com.hsbc.portfoliomanager.portfolio.holding.AssetType;
import com.hsbc.portfoliomanager.portfolio.holding.PortfolioItem;
import com.hsbc.portfoliomanager.portfolio.holding.PortfolioItemRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;

@Service
class TransactionService {

    private final TransactionRepository transactionRepository;
    private final PortfolioItemRepository portfolioItemRepository;
    private final ExchangeRateClient exchangeRateClient;
    private final AssetMetadataClient assetMetadataClient;
    private final PortfolioActivityService activityService;

    /**
     * 中文：注入交易仓库、持仓仓库、币种校验客户端和资产元数据客户端。
     * English: Injects transaction and holding repositories plus currency-validation and asset-metadata clients.
     */
    TransactionService(
            TransactionRepository transactionRepository,
            PortfolioItemRepository portfolioItemRepository,
            ExchangeRateClient exchangeRateClient,
            AssetMetadataClient assetMetadataClient,
            PortfolioActivityService activityService
    ) {
        this.transactionRepository = transactionRepository;
        this.portfolioItemRepository = portfolioItemRepository;
        this.exchangeRateClient = exchangeRateClient;
        this.assetMetadataClient = assetMetadataClient;
        this.activityService = activityService;
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

        // 中文：统一标准化资产代码；股票的代码、交易所和币种由 Twelve Data 自动解析。
        // English: Normalize the submitted ticker; Twelve Data resolves canonical ticker, exchange, and currency for stocks.
        String requestedSymbol = request.symbol().trim().toUpperCase(Locale.ROOT);
        String requestedCurrency = StringUtils.hasText(request.currency())
                ? request.currency().trim().toUpperCase(Locale.ROOT)
                : "USD";

        AssetMetadata metadata = request.assetType() == AssetType.STOCK
                ? assetMetadataClient.findBySymbol(requestedSymbol)
                : new AssetMetadata(requestedSymbol, null, null, requestedCurrency);

        String normalizedSymbol = metadata.symbol();
        String normalizedCurrency = request.assetType() == AssetType.STOCK
                ? metadata.currency()
                : requestedCurrency;

        // 中文：股票币种由 Twelve Data 的已解析上市记录提供；手工输入币种只需对非股票资产额外校验。
        // English: Stock currency comes from the resolved Twelve Data listing; only manually entered non-stock currency
        // needs separate validation.
        if (request.assetType() != AssetType.STOCK
                && !exchangeRateClient.isKnownCurrency(normalizedCurrency)) {
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
                                metadata.companyName(),
                                metadata.exchange(),
                                request.quantity(),
                                normalizedCurrency
                        ))
                );

        activityService.recordAdded(
                request.assetType(),
                normalizedSymbol,
                request.quantity(),
                request.pricePerUnit(),
                normalizedCurrency,
                request.purchasedAt()
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

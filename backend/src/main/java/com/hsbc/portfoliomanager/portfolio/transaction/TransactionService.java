package com.hsbc.portfoliomanager.portfolio.transaction;

import com.hsbc.portfoliomanager.portfolio.activity.PortfolioActivityService;
import com.hsbc.portfoliomanager.portfolio.activity.PortfolioLedgerEntry;
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

    private final PortfolioItemRepository portfolioItemRepository;
    private final ExchangeRateClient exchangeRateClient;
    private final AssetMetadataClient assetMetadataClient;
    private final PortfolioActivityService activityService;

    /**
     * 中文：注入持仓、活动账本、币种校验和资产元数据依赖。
     * English: Injects holding, activity-ledger, currency-validation, and asset-metadata dependencies.
     */
    TransactionService(
            PortfolioItemRepository portfolioItemRepository,
            ExchangeRateClient exchangeRateClient,
            AssetMetadataClient assetMetadataClient,
            PortfolioActivityService activityService
    ) {
        this.portfolioItemRepository = portfolioItemRepository;
        this.exchangeRateClient = exchangeRateClient;
        this.assetMetadataClient = assetMetadataClient;
        this.activityService = activityService;
    }

    /**
     * 中文：在一个事务中同时完成“写购买活动 + 更新当前持仓”，避免两张表状态不一致。
     * English: Atomically writes purchase activity and updates current holdings in one transaction.
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

        // 中文：更新持仓快照；若已有同一资产则累加数量，否则创建新持仓行。
        // English: Update the holdings snapshot; add quantity if the position exists, otherwise create it.
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

        PortfolioLedgerEntry savedPurchase = activityService.recordAdded(
                request.assetType(),
                normalizedSymbol,
                request.quantity(),
                request.pricePerUnit(),
                normalizedCurrency,
                request.purchasedAt()
        );

        return TransactionResponse.from(savedPurchase);
    }

    /**
     * 中文：按交易类型返回历史记录，结果由数据库排序后再映射为响应 DTO。
     * English: Returns history by transaction type, using DB ordering and DTO mapping for API output.
     */
    @Transactional(readOnly = true)
    List<TransactionResponse> findByType(TransactionType type) {
        if (type != TransactionType.BUY) {
            return List.of();
        }
        return activityService.findPurchasesNewestFirst().stream()
                .map(TransactionResponse::from)
                .toList();
    }
}

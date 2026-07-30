package com.hsbc.portfoliomanager.portfolio.holding;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
class TwelveDataAssetMetadataClient implements AssetMetadataClient {

    private static final int MAX_QUOTE_CANDIDATES = 8;
    private static final Map<String, String> SUFFIX_TO_MIC = Map.ofEntries(
            Map.entry("HK", "XHKG"),
            Map.entry("L", "XLON"),
            Map.entry("TO", "XTSE"),
            Map.entry("V", "XTSX"),
            Map.entry("AX", "XASX"),
            Map.entry("T", "XTKS"),
            Map.entry("SS", "XSHG"),
            Map.entry("SH", "XSHG"),
            Map.entry("SZ", "XSHE"),
            Map.entry("KS", "XKRX"),
            Map.entry("KQ", "XKOS"),
            Map.entry("PA", "XPAR"),
            Map.entry("DE", "XETR"),
            Map.entry("MI", "XMIL"),
            Map.entry("AS", "XAMS"),
            Map.entry("BR", "XBRU"),
            Map.entry("SW", "XSWX"),
            Map.entry("ST", "XSTO")
    );

    private final RestClient restClient;
    private final TwelveDataProperties properties;

    /**
     * 中文：根据 Twelve Data 配置创建元数据 HTTP 客户端。
     * English: Creates the metadata HTTP client from the Twelve Data configuration.
     */
    @Autowired
    TwelveDataAssetMetadataClient(TwelveDataProperties properties) {
        this(properties, RestClient.builder());
    }

    /**
     * 中文：使用给定构建器创建客户端，使 HTTP 行为可以在单元测试中被隔离验证。
     * English: Creates the client with a supplied builder so HTTP behavior can be isolated in unit tests.
     */
    TwelveDataAssetMetadataClient(
            TwelveDataProperties properties,
            RestClient.Builder restClientBuilder
    ) {
        this.restClient = restClientBuilder
                .baseUrl(properties.baseUrl())
                .build();
        this.properties = properties;
    }

    /**
     * 中文：只根据用户输入的股票代码搜索全球上市记录，并返回第一个可成功报价的规范资产。
     * English: Searches global listings using only the submitted ticker and returns the first listing with a valid quote.
     */
    @Override
    public AssetMetadata findBySymbol(String symbol) {
        if (!StringUtils.hasText(properties.apiKey())) {
            throw new AssetMetadataLookupException("Twelve Data API key is not configured");
        }

        SymbolQuery query = SymbolQuery.from(symbol);
        List<TwelveDataSymbolSearchResponse.Result> candidates = searchCandidates(query);
        if (candidates.isEmpty()) {
            throw new AssetMetadataLookupException(
                    "No stock listing found for " + query.original()
                            + ". Enter a ticker supported by Twelve Data."
            );
        }

        List<String> quoteFailures = new ArrayList<>();
        for (TwelveDataSymbolSearchResponse.Result candidate
                : candidates.stream().limit(MAX_QUOTE_CANDIDATES).toList()) {
            try {
                TwelveDataQuoteResponse quote = fetchQuote(candidate);
                if (quote != null && StringUtils.hasText(quote.message())) {
                    quoteFailures.add(candidateLabel(candidate) + ": " + quote.message());
                    continue;
                }
                if (!isComplete(quote)) {
                    quoteFailures.add(candidateLabel(candidate) + ": incomplete quote metadata");
                    continue;
                }
                return toAssetMetadata(quote);
            } catch (RestClientException exception) {
                quoteFailures.add(candidateLabel(candidate) + ": " + upstreamReason(exception));
            }
        }

        String detail = quoteFailures.isEmpty()
                ? "No matching listing returned complete quote metadata."
                : quoteFailures.get(0);
        throw new AssetMetadataLookupException(
                "Twelve Data found " + query.original()
                        + " but could not quote an accessible listing. "
                        + detail
                        + " Check the symbol and your Twelve Data market-data plan."
        );
    }

    /**
     * 中文：调用 symbol_search，并按代码精确度、后缀市场提示和结果顺序排列候选股票。
     * English: Calls symbol_search and ranks stock candidates by ticker match, suffix market hint, and provider order.
     */
    private List<TwelveDataSymbolSearchResponse.Result> searchCandidates(SymbolQuery query) {
        Map<String, TwelveDataSymbolSearchResponse.Result> uniqueResults = new LinkedHashMap<>();
        List<String> searchTerms = query.searchTerms();

        for (String searchTerm : searchTerms) {
            TwelveDataSymbolSearchResponse response;
            try {
                response = restClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/symbol_search")
                                .queryParam("symbol", searchTerm)
                                .queryParam("outputsize", 30)
                                .queryParam("show_plan", true)
                                .queryParam("apikey", properties.apiKey())
                                .build())
                        .retrieve()
                        .body(TwelveDataSymbolSearchResponse.class);
            } catch (RestClientException exception) {
                throw new AssetMetadataLookupException(
                        "Unable to search Twelve Data for " + query.original()
                                + ": " + upstreamReason(exception),
                        exception
                );
            }

            if (response != null && StringUtils.hasText(response.message())) {
                throw new AssetMetadataLookupException(
                        "Twelve Data rejected symbol search for "
                                + query.original() + ": " + response.message()
                );
            }
            if (response == null || response.data() == null) {
                continue;
            }

            response.data().stream()
                    .filter(result -> StringUtils.hasText(result.symbol()))
                    .filter(result -> query.matchesSymbol(result.symbol()))
                    .filter(TwelveDataAssetMetadataClient::isStockListing)
                    .forEach(result -> uniqueResults.putIfAbsent(candidateKey(result), result));

            if (!uniqueResults.isEmpty()) {
                break;
            }
        }

        return uniqueResults.values().stream()
                .sorted(Comparator
                        .comparing((TwelveDataSymbolSearchResponse.Result result) ->
                                !query.matchesMarketHint(result))
                        .thenComparing(result -> !query.matchesExactly(result.symbol())))
                .toList();
    }

    /**
     * 中文：使用搜索结果中的 MIC（优先）或交易所名称调用 quote，避免同名股票取错市场。
     * English: Calls quote with the search result's MIC (preferred) or exchange to disambiguate duplicate tickers.
     */
    private TwelveDataQuoteResponse fetchQuote(TwelveDataSymbolSearchResponse.Result candidate) {
        return restClient.get()
                .uri(uriBuilder -> {
                    var builder = uriBuilder
                            .path("/quote")
                            .queryParam("symbol", candidate.symbol())
                            .queryParam("apikey", properties.apiKey());
                    if (StringUtils.hasText(candidate.micCode())) {
                        builder.queryParam("mic_code", candidate.micCode());
                    } else if (StringUtils.hasText(candidate.exchange())) {
                        builder.queryParam("exchange", candidate.exchange());
                    }
                    return builder.build();
                })
                .retrieve()
                .body(TwelveDataQuoteResponse.class);
    }

    /**
     * 中文：判断搜索结果是否属于可作为股票持仓保存的证券类型。
     * English: Determines whether a search result is an equity-like instrument suitable for a stock holding.
     */
    private static boolean isStockListing(TwelveDataSymbolSearchResponse.Result result) {
        if (!StringUtils.hasText(result.instrumentType())) {
            return true;
        }
        String type = result.instrumentType().toUpperCase(Locale.ROOT);
        return type.contains("STOCK")
                || type.contains("DEPOSITARY RECEIPT")
                || type.equals("REIT");
    }

    /**
     * 中文：检查 quote 是否包含保存持仓所需的全部长期元数据。
     * English: Checks that quote contains all long-lived metadata required for a holding.
     */
    private static boolean isComplete(TwelveDataQuoteResponse quote) {
        return quote != null
                && StringUtils.hasText(quote.symbol())
                && StringUtils.hasText(quote.name())
                && StringUtils.hasText(quote.exchange())
                && StringUtils.hasText(quote.currency());
    }

    /**
     * 中文：将 Twelve Data quote 转换为标准化后的领域元数据。
     * English: Converts a Twelve Data quote into normalized domain metadata.
     */
    private static AssetMetadata toAssetMetadata(TwelveDataQuoteResponse quote) {
        return new AssetMetadata(
                quote.symbol().trim().toUpperCase(Locale.ROOT),
                quote.name().trim(),
                quote.exchange().trim().toUpperCase(Locale.ROOT),
                quote.currency().trim().toUpperCase(Locale.ROOT)
        );
    }

    /**
     * 中文：生成候选上市记录的稳定去重键。
     * English: Builds a stable key for de-duplicating listing candidates.
     */
    private static String candidateKey(TwelveDataSymbolSearchResponse.Result candidate) {
        String market = StringUtils.hasText(candidate.micCode())
                ? candidate.micCode()
                : candidate.exchange();
        return candidate.symbol().toUpperCase(Locale.ROOT)
                + ":" + (market == null ? "" : market.toUpperCase(Locale.ROOT));
    }

    /**
     * 中文：生成便于理解的股票候选名称，用于 API 错误信息。
     * English: Builds a readable candidate label for API error messages.
     */
    private static String candidateLabel(TwelveDataSymbolSearchResponse.Result candidate) {
        String market = StringUtils.hasText(candidate.micCode())
                ? candidate.micCode()
                : candidate.exchange();
        return candidate.symbol() + (StringUtils.hasText(market) ? " on " + market : "");
    }

    /**
     * 中文：从 HTTP 客户端异常中提取简洁的上游状态或错误内容。
     * English: Extracts a concise upstream status or response body from an HTTP client exception.
     */
    private static String upstreamReason(RestClientException exception) {
        if (exception instanceof RestClientResponseException responseException) {
            String body = responseException.getResponseBodyAsString();
            if (StringUtils.hasText(body)) {
                String normalized = body.replaceAll("\\s+", " ").trim();
                return normalized.length() <= 240
                        ? normalized
                        : normalized.substring(0, 240);
            }
            return "HTTP " + responseException.getStatusCode().value();
        }
        return exception.getMessage() == null
                ? "upstream request failed"
                : exception.getMessage();
    }

    private record SymbolQuery(
            String original,
            String symbol,
            String marketHint,
            List<String> searchTerms
    ) {

        /**
         * 中文：标准化用户代码，并把常见市场后缀作为自动排序提示而不是必填交易所字段。
         * English: Normalizes a ticker and treats common market suffixes as ranking hints, not required exchange input.
         */
        static SymbolQuery from(String rawSymbol) {
            String original = rawSymbol.trim().toUpperCase(Locale.ROOT);
            String normalizedSymbol = original;
            String hint = null;

            int colon = original.lastIndexOf(':');
            if (colon > 0 && colon < original.length() - 1) {
                normalizedSymbol = original.substring(0, colon);
                hint = original.substring(colon + 1);
            } else {
                int dot = original.lastIndexOf('.');
                if (dot > 0 && dot < original.length() - 1) {
                    String suffix = original.substring(dot + 1);
                    String mappedMic = SUFFIX_TO_MIC.get(suffix);
                    if (mappedMic != null) {
                        normalizedSymbol = original.substring(0, dot);
                        hint = mappedMic;
                    }
                }
            }

            List<String> terms = new ArrayList<>();
            terms.add(normalizedSymbol);
            if (!normalizedSymbol.equals(original)) {
                terms.add(original);
            } else {
                int dot = original.lastIndexOf('.');
                if (dot > 0) {
                    terms.add(original.substring(0, dot));
                }
            }
            return new SymbolQuery(original, normalizedSymbol, hint, List.copyOf(terms));
        }

        /**
         * 中文：比较搜索结果代码，同时兼容数字股票代码前导零数量不同。
         * English: Matches provider tickers while tolerating different leading-zero counts for numeric symbols.
         */
        boolean matchesSymbol(String candidateSymbol) {
            String candidate = candidateSymbol.trim().toUpperCase(Locale.ROOT);
            if (matchesExactly(candidate)) {
                return true;
            }
            if (!symbol.matches("\\d+") || !candidate.matches("\\d+")) {
                return false;
            }
            return stripLeadingZeros(symbol).equals(stripLeadingZeros(candidate));
        }

        /**
         * 中文：判断搜索结果是否与标准化后的代码完全一致。
         * English: Checks whether a provider result exactly equals the normalized ticker.
         */
        boolean matchesExactly(String candidateSymbol) {
            return symbol.equals(candidateSymbol.trim().toUpperCase(Locale.ROOT));
        }

        /**
         * 中文：判断搜索结果是否匹配从代码后缀自动推断出的市场提示。
         * English: Checks whether a result matches the market hint inferred automatically from the ticker suffix.
         */
        boolean matchesMarketHint(TwelveDataSymbolSearchResponse.Result result) {
            if (!StringUtils.hasText(marketHint)) {
                return true;
            }
            return marketHint.equalsIgnoreCase(result.micCode())
                    || marketHint.equalsIgnoreCase(result.exchange());
        }

        /**
         * 中文：移除数字代码的非必要前导零，以匹配不同市场代码格式。
         * English: Removes insignificant leading zeros to match differing numeric ticker formats.
         */
        private static String stripLeadingZeros(String value) {
            String stripped = value.replaceFirst("^0+(?!$)", "");
            return stripped.isEmpty() ? "0" : stripped;
        }
    }
}

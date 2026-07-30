package com.hsbc.portfoliomanager.portfolio.transaction;

import com.hsbc.portfoliomanager.portfolio.holding.AssetMetadata;
import com.hsbc.portfoliomanager.portfolio.holding.AssetMetadataClient;
import com.hsbc.portfoliomanager.portfolio.holding.PortfolioItem;
import com.hsbc.portfoliomanager.portfolio.holding.PortfolioItemRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
/**
 * 中文：Transaction 控制器集成测试，覆盖创建交易、查询历史、持仓联动及异常路径。
 * English: Integration tests for Transaction controller, covering creation, history query, holding sync, and error paths.
 */
class TransactionControllerIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private PortfolioItemRepository portfolioItemRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private ExchangeRateClient exchangeRateClient;

    @MockitoBean
    private AssetMetadataClient assetMetadataClient;

    /**
     * 中文：每个测试前清空交易和持仓，确保用例互不影响；并 mock 汇率校验通过，聚焦交易业务本身。
     * English: Clears tables before each test to keep cases isolated, and mocks exchange-rate validation as true
     * to focus on transaction business behavior.
     */
    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM portfolio_activities");
        transactionRepository.deleteAll();
        portfolioItemRepository.deleteAll();
        when(exchangeRateClient.isKnownCurrency(anyString())).thenReturn(true);
        when(assetMetadataClient.findBySymbol(anyString()))
                .thenAnswer(invocation -> {
                    String symbol = invocation.getArgument(0);
                    return new AssetMetadata(symbol, symbol + " Inc.", "NASDAQ", "USD");
                });
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    /**
     * 中文：验证在没有购买历史时，查询 BUY 返回空数组。
     * English: Verifies querying BUY history returns an empty array when no records exist.
     */
    @Test
    void shouldReturnEmptyArrayWhenNoBuyHistoryExists() throws Exception {
        mockMvc.perform(get("/api/transactions").queryParam("type", "BUY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    /**
     * 中文：验证缺省查询参数 type 时，接口按默认值 BUY 查询。
     * English: Verifies that when query parameter type is omitted, endpoint defaults to BUY.
     */
    @Test
    void shouldUseBuyAsDefaultTransactionTypeWhenTypeNotProvided() throws Exception {
        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "transactionType":"BUY",
                                  "assetType":"STOCK",
                                  "symbol":"AAPL",
                                  "quantity":1,
                                  "pricePerUnit":180.50,
                                  "currency":"USD",
                                  "purchasedAt":"2026-07-27T10:30:00Z"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].symbol").value("AAPL"));
    }

    /**
     * 中文：验证购买记录按 purchasedAt 从新到旧排序，并检查标准化后的 symbol/currency。
     * English: Verifies buy history is sorted by purchasedAt descending and checks normalized symbol/currency.
     */
    @Test
    void shouldCreateAndReturnBuyHistoryInDescendingOrder() throws Exception {
        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "transactionType":"BUY",
                                  "assetType":"STOCK",
                                  "symbol":" aapl ",
                                  "quantity":5,
                                  "pricePerUnit":180.50,
                                  "currency":"usd",
                                  "purchasedAt":"2026-07-27T10:30:00Z"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "transactionType":"BUY",
                                  "assetType":"STOCK",
                                  "symbol":"AAPL",
                                  "quantity":2,
                                  "pricePerUnit":185.20,
                                  "currency":"USD",
                                  "purchasedAt":"2026-07-27T11:30:00Z"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/transactions").queryParam("type", "BUY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].symbol").value("AAPL"))
                .andExpect(jsonPath("$[0].quantity").value(2))
                .andExpect(jsonPath("$[0].pricePerUnit").value(185.20))
                .andExpect(jsonPath("$[0].currency").value("USD"))
                .andExpect(jsonPath("$[0].purchasedAt").value("2026-07-27T11:30:00Z"))
                .andExpect(jsonPath("$[1].purchasedAt").value("2026-07-27T10:30:00Z"));

        mockMvc.perform(get("/api/portfolio/activities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].action").value("ADDED"))
                .andExpect(jsonPath("$[0].symbol").value("AAPL"))
                .andExpect(jsonPath("$[0].quantity").value(2))
                .andExpect(jsonPath("$[0].pricePerUnit").value(185.20))
                .andExpect(jsonPath("$[0].occurredAt").value("2026-07-27T11:30:00Z"));
    }

    /**
     * 中文：验证股票请求无需提供交易所或币种，后端会使用 Twelve Data 返回的规范元数据。
     * English: Verifies a stock request needs no exchange or currency and uses canonical metadata from Twelve Data.
     */
    @Test
    void shouldResolveStockExchangeAndCurrencyFromSymbolOnly() throws Exception {
        when(assetMetadataClient.findBySymbol("00700.HK"))
                .thenReturn(new AssetMetadata("0700", "Tencent Holdings Ltd", "HKEX", "HKD"));

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "transactionType":"BUY",
                                  "assetType":"STOCK",
                                  "symbol":"00700.HK",
                                  "quantity":2,
                                  "pricePerUnit":550.00,
                                  "purchasedAt":"2026-07-29T07:00:00Z"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.symbol").value("0700"))
                .andExpect(jsonPath("$.currency").value("HKD"));

        PortfolioItem holding = portfolioItemRepository.findAll().get(0);
        org.junit.jupiter.api.Assertions.assertEquals("0700", holding.getSymbol());
        org.junit.jupiter.api.Assertions.assertEquals("Tencent Holdings Ltd", holding.getCompanyName());
        org.junit.jupiter.api.Assertions.assertEquals("HKEX", holding.getExchange());
        org.junit.jupiter.api.Assertions.assertEquals("HKD", holding.getCurrency());
    }

    /**
     * 中文：验证 quantity <= 0 时返回字段级校验错误。
     * English: Verifies field-level validation error when quantity <= 0.
     */
    @Test
    void shouldReturnValidationErrorForInvalidQuantity() throws Exception {
        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "transactionType":"BUY",
                                  "assetType":"STOCK",
                                  "symbol":"AAPL",
                                  "quantity":0,
                                  "pricePerUnit":180.50,
                                  "currency":"USD",
                                  "purchasedAt":"2026-07-27T10:30:00Z"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.quantity").exists());
    }

    /**
     * 中文：验证 currency 长度不为 3 时返回字段级 400 错误。
     * English: Verifies field-level 400 validation error when currency length is not 3.
     */
    @Test
    void shouldReturnValidationErrorForInvalidCurrencyLength() throws Exception {
        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "transactionType":"BUY",
                                  "assetType":"STOCK",
                                  "symbol":"AAPL",
                                  "quantity":2,
                                  "pricePerUnit":180.50,
                                  "currency":"US",
                                  "purchasedAt":"2026-07-27T10:30:00Z"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.currency").exists());
    }

    /**
     * 中文：验证缺少 purchasedAt 时返回字段级 400 错误。
     * English: Verifies field-level 400 validation error when purchasedAt is missing.
     */
    @Test
    void shouldReturnValidationErrorForMissingPurchasedAt() throws Exception {
        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "transactionType":"BUY",
                                  "assetType":"CASH",
                                  "symbol":"AAPL",
                                  "quantity":2,
                                  "pricePerUnit":180.50,
                                  "currency":"USD"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.purchasedAt").exists());
    }

    /**
     * 中文：验证 pricePerUnit <= 0 时返回字段级校验错误。
     * English: Verifies field-level validation error when pricePerUnit <= 0.
     */
    @Test
    void shouldReturnValidationErrorForInvalidPricePerUnit() throws Exception {
        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "transactionType":"BUY",
                                  "assetType":"STOCK",
                                  "symbol":"AAPL",
                                  "quantity":2,
                                  "pricePerUnit":0,
                                  "currency":"USD",
                                  "purchasedAt":"2026-07-27T10:30:00Z"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.pricePerUnit").exists());
    }

    /**
     * 中文：验证当前阶段不支持 SELL 创建，返回 transactionType 字段错误。
     * English: Verifies SELL creation is not supported in current phase and returns transactionType field error.
     */
    @Test
    void shouldReturnValidationErrorForUnsupportedTransactionType() throws Exception {
        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "transactionType":"SELL",
                                  "assetType":"STOCK",
                                  "symbol":"AAPL",
                                  "quantity":2,
                                  "pricePerUnit":180.50,
                                  "currency":"USD",
                                  "purchasedAt":"2026-07-27T10:30:00Z"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.transactionType").exists());
    }

    /**
     * 中文：验证币种无法被汇率服务识别时返回 currency 字段错误。
     * English: Verifies unknown currency from exchange-rate validation returns a currency field error.
     */
    @Test
    void shouldReturnValidationErrorForUnknownCurrency() throws Exception {
        when(exchangeRateClient.isKnownCurrency("XYZ")).thenReturn(false);

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "transactionType":"BUY",
                                  "assetType":"CASH",
                                  "symbol":"AAPL",
                                  "quantity":2,
                                  "pricePerUnit":180.50,
                                  "currency":"XYZ",
                                  "purchasedAt":"2026-07-27T10:30:00Z"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.currency").exists());
    }

    /**
     * 中文：验证汇率服务不可用时返回 502，标识上游依赖故障。
     * English: Verifies HTTP 502 is returned when exchange-rate service is unavailable (upstream dependency failure).
     */
    @Test
    void shouldReturnBadGatewayWhenExchangeRateSourceUnavailable() throws Exception {
        when(exchangeRateClient.isKnownCurrency("HKD"))
                .thenThrow(new ExchangeRateUnavailableException("Failed to load exchange rates", new RuntimeException("boom")));

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "transactionType":"BUY",
                                  "assetType":"CASH",
                                  "symbol":"AAPL",
                                  "quantity":2,
                                  "pricePerUnit":180.50,
                                  "currency":"HKD",
                                  "purchasedAt":"2026-07-27T10:30:00Z"
                                }
                                """))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message").value("Failed to load exchange rates"));
    }

    /**
     * 中文：验证同资产重复 BUY 会累加持仓数量，而不是新增重复持仓行。
     * English: Verifies repeated BUY for same asset increases holding quantity instead of creating duplicate rows.
     */
    @Test
    void shouldUpdateExistingHoldingInsteadOfCreatingDuplicateRows() throws Exception {
        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "transactionType":"BUY",
                                  "assetType":"STOCK",
                                  "symbol":" aapl ",
                                  "quantity":5,
                                  "pricePerUnit":180.50,
                                  "currency":"usd",
                                  "purchasedAt":"2026-07-27T10:30:00Z"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "transactionType":"BUY",
                                  "assetType":"STOCK",
                                  "symbol":"AAPL",
                                  "quantity":2,
                                  "pricePerUnit":181.00,
                                  "currency":"USD",
                                  "purchasedAt":"2026-07-27T11:30:00Z"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/portfolio/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        PortfolioItem first = portfolioItemRepository.findAll().get(0);
        org.junit.jupiter.api.Assertions.assertEquals("AAPL", first.getSymbol());
        org.junit.jupiter.api.Assertions.assertEquals(0, first.getQuantity().compareTo(new java.math.BigDecimal("7.000000")));
    }

    /**
     * 中文：验证 SELL 查询路径可达；在无 SELL 数据时返回空数组。
     * English: Verifies SELL query path is reachable and returns empty array when no SELL records exist.
     */
    @Test
    void shouldAllowQueryingSellHistoryPathEvenWhenResultIsEmpty() throws Exception {
        mockMvc.perform(get("/api/transactions").queryParam("type", "SELL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    /**
     * 中文：验证清仓删除持仓时，同步删除该资产的全部购买历史。
     * English: Verifies full holding removal also deletes all buy history for that asset.
     */
    @Test
    void shouldDeleteBuyHistoryAfterPortfolioItemDeleted() throws Exception {
        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "transactionType":"BUY",
                                  "assetType":"STOCK",
                                  "symbol":"AAPL",
                                  "quantity":3,
                                  "pricePerUnit":180.50,
                                  "currency":"USD",
                                  "purchasedAt":"2026-07-27T10:30:00Z"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/portfolio/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        long itemId = portfolioItemRepository.findAll().get(0).getId();

        mockMvc.perform(delete("/api/portfolio/items/{id}", itemId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/transactions").queryParam("type", "BUY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(get("/api/portfolio/activities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].action").value("REMOVED"))
                .andExpect(jsonPath("$[0].symbol").value("AAPL"))
                .andExpect(jsonPath("$[0].quantity").value(3))
                .andExpect(jsonPath("$[0].remainingQuantity").value(0))
                .andExpect(jsonPath("$[1].action").value("ADDED"));
    }

    /**
     * 中文：验证部分减仓只更新剩余持仓数量，并保留原始买入历史用于成本计算。
     * English: Verifies partial removal only updates the remaining quantity and retains buy history for cost calculation.
     */
    @Test
    void shouldKeepBuyHistoryAfterPartialRemoval() throws Exception {
        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "transactionType":"BUY",
                                  "assetType":"STOCK",
                                  "symbol":"AAPL",
                                  "quantity":10,
                                  "pricePerUnit":180.50,
                                  "currency":"USD",
                                  "purchasedAt":"2026-07-27T10:30:00Z"
                                }
                                """))
                .andExpect(status().isCreated());

        long itemId = portfolioItemRepository.findAll().get(0).getId();

        mockMvc.perform(put("/api/portfolio/items/{id}/quantity", itemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "quantity": 6
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(6));

        mockMvc.perform(get("/api/transactions").queryParam("type", "BUY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].symbol").value("AAPL"));

        mockMvc.perform(get("/api/portfolio/activities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].action").value("REMOVED"))
                .andExpect(jsonPath("$[0].quantity").value(4))
                .andExpect(jsonPath("$[0].remainingQuantity").value(6))
                .andExpect(jsonPath("$[1].action").value("ADDED"));
    }
}

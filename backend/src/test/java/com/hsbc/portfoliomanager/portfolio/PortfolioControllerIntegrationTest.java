package com.hsbc.portfoliomanager.portfolio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
/**
 * 中文：Portfolio 控制器集成测试，覆盖查询/创建/删除及主要校验与异常路径。
 * English: Integration tests for Portfolio controller, covering query/create/delete and key validation/error paths.
 */
class PortfolioControllerIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private PortfolioItemRepository portfolioItemRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    /**
     * 中文：每次测试前清理数据，确保测试之间互不污染并可重复执行。
     * English: Cleans data before each test to keep tests isolated and repeatable.
     */
    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        portfolioItemRepository.deleteAll();
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    /**
     * 中文：验证空持仓场景返回 200 与空数组。
     * English: Verifies empty-portfolio scenario returns HTTP 200 with an empty array.
     */
    @Test
    void shouldReturnEmptyArrayWhenPortfolioHasNoItems() throws Exception {
        mockMvc.perform(get("/api/portfolio/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    /**
     * 中文：验证创建持仓时 symbol 会被 trim + uppercase 规范化。
     * English: Verifies symbol is normalized with trim + uppercase when creating a portfolio item.
     */
    @Test
    void shouldCreatePortfolioItemWithNormalizedSymbol() throws Exception {
        mockMvc.perform(post("/api/portfolio/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "assetType":"STOCK",
                                  "symbol":" aapl ",
                                  "quantity":10
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.symbol").value("AAPL"));
    }

    /**
     * 中文：验证查询持仓可返回多条记录。
     * English: Verifies portfolio listing returns multiple records.
     */
    @Test
    void shouldReturnAllPortfolioItems() throws Exception {
        mockMvc.perform(post("/api/portfolio/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "assetType":"STOCK",
                                  "symbol":"AAPL",
                                  "quantity":10
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/portfolio/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "assetType":"BOND",
                                  "symbol":"TBOND",
                                  "quantity":2
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/portfolio/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    /**
     * 中文：验证 quantity 非法（<=0）时返回字段级 400 错误。
     * English: Verifies field-level 400 validation error when quantity is invalid (<= 0).
     */
    @Test
    void shouldReturnValidationErrorWhenPortfolioQuantityIsInvalid() throws Exception {
        mockMvc.perform(post("/api/portfolio/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "assetType":"STOCK",
                                  "symbol":"AAPL",
                                  "quantity":0
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.quantity").exists());
    }

    /**
     * 中文：验证 symbol 为空白时返回字段级 400 错误。
     * English: Verifies field-level 400 validation error when symbol is blank.
     */
    @Test
    void shouldReturnValidationErrorWhenPortfolioSymbolIsBlank() throws Exception {
        mockMvc.perform(post("/api/portfolio/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "assetType":"STOCK",
                                  "symbol":"   ",
                                  "quantity":1
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.symbol").exists());
    }

    /**
     * 中文：验证删除已存在持仓返回 204，且后续查询结果中不再包含该记录。
     * English: Verifies deleting an existing item returns 204 and the item no longer appears in subsequent queries.
     */
    @Test
    void shouldDeleteExistingPortfolioItem() throws Exception {
        mockMvc.perform(post("/api/portfolio/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "assetType":"STOCK",
                                  "symbol":"AAPL",
                                  "quantity":10
                                }
                                """))
                .andExpect(status().isCreated());

        long id = portfolioItemRepository.findAll().get(0).getId();

        mockMvc.perform(delete("/api/portfolio/items/{id}", id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/portfolio/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    /**
     * 中文：验证删除不存在持仓时返回 404 与明确错误信息。
     * English: Verifies deleting a missing portfolio item returns 404 with a clear error message.
     */
    @Test
    void shouldReturn404WhenDeletingMissingPortfolioItem() throws Exception {
        mockMvc.perform(delete("/api/portfolio/items/{id}", 99999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Portfolio item 99999 was not found"));
    }
}

package com.hsbc.portfoliomanager.portfolio.transaction;

import com.hsbc.portfoliomanager.portfolio.holding.AssetMetadata;
import com.hsbc.portfoliomanager.portfolio.holding.AssetMetadataClient;

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
class PortfolioActivityControllerIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private ExchangeRateClient exchangeRateClient;

    @MockitoBean
    private AssetMetadataClient assetMetadataClient;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM portfolio_activities");
        jdbcTemplate.update("DELETE FROM transactions");
        jdbcTemplate.update("DELETE FROM portfolio_items");
        when(exchangeRateClient.isKnownCurrency(anyString())).thenReturn(true);
        when(assetMetadataClient.findBySymbol(anyString()))
                .thenAnswer(invocation -> {
                    String symbol = invocation.getArgument(0);
                    return new AssetMetadata(symbol, symbol + " Inc.", "NASDAQ", "USD");
                });
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void shouldReturnEmptyActivitiesWhenNoHistory() throws Exception {
        mockMvc.perform(get("/api/portfolio/activities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void shouldReturnActivitiesAfterCreatingTransaction() throws Exception {
        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "transactionType":"BUY",
                                  "assetType":"STOCK",
                                  "symbol":"AAPL",
                                  "quantity":10,
                                  "pricePerUnit":195.00,
                                  "currency":"USD",
                                  "purchasedAt":"2026-07-27T10:30:00Z"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/portfolio/activities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].action").value("ADDED"))
                .andExpect(jsonPath("$[0].symbol").value("AAPL"))
                .andExpect(jsonPath("$[0].quantity").value(10))
                .andExpect(jsonPath("$[0].pricePerUnit").value(195.00))
                .andExpect(jsonPath("$[0].currency").value("USD"))
                .andExpect(jsonPath("$[0].occurredAt").value("2026-07-27T10:30:00Z"));
    }

    @Test
    void shouldReturnActivitiesInDescendingOrder() throws Exception {
        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "transactionType":"BUY",
                                  "assetType":"STOCK",
                                  "symbol":"AAPL",
                                  "quantity":5,
                                  "pricePerUnit":180.00,
                                  "currency":"USD",
                                  "purchasedAt":"2026-07-27T09:00:00Z"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "transactionType":"BUY",
                                  "assetType":"STOCK",
                                  "symbol":"TSLA",
                                  "quantity":3,
                                  "pricePerUnit":250.00,
                                  "currency":"USD",
                                  "purchasedAt":"2026-07-27T15:00:00Z"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/portfolio/activities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].symbol").value("TSLA"))
                .andExpect(jsonPath("$[0].occurredAt").value("2026-07-27T15:00:00Z"))
                .andExpect(jsonPath("$[1].symbol").value("AAPL"))
                .andExpect(jsonPath("$[1].occurredAt").value("2026-07-27T09:00:00Z"));
    }

    @Test
    void shouldShowRemovalActivityAfterDeletingPortfolioItem() throws Exception {
        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "transactionType":"BUY",
                                  "assetType":"STOCK",
                                  "symbol":"MSFT",
                                  "quantity":10,
                                  "pricePerUnit":400.00,
                                  "currency":"USD",
                                  "purchasedAt":"2026-07-27T10:30:00Z"
                                }
                                """))
                .andExpect(status().isCreated());

        var items = jdbcTemplate.queryForList("SELECT id FROM portfolio_items");
        Long itemId = ((Number) items.get(0).get("id")).longValue();

        mockMvc.perform(delete("/api/portfolio/items/{id}", itemId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/portfolio/activities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].action").value("REMOVED"))
                .andExpect(jsonPath("$[0].symbol").value("MSFT"))
                .andExpect(jsonPath("$[0].quantity").value(10))
                .andExpect(jsonPath("$[0].remainingQuantity").value(0))
                .andExpect(jsonPath("$[1].action").value("ADDED"));
    }

    @Test
    void shouldShowRemainingQuantityInRemovalActivity() throws Exception {
        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "transactionType":"BUY",
                                  "assetType":"STOCK",
                                  "symbol":"GOOGL",
                                  "quantity":10,
                                  "pricePerUnit":150.00,
                                  "currency":"USD",
                                  "purchasedAt":"2026-07-27T10:30:00Z"
                                }
                                """))
                .andExpect(status().isCreated());

        var items = jdbcTemplate.queryForList("SELECT id FROM portfolio_items");
        Long itemId = ((Number) items.get(0).get("id")).longValue();

        mockMvc.perform(put("/api/portfolio/items/{id}/quantity", itemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "quantity": 4
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/portfolio/activities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].action").value("REMOVED"))
                .andExpect(jsonPath("$[0].symbol").value("GOOGL"))
                .andExpect(jsonPath("$[0].quantity").value(6))
                .andExpect(jsonPath("$[0].remainingQuantity").value(4))
                .andExpect(jsonPath("$[1].action").value("ADDED"));
    }
}

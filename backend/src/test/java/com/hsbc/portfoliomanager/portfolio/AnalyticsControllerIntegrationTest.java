package com.hsbc.portfoliomanager.portfolio;

import com.hsbc.portfoliomanager.marketdata.MarketDataService;
import com.hsbc.portfoliomanager.marketdata.MarketDataService.PriceData;
import com.hsbc.portfoliomanager.transaction.Transaction;
import com.hsbc.portfoliomanager.transaction.TransactionRepository;
import com.hsbc.portfoliomanager.transaction.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.Instant;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Analytics API Integration")
class AnalyticsControllerIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private PortfolioItemRepository portfolioItemRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @MockitoBean
    private MarketDataService marketDataService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        transactionRepository.deleteAll();
        portfolioItemRepository.deleteAll();
    }

    @Nested
    @DisplayName("GET /api/portfolio/value")
    class PortfolioValue {

        @Test
        @DisplayName("returns 200 with market values for all holdings")
        void returnsMarketValues() throws Exception {
            portfolioItemRepository.save(
                    new PortfolioItem(AssetType.STOCK, "AAPL", new BigDecimal("10")));

            when(marketDataService.getCurrentPrice("AAPL"))
                    .thenReturn(java.util.Optional.of(
                            new PriceData("AAPL", new BigDecimal("195.00"), "USD", Instant.now())));

            mockMvc.perform(get("/api/portfolio/value")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("COMPLETE"))
                    .andExpect(jsonPath("$.currency").value("USD"))
                    .andExpect(jsonPath("$.assets", hasSize(1)))
                    .andExpect(jsonPath("$.assets[0].symbol").value("AAPL"))
                    .andExpect(jsonPath("$.assets[0].quantity").value(10))
                    .andExpect(jsonPath("$.assets[0].currentPrice").value(195.00))
                    .andExpect(jsonPath("$.assets[0].marketValue").value(1950.00))
                    .andExpect(jsonPath("$.missingPrices").isEmpty());
        }

        @Test
        @DisplayName("returns 200 with UNAVAILABLE status when all prices are missing")
        void returnsUnavailableWhenPricesMissing() throws Exception {
            portfolioItemRepository.save(
                    new PortfolioItem(AssetType.STOCK, "AAPL", new BigDecimal("10")));

            when(marketDataService.getCurrentPrice(anyString()))
                    .thenReturn(java.util.Optional.empty());

            mockMvc.perform(get("/api/portfolio/value")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("UNAVAILABLE"))
                    .andExpect(jsonPath("$.missingPrices[0]").value("AAPL"));
        }

        @Test
        @DisplayName("returns 200 with empty assets when portfolio is empty")
        void emptyPortfolio() throws Exception {
            mockMvc.perform(get("/api/portfolio/value")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("COMPLETE"))
                    .andExpect(jsonPath("$.assets").isEmpty());
        }
    }

    @Nested
    @DisplayName("GET /api/portfolio/performance")
    class PortfolioPerformance {

        @Test
        @DisplayName("returns 200 with complete performance data")
        void returnsPerformanceData() throws Exception {
            portfolioItemRepository.save(
                    new PortfolioItem(AssetType.STOCK, "AAPL", new BigDecimal("10")));

            transactionRepository.save(new Transaction(
                    TransactionType.BUY, AssetType.STOCK, "AAPL",
                    new BigDecimal("10"), new BigDecimal("180.50"), "USD",
                    Instant.now().minusSeconds(86400)));

            when(marketDataService.getCurrentPrice("AAPL"))
                    .thenReturn(java.util.Optional.of(
                            new PriceData("AAPL", new BigDecimal("195.00"), "USD", Instant.now())));

            mockMvc.perform(get("/api/portfolio/performance")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("COMPLETE"))
                    .andExpect(jsonPath("$.currency").value("USD"))
                    .andExpect(jsonPath("$.totalCost").value(1805.00))
                    .andExpect(jsonPath("$.currentValue").value(1950.00))
                    .andExpect(jsonPath("$.unrealizedProfitLoss").value(145.00))
                    .andExpect(jsonPath("$.assets", hasSize(1)))
                    .andExpect(jsonPath("$.assets[0].symbol").value("AAPL"))
                    .andExpect(jsonPath("$.assets[0].averageCost").value(180.50))
                    .andExpect(jsonPath("$.assets[0].currentValue").value(1950.00))
                    .andExpect(jsonPath("$.assets[0].allocationPercentage").value(100.00));
        }

        @Test
        @DisplayName("returns 200 with zero values for empty portfolio")
        void emptyPortfolio() throws Exception {
            mockMvc.perform(get("/api/portfolio/performance")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("COMPLETE"))
                    .andExpect(jsonPath("$.totalCost").value(0))
                    .andExpect(jsonPath("$.currentValue").value(0))
                    .andExpect(jsonPath("$.unrealizedProfitLoss").value(0))
                    .andExpect(jsonPath("$.returnPercentage").value(0))
                    .andExpect(jsonPath("$.assets").isEmpty());
        }

        @Test
        @DisplayName("returns PARTIAL status when some prices are missing")
        void partialPerformance() throws Exception {
            portfolioItemRepository.save(
                    new PortfolioItem(AssetType.STOCK, "AAPL", new BigDecimal("10")));
            portfolioItemRepository.save(
                    new PortfolioItem(AssetType.STOCK, "MSFT", new BigDecimal("5")));

            when(marketDataService.getCurrentPrice("AAPL"))
                    .thenReturn(java.util.Optional.of(
                            new PriceData("AAPL", new BigDecimal("195.00"), "USD", Instant.now())));
            when(marketDataService.getCurrentPrice("MSFT"))
                    .thenReturn(java.util.Optional.empty());

            mockMvc.perform(get("/api/portfolio/performance")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("PARTIAL"))
                    .andExpect(jsonPath("$.missingPrices[0]").value("MSFT"));
        }
    }
}

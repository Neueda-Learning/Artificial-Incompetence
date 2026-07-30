package com.hsbc.portfoliomanager.portfolio.holding;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Import(PortfolioControllerIntegrationTest.AssetMetadataTestConfiguration.class)
class PortfolioControllerIntegrationTest {

    @Autowired
    private WebApplicationContext applicationContext;

    @Autowired
    private PortfolioItemRepository repository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext).build();
    }

    @Test
    void createsPortfolioItemAndNormalizesSymbol() throws Exception {
        mockMvc.perform(post("/api/portfolio/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "assetType": "STOCK",
                                  "symbol": "  aapl  ",
                                  "quantity": 10
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.assetType").value("STOCK"))
                .andExpect(jsonPath("$.symbol").value("AAPL"))
                .andExpect(jsonPath("$.companyName").value("Apple Inc."))
                .andExpect(jsonPath("$.exchange").value("NASDAQ"))
                .andExpect(jsonPath("$.quantity").value(10))
                .andExpect(jsonPath("$.currency").value("USD"));

        assertThat(repository.findAll())
                .singleElement()
                .satisfies(item -> {
                    assertThat(item.getAssetType()).isEqualTo(AssetType.STOCK);
                    assertThat(item.getSymbol()).isEqualTo("AAPL");
                    assertThat(item.getCompanyName()).isEqualTo("Apple Inc.");
                    assertThat(item.getExchange()).isEqualTo("NASDAQ");
                    assertThat(item.getQuantity()).isEqualByComparingTo(BigDecimal.TEN);
                    assertThat(item.getCurrency()).isEqualTo("USD");
                });
    }

    @Test
    void repeatedPortfolioItemCreationAddsQuantityInsteadOfViolatingUniqueConstraint() throws Exception {
        String request = """
                {
                  "assetType": "STOCK",
                  "symbol": "AAPL",
                  "quantity": 3
                }
                """;

        mockMvc.perform(post("/api/portfolio/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/portfolio/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.quantity").value(6));

        assertThat(repository.findAll())
                .singleElement()
                .satisfies(item ->
                        assertThat(item.getQuantity()).isEqualByComparingTo("6")
                );
    }

    @Test
    void rejectsBlankSymbolWithoutSaving() throws Exception {
        mockMvc.perform(post("/api/portfolio/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "assetType": "STOCK",
                                  "symbol": "   ",
                                  "quantity": 10
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.symbol").exists());

        assertThat(repository.count()).isZero();
    }

    @Test
    void rejectsNonPositiveQuantityWithoutSaving() throws Exception {
        mockMvc.perform(post("/api/portfolio/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "assetType": "STOCK",
                                  "symbol": "AAPL",
                                  "quantity": 0
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.quantity").exists());

        assertThat(repository.count()).isZero();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class AssetMetadataTestConfiguration {

        @Bean
        @Primary
        AssetMetadataClient assetMetadataClient() {
            return symbol -> new AssetMetadata(
                    symbol,
                    "Apple Inc.",
                    "NASDAQ",
                    "USD"
            );
        }
    }
}

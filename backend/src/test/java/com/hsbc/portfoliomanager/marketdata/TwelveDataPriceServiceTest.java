package com.hsbc.portfoliomanager.marketdata;

import com.hsbc.portfoliomanager.marketdata.MarketDataService.PriceData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TwelveDataPriceService")
class TwelveDataPriceServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private MarketDataConfig config;

    private TwelveDataPriceService priceService;

    @BeforeEach
    void setUp() {
        priceService = new TwelveDataPriceService(restTemplate, config);
    }

    @Test
    @DisplayName("parses current and previous-close prices from quote")
    void parsesPreviousClose() {
        when(config.getTwelveDataApiKey()).thenReturn("test-key");
        when(restTemplate.getForObject(
                "https://api.twelvedata.com/quote?symbol={symbol}&apikey={apikey}",
                Map.class,
                "AAPL",
                "test-key"
        )).thenReturn(Map.of(
                "symbol", "AAPL",
                "close", "195.00",
                "previous_close", "190.00",
                "currency", "USD"
        ));

        Optional<PriceData> result = priceService.fetchPrice("AAPL");

        assertThat(result).isPresent();
        assertThat(result.orElseThrow().price())
                .isEqualByComparingTo(new BigDecimal("195.00"));
        assertThat(result.orElseThrow().previousClose())
                .isEqualByComparingTo(new BigDecimal("190.00"));
        assertThat(result.orElseThrow().currency()).isEqualTo("USD");
    }
}

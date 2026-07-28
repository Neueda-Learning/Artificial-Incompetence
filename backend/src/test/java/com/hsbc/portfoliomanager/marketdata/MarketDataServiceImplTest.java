package com.hsbc.portfoliomanager.marketdata;

import com.hsbc.portfoliomanager.marketdata.MarketDataService.PriceData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MarketDataServiceImpl")
class MarketDataServiceImplTest {

    @Mock
    private TwelveDataPriceService priceService;

    @Mock
    private ExchangeRateService exchangeRateService;

    private MarketDataServiceImpl marketDataService;

    @BeforeEach
    void setUp() {
        marketDataService = new MarketDataServiceImpl(priceService, exchangeRateService);
    }

    @Test
    @DisplayName("delegates price fetching to TwelveDataPriceService")
    void delegatesToPriceService() {
        PriceData expected = new PriceData("AAPL", new BigDecimal("195.00"), "USD", Instant.now());
        when(priceService.fetchPrice("AAPL")).thenReturn(Optional.of(expected));

        Optional<PriceData> result = marketDataService.getCurrentPrice("AAPL");

        assertThat(result).isPresent();
        assertThat(result.get().symbol()).isEqualTo("AAPL");
        assertThat(result.get().price()).isEqualByComparingTo("195.00");
    }

    @Test
    @DisplayName("delegates currency conversion to ExchangeRateService")
    void delegatesToExchangeRateService() {
        when(exchangeRateService.convertToUsd(new BigDecimal("100"), "GBP"))
                .thenReturn(Optional.of(new BigDecimal("126.58")));

        Optional<BigDecimal> result = marketDataService.convertToUsd(new BigDecimal("100"), "GBP");

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualByComparingTo("126.58");
    }

    @Test
    @DisplayName("isAvailable reflects last price fetch success")
    void availabilityTracking() {
        // Initially true (default)
        assertThat(marketDataService.isAvailable()).isTrue();

        // Failed price fetch
        when(priceService.fetchPrice(anyString())).thenReturn(Optional.empty());
        marketDataService.getCurrentPrice("AAPL");
        assertThat(marketDataService.isAvailable()).isFalse();

        // Successful price fetch
        PriceData data = new PriceData("TSLA", new BigDecimal("250.00"), "USD", Instant.now());
        when(priceService.fetchPrice("TSLA")).thenReturn(Optional.of(data));
        marketDataService.getCurrentPrice("TSLA");
        assertThat(marketDataService.isAvailable()).isTrue();
    }
}

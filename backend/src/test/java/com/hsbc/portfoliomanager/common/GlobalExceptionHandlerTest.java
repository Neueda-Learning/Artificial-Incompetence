package com.hsbc.portfoliomanager.common;

import com.hsbc.portfoliomanager.marketdata.MarketDataUnavailableException;
import com.hsbc.portfoliomanager.portfolio.holding.AssetMetadataLookupException;
import com.hsbc.portfoliomanager.portfolio.holding.PortfolioItemNotFoundException;
import com.hsbc.portfoliomanager.portfolio.transaction.ExchangeRateUnavailableException;
import com.hsbc.portfoliomanager.portfolio.transaction.UnknownCurrencyException;
import com.hsbc.portfoliomanager.portfolio.transaction.UnsupportedTransactionTypeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    // ── AssetMetadataLookupException → 502 ─────────────────────────────────

    @Nested
    @DisplayName("AssetMetadataLookupException")
    class AssetMetadataLookup {

        @Test
        @DisplayName("returns 502 BAD_GATEWAY with exception message")
        void returns502BadGateway() {
            AssetMetadataLookupException exception = mock(AssetMetadataLookupException.class);
            when(exception.getMessage()).thenReturn("Failed to lookup metadata for AAPL");

            ApiError error = handler.handleAssetMetadataLookup(exception);

            assertThat(error.status()).isEqualTo(HttpStatus.BAD_GATEWAY.value());
            assertThat(error.message()).contains("AAPL");
            assertThat(error.fieldErrors()).isEmpty();
            assertThat(error.timestamp()).isNotNull();
        }
    }

    // ── PortfolioItemNotFoundException → 404 ────────────────────────────────

    @Nested
    @DisplayName("PortfolioItemNotFoundException")
    class PortfolioItemNotFound {

        @Test
        @DisplayName("returns 404 NOT_FOUND with item id in message")
        void returns404NotFound() {
            PortfolioItemNotFoundException exception = mock(PortfolioItemNotFoundException.class);
            when(exception.getMessage()).thenReturn("Portfolio item 42 was not found");

            ApiError error = handler.handleNotFound(exception);

            assertThat(error.status()).isEqualTo(HttpStatus.NOT_FOUND.value());
            assertThat(error.message()).contains("42");
            assertThat(error.fieldErrors()).isEmpty();
        }
    }

    // ── MethodArgumentNotValidException → 400 ───────────────────────────────

    @Nested
    @DisplayName("MethodArgumentNotValidException")
    class Validation {

        @Test
        @DisplayName("returns 400 BAD_REQUEST with field error map")
        void returns400WithFieldErrors() throws Exception {
            BeanPropertyBindingResult bindingResult =
                    new BeanPropertyBindingResult(new Object(), "request");
            bindingResult.addError(new FieldError("request", "symbol",
                    "must not be blank"));
            bindingResult.addError(new FieldError("request", "quantity",
                    "must be greater than 0"));

            MethodArgumentNotValidException exception =
                    new MethodArgumentNotValidException(null, bindingResult);

            ApiError error = handler.handleValidation(exception);

            assertThat(error.status()).isEqualTo(HttpStatus.BAD_REQUEST.value());
            assertThat(error.message()).isEqualTo("Request validation failed");
            assertThat(error.fieldErrors()).hasSize(2);
            assertThat(error.fieldErrors()).containsEntry("symbol", "must not be blank");
            assertThat(error.fieldErrors()).containsEntry("quantity", "must be greater than 0");
        }

        @Test
        @DisplayName("uses putIfAbsent to deduplicate field errors on the same field")
        void deduplicatesFieldErrors() throws Exception {
            BeanPropertyBindingResult bindingResult =
                    new BeanPropertyBindingResult(new Object(), "request");
            bindingResult.addError(new FieldError("request", "symbol",
                    "must not be blank"));
            bindingResult.addError(new FieldError("request", "symbol",
                    "size must be between 1 and 20"));

            MethodArgumentNotValidException exception =
                    new MethodArgumentNotValidException(null, bindingResult);

            ApiError error = handler.handleValidation(exception);

            assertThat(error.fieldErrors()).hasSize(1);
            assertThat(error.fieldErrors()).containsEntry("symbol", "must not be blank");
        }
    }

    // ── UnsupportedTransactionTypeException → 400 ───────────────────────────

    @Nested
    @DisplayName("UnsupportedTransactionTypeException")
    class UnsupportedTransactionType {

        @Test
        @DisplayName("returns 400 with 'transactionType' field error")
        void returns400WithFieldError() {
            UnsupportedTransactionTypeException exception = mock(UnsupportedTransactionTypeException.class);
            when(exception.getMessage()).thenReturn(
                    "Only BUY transactions are supported for now, but received: SELL");

            ApiError error = handler.handleUnsupportedTransactionType(exception);

            assertThat(error.status()).isEqualTo(HttpStatus.BAD_REQUEST.value());
            assertThat(error.message()).isEqualTo("Request validation failed");
            assertThat(error.fieldErrors()).containsEntry("transactionType",
                    "Only BUY transactions are supported for now, but received: SELL");
            assertThat(error.fieldErrors().get("transactionType")).contains("SELL");
        }
    }

    // ── UnknownCurrencyException → 400 ──────────────────────────────────────

    @Nested
    @DisplayName("UnknownCurrencyException")
    class UnknownCurrency {

        @Test
        @DisplayName("returns 400 with 'currency' field error")
        void returns400WithFieldError() {
            UnknownCurrencyException exception = mock(UnknownCurrencyException.class);
            when(exception.getMessage()).thenReturn("Unknown currency: ZZZ");

            ApiError error = handler.handleUnknownCurrency(exception);

            assertThat(error.status()).isEqualTo(HttpStatus.BAD_REQUEST.value());
            assertThat(error.message()).isEqualTo("Request validation failed");
            assertThat(error.fieldErrors()).containsEntry("currency", "Unknown currency: ZZZ");
        }
    }

    // ── ExchangeRateUnavailableException → 502 ──────────────────────────────

    @Nested
    @DisplayName("ExchangeRateUnavailableException")
    class ExchangeRateUnavailable {

        @Test
        @DisplayName("returns 502 BAD_GATEWAY with exception message")
        void returns502BadGateway() {
            ExchangeRateUnavailableException exception = mock(ExchangeRateUnavailableException.class);
            when(exception.getMessage()).thenReturn("Server error");

            ApiError error = handler.handleExchangeRateUnavailable(exception);

            assertThat(error.status()).isEqualTo(HttpStatus.BAD_GATEWAY.value());
            assertThat(error.message()).isEqualTo("Server error");
            assertThat(error.fieldErrors()).isEmpty();
        }
    }

    // ── MarketDataUnavailableException → 503 ────────────────────────────────

    @Nested
    @DisplayName("MarketDataUnavailableException")
    class MarketDataUnavailable {

        @Test
        @DisplayName("returns 503 SERVICE_UNAVAILABLE with exception message")
        void returns503ServiceUnavailable() {
            MarketDataUnavailableException exception = mock(MarketDataUnavailableException.class);
            when(exception.getMessage()).thenReturn("Market data temporarily unavailable");

            ApiError error = handler.handleMarketDataUnavailable(exception);

            assertThat(error.status()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE.value());
            assertThat(error.message()).isEqualTo("Market data temporarily unavailable");
            assertThat(error.fieldErrors()).isEmpty();
        }
    }

    // Simple object for binding result construction
    private static class Object {
    }
}

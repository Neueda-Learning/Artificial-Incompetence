package com.hsbc.portfoliomanager.common;

import com.hsbc.portfoliomanager.portfolio.AssetMetadataLookupException;
import com.hsbc.portfoliomanager.portfolio.ExchangeRateUnavailableException;
import com.hsbc.portfoliomanager.portfolio.MarketDataUnavailableException;
import com.hsbc.portfoliomanager.portfolio.PortfolioItemNotFoundException;
import com.hsbc.portfoliomanager.portfolio.UnknownCurrencyException;
import com.hsbc.portfoliomanager.portfolio.UnsupportedTransactionTypeException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(AssetMetadataLookupException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    ApiError handleAssetMetadataLookup(AssetMetadataLookupException exception) {
        return new ApiError(
                Instant.now(),
                HttpStatus.BAD_GATEWAY.value(),
                exception.getMessage(),
                Map.of()
        );
    }

    @ExceptionHandler(PortfolioItemNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    ApiError handleNotFound(PortfolioItemNotFoundException exception) {
        return new ApiError(
                Instant.now(),
                HttpStatus.NOT_FOUND.value(),
                exception.getMessage(),
                Map.of()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ApiError handleValidation(MethodArgumentNotValidException exception) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error ->
                fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage())
        );

        return new ApiError(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Request validation failed",
                fieldErrors
        );
    }

    @ExceptionHandler(UnsupportedTransactionTypeException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ApiError handleUnsupportedTransactionType(UnsupportedTransactionTypeException exception) {
        return new ApiError(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Request validation failed",
                Map.of("transactionType", exception.getMessage())
        );
    }

    @ExceptionHandler(UnknownCurrencyException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ApiError handleUnknownCurrency(UnknownCurrencyException exception) {
        return new ApiError(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Request validation failed",
                Map.of("currency", exception.getMessage())
        );
    }

    @ExceptionHandler(ExchangeRateUnavailableException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    ApiError handleExchangeRateUnavailable(ExchangeRateUnavailableException exception) {
        return new ApiError(
                Instant.now(),
                HttpStatus.BAD_GATEWAY.value(),
                exception.getMessage(),
                Map.of()
        );
    }

    @ExceptionHandler(MarketDataUnavailableException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    ApiError handleMarketDataUnavailable(MarketDataUnavailableException exception) {
        return new ApiError(
                Instant.now(),
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                exception.getMessage(),
                Map.of()
        );
    }
}
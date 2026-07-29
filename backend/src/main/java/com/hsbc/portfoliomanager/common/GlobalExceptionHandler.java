package com.hsbc.portfoliomanager.common;


import com.hsbc.portfoliomanager.marketdata.MarketDataUnavailableException;
import com.hsbc.portfoliomanager.portfolio.holding.AssetMetadataLookupException;
import com.hsbc.portfoliomanager.portfolio.holding.PortfolioItemNotFoundException;
import com.hsbc.portfoliomanager.portfolio.transaction.ExchangeRateUnavailableException;
import com.hsbc.portfoliomanager.portfolio.transaction.UnknownCurrencyException;
import com.hsbc.portfoliomanager.portfolio.transaction.UnsupportedTransactionTypeException;
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

    /**
     * 中文：将资产元数据上游调用失败转换为 502 API 错误。
     * English: Converts upstream asset-metadata failures into a 502 API error.
     */
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

    /**
     * 中文：将持仓不存在异常转换为 404 API 错误。
     * English: Converts a missing portfolio item into a 404 API error.
     */
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

    /**
     * 中文：收集请求字段校验错误并转换为统一的 400 响应。
     * English: Collects request field-validation failures into a consistent 400 response.
     */
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

    /**
     * 中文：将当前不支持的交易类型转换为带字段信息的 400 响应。
     * English: Converts an unsupported transaction type into a field-specific 400 response.
     */
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

    /**
     * 中文：将未知币种转换为带 currency 字段信息的 400 响应。
     * English: Converts an unknown currency into a currency-specific 400 response.
     */
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

    /**
     * 中文：将上游汇率服务失败转换为 502 API 错误。
     * English: Converts an upstream exchange-rate failure into a 502 API error.
     */
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

    /**
     * 中文：将市场数据不可用异常转换为 503 API 错误。
     * English: Converts market-data unavailability into a 503 API error.
     */
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

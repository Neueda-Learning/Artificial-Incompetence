package com.hsbc.portfoliomanager.common;

import com.hsbc.portfoliomanager.portfolio.PortfolioItemNotFoundException;
import com.hsbc.portfoliomanager.portfolio.ExchangeRateUnavailableException;
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

    /**
     * 中文：将“持仓不存在”业务异常统一映射为 404，保证接口错误格式一致。
     * English: Maps the "portfolio item not found" business exception to HTTP 404 with a consistent error payload.
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
     * 中文：处理 Bean Validation 触发的字段级校验错误，并返回字段->错误消息映射。
     * English: Handles Bean Validation field-level errors and returns a field-to-message map.
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
     * 中文：当前版本只允许 BUY，若传入其他交易类型则返回 400 并定位到 transactionType 字段。
     * English: Only BUY is supported for now; unsupported transaction types return HTTP 400 on transactionType.
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
     * 中文：当请求币种不在汇率服务返回列表中时返回 400，提示客户端修正 currency 输入。
     * English: Returns HTTP 400 when requested currency is not recognized by the exchange-rate source.
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
     * 中文：汇率服务不可达时返回 502，明确这是上游依赖异常而非业务输入问题。
     * English: Returns HTTP 502 when exchange-rate service is unavailable, indicating an upstream dependency failure.
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
}

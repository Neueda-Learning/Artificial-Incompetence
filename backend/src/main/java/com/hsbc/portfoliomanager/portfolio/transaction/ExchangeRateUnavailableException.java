package com.hsbc.portfoliomanager.portfolio.transaction;

/**
 * 中文：表示上游汇率服务不可用或调用失败。
 * English: Indicates that the upstream exchange-rate service is unavailable or failed.
 */
public class ExchangeRateUnavailableException extends RuntimeException {

    /**
     * 中文：使用业务错误信息和上游调用异常创建汇率服务异常。
     * English: Creates an exchange-rate service exception with a business message and upstream cause.
     */
    ExchangeRateUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}

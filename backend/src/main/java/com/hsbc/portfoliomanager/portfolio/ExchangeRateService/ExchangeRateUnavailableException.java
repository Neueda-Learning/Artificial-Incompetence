package com.hsbc.portfoliomanager.portfolio;

/**
 * 中文：表示上游汇率服务不可用或调用失败。
 * English: Indicates that the upstream exchange-rate service is unavailable or failed.
 */
public class ExchangeRateUnavailableException extends RuntimeException {

    ExchangeRateUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}

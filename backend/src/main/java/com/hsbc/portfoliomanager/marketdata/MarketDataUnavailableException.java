package com.hsbc.portfoliomanager.marketdata;

public class MarketDataUnavailableException extends RuntimeException {

    /**
     * 中文：使用指定错误信息创建市场数据不可用异常。
     * English: Creates a market data unavailable exception with the specified message.
     */
    public MarketDataUnavailableException(String message) {
        super(message);
    }

    /**
     * 中文：使用指定错误信息和原始异常创建市场数据不可用异常。
     * English: Creates a market data unavailable exception with the specified message and original cause.
     */
    public MarketDataUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}

package com.hsbc.portfoliomanager.portfolio.transaction;

/**
 * 中文：当汇率服务无法识别输入币种时抛出，提示客户端修正 currency 参数。
 * English: Thrown when the exchange-rate source cannot recognize the input currency code.
 */
public class UnknownCurrencyException extends RuntimeException {

    /**
     * 中文：根据无法识别的币种代码创建异常。
     * English: Creates an exception for a currency code that cannot be recognized.
     */
    UnknownCurrencyException(String currency) {
        super("Unknown currency: %s".formatted(currency));
    }
}

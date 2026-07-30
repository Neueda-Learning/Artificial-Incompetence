package com.hsbc.portfoliomanager.portfolio.transaction;

/**
 * 中文：当请求交易类型超出当前业务支持范围时抛出。
 * English: Thrown when requested transaction type is outside the currently supported business scope.
 */
public class UnsupportedTransactionTypeException extends RuntimeException {

    /**
     * 中文：根据当前不支持的交易类型创建异常。
     * English: Creates an exception for a transaction type that is not currently supported.
     */
    UnsupportedTransactionTypeException(TransactionType type) {
        super("Only BUY transactions are supported for now, but received: %s".formatted(type));
    }
}

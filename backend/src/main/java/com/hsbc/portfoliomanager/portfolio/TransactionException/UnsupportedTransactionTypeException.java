package com.hsbc.portfoliomanager.portfolio;

/**
 * 中文：当请求交易类型超出当前业务支持范围时抛出。
 * English: Thrown when requested transaction type is outside the currently supported business scope.
 */
public class UnsupportedTransactionTypeException extends RuntimeException {

    UnsupportedTransactionTypeException(TransactionType type) {
        super("Only BUY transactions are supported for now, but received: %s".formatted(type));
    }
}

package com.hsbc.portfoliomanager.portfolio.holding;

public class AssetMetadataLookupException extends RuntimeException {

    /**
     * 中文：使用指定错误信息创建资产元数据查询异常。
     * English: Creates an asset metadata lookup exception with the specified error message.
     */
    AssetMetadataLookupException(String message) {
        super(message);
    }

    /**
     * 中文：使用指定错误信息和原始异常创建资产元数据查询异常。
     * English: Creates an asset metadata lookup exception with the specified message and original cause.
     */
    AssetMetadataLookupException(String message, Throwable cause) {
        super(message, cause);
    }
}

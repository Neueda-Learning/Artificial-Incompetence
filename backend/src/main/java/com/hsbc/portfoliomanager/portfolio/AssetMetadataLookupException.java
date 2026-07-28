package com.hsbc.portfoliomanager.portfolio;

public class AssetMetadataLookupException extends RuntimeException {

    AssetMetadataLookupException(String message) {
        super(message);
    }

    AssetMetadataLookupException(String message, Throwable cause) {
        super(message, cause);
    }
}

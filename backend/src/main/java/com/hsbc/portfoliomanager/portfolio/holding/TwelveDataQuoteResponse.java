package com.hsbc.portfoliomanager.portfolio.holding;

import com.fasterxml.jackson.annotation.JsonProperty;

record TwelveDataQuoteResponse(
        String symbol,
        String name,
        String exchange,
        String currency,
        @JsonProperty("mic_code") String micCode,
        Integer code,
        String status,
        String message
) {
}

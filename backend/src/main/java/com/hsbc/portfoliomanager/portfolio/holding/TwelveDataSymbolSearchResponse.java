package com.hsbc.portfoliomanager.portfolio.holding;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

record TwelveDataSymbolSearchResponse(
        List<Result> data,
        String status,
        Integer code,
        String message
) {
    record Result(
            String symbol,
            @JsonProperty("instrument_name") String instrumentName,
            String exchange,
            @JsonProperty("mic_code") String micCode,
            @JsonProperty("instrument_type") String instrumentType,
            String country,
            String currency
    ) {
    }
}

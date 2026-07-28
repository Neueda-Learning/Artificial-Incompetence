package com.hsbc.portfoliomanager.common;

import java.time.Instant;
import java.util.Map;

record ApiError(
        Instant timestamp,
        int status,
        String message,
        Map<String, String> fieldErrors
) {
}


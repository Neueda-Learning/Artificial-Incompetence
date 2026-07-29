package com.hsbc.portfoliomanager.portfolio.transaction;

import com.hsbc.portfoliomanager.portfolio.holding.AssetType;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 中文：创建交易请求模型，覆盖 US-05 所需字段，并通过注解进行基础输入约束。
 * English: Request model for creating transactions, covering US-05 fields with annotation-based validation.
 */
record CreateTransactionRequest(
        @NotNull TransactionType transactionType,
        @NotNull AssetType assetType,
        @NotBlank @Size(max = 20) String symbol,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal quantity,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal pricePerUnit,
        @NotBlank @Size(min = 3, max = 3) String currency,
        @NotNull Instant purchasedAt
) {
}

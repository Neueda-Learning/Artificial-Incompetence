package com.hsbc.portfoliomanager.portfolio.holding;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

record CreatePortfolioItemRequest(
        @NotNull AssetType assetType,
        @NotBlank @Size(max = 20) String symbol,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal quantity
) {
}

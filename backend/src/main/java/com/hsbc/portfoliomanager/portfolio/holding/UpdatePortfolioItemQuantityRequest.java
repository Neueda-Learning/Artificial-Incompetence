package com.hsbc.portfoliomanager.portfolio.holding;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * 中文：部分减仓后的剩余持仓数量请求。
 * English: Request containing the remaining holding quantity after a partial removal.
 */
record UpdatePortfolioItemQuantityRequest(
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal quantity
) {
}

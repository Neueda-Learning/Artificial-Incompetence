package com.hsbc.portfoliomanager.portfolio.holding;

public class PortfolioItemNotFoundException extends RuntimeException {

    /**
     * 中文：根据未找到的持仓 ID 创建异常。
     * English: Creates an exception for the holding identifier that could not be found.
     */
    PortfolioItemNotFoundException(Long id) {
        super("Portfolio item %d was not found".formatted(id));
    }
}

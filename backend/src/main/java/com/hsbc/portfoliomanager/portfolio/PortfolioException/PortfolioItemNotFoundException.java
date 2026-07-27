package com.hsbc.portfoliomanager.portfolio;

public class PortfolioItemNotFoundException extends RuntimeException {

    PortfolioItemNotFoundException(Long id) {
        super("Portfolio item %d was not found".formatted(id));
    }
}

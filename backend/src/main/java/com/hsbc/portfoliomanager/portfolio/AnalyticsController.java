package com.hsbc.portfoliomanager.portfolio;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/portfolio")
class AnalyticsController {

    private final AnalyticsService analyticsService;

    AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/value")
    @ResponseStatus(HttpStatus.OK)
    PortfolioValueResponse getCurrentValue() {
        return analyticsService.calculateCurrentValue();
    }

    @GetMapping("/performance")
    @ResponseStatus(HttpStatus.OK)
    PortfolioPerformanceResponse getPerformance() {
        return analyticsService.calculatePerformance();
    }
}

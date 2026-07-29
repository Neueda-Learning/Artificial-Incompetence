package com.hsbc.portfoliomanager.portfolio.analytics;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/portfolio")
class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final HistoricalPerformanceService historicalPerformanceService;

    AnalyticsController(
            AnalyticsService analyticsService,
            HistoricalPerformanceService historicalPerformanceService
    ) {
        this.analyticsService = analyticsService;
        this.historicalPerformanceService = historicalPerformanceService;
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

    @GetMapping("/performance/history")
    @ResponseStatus(HttpStatus.OK)
    HistoricalPerformanceResponse getHistoricalPerformance(
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "1M") String range
    ) {
        return historicalPerformanceService.calculate(range);
    }
}

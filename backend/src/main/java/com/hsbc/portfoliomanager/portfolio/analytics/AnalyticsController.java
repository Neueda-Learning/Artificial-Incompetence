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

    /**
     * 中文：注入当前分析服务和历史绩效服务。
     * English: Injects the current analytics service and historical performance service.
     */
    AnalyticsController(
            AnalyticsService analyticsService,
            HistoricalPerformanceService historicalPerformanceService
    ) {
        this.analyticsService = analyticsService;
        this.historicalPerformanceService = historicalPerformanceService;
    }

    /**
     * 中文：计算并返回当前各资产的价格和市值。
     * English: Calculates and returns the current price and market value of each asset.
     */
    @GetMapping("/value")
    @ResponseStatus(HttpStatus.OK)
    PortfolioValueResponse getCurrentValue() {
        return analyticsService.calculateCurrentValue();
    }

    /**
     * 中文：计算并返回当前组合成本、盈亏、收益率和资产占比。
     * English: Calculates and returns portfolio cost, profit or loss, return, and asset allocation.
     */
    @GetMapping("/performance")
    @ResponseStatus(HttpStatus.OK)
    PortfolioPerformanceResponse getPerformance() {
        return analyticsService.calculatePerformance();
    }

    /**
     * 中文：根据指定时间范围计算并返回历史组合绩效序列。
     * English: Calculates and returns the portfolio performance series for the requested time range.
     */
    @GetMapping("/performance/history")
    @ResponseStatus(HttpStatus.OK)
    HistoricalPerformanceResponse getHistoricalPerformance(
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "1M") String range
    ) {
        return historicalPerformanceService.calculate(range);
    }
}

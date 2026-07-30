package com.hsbc.portfoliomanager.portfolio.activity;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/portfolio/activities")
class PortfolioActivityController {

    private final PortfolioActivityService service;

    PortfolioActivityController(PortfolioActivityService service) {
        this.service = service;
    }

    /**
     * 中文：为 Dashboard Recent Activity 和 Holdings History 提供同一个数据库活动源。
     * English: Supplies one database-backed activity source to Dashboard Recent Activity and Holdings History.
     */
    @GetMapping
    List<PortfolioActivityResponse> findAll() {
        return service.findAll();
    }
}

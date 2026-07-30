package com.hsbc.portfoliomanager.portfolio.activity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface PortfolioActivityRepository extends JpaRepository<PortfolioActivity, Long> {
    List<PortfolioActivity> findAllByOrderByOccurredAtDescIdDesc();

    List<PortfolioActivity> findAllByOrderByOccurredAtAscIdAsc();

    List<PortfolioActivity> findAllByActionOrderByOccurredAtDescIdDesc(
            PortfolioActivityAction action
    );
}

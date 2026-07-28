package com.hsbc.portfoliomanager.portfolio;

import org.springframework.data.jpa.repository.JpaRepository;

interface PortfolioItemRepository extends JpaRepository<PortfolioItem, Long> {
}


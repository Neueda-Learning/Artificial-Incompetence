package com.hsbc.portfoliomanager.marketdata;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

interface ExchangeRateHistoryRepository extends JpaRepository<ExchangeRateHistory, Long> {

    Optional<ExchangeRateHistory> findByBaseCurrencyAndQuoteCurrencyAndRateDate(
            String baseCurrency,
            String quoteCurrency,
            LocalDate rateDate
    );
}

package com.hsbc.portfoliomanager.marketdata;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

interface ExchangeRateHistoryRepository extends JpaRepository<ExchangeRateHistory, Long> {

    /**
     * 中文：按基础币种、目标币种和日期查询唯一的历史汇率记录。
     * English: Finds the unique historical rate by base currency, quote currency, and date.
     */
    Optional<ExchangeRateHistory> findByBaseCurrencyAndQuoteCurrencyAndRateDate(
            String baseCurrency,
            String quoteCurrency,
            LocalDate rateDate
    );
}

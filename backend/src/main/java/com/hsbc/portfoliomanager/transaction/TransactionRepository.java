package com.hsbc.portfoliomanager.transaction;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByTransactionType(TransactionType transactionType);

    List<Transaction> findByTransactionTypeAndSymbol(TransactionType transactionType, String symbol);
}

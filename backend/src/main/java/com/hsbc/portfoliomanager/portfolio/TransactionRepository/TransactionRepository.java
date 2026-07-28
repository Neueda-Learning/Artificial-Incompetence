package com.hsbc.portfoliomanager.portfolio;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface TransactionRepository extends JpaRepository<TransactionRecord, Long> {
    /**
     * 中文：按类型查询交易并按时间倒序，直接满足“购买历史从新到旧”的查询要求。
     * English: Queries transactions by type in descending transaction time order for newest-first history.
     */
    List<TransactionRecord> findByTransactionTypeOrderByTransactedAtDesc(TransactionType transactionType);

    /**
     * Returns the complete ledger in chronological order for historical portfolio reconstruction.
     */
    List<TransactionRecord> findAllByOrderByTransactedAtAsc();
}

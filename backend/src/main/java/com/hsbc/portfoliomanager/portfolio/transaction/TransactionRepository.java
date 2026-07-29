package com.hsbc.portfoliomanager.portfolio.transaction;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionRepository extends JpaRepository<TransactionRecord, Long> {
    /**
     * 中文：按类型查询交易并按时间倒序，直接满足“购买历史从新到旧”的查询要求。
     * English: Queries transactions by type in descending transaction time order for newest-first history.
     */
    List<TransactionRecord> findByTransactionTypeOrderByTransactedAtDesc(TransactionType transactionType);

    /**
     * 中文：按时间顺序返回完整交易账本，用于重建历史投资组合。
     * English: Returns the complete ledger in chronological order for historical portfolio reconstruction.
     */
    List<TransactionRecord> findAllByOrderByTransactedAtAsc();
}

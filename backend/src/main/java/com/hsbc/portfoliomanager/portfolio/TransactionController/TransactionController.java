package com.hsbc.portfoliomanager.portfolio;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
class TransactionController {

    private final TransactionService service;

    TransactionController(TransactionService service) {
        this.service = service;
    }

    /**
     * 中文：创建一笔交易记录。当前业务仅支持 BUY，创建成功后返回 201 与交易详情。
     * English: Creates one transaction record. Current business scope supports BUY only; returns 201 on success.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    TransactionResponse create(@Valid @RequestBody CreateTransactionRequest request) {
        return service.create(request);
    }

    /**
     * 中文：按交易类型查询历史记录，默认查询 BUY，并按交易时间倒序返回。
     * English: Returns transaction history by type (default BUY), ordered by transacted time descending.
     */
    @GetMapping
    List<TransactionResponse> findByType(@RequestParam(name = "type", defaultValue = "BUY") TransactionType type) {
        return service.findByType(type);
    }
}

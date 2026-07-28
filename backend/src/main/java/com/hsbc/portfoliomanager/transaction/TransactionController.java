package com.hsbc.portfoliomanager.transaction;

import com.hsbc.portfoliomanager.portfolio.AssetType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionRepository repository;

    TransactionController(TransactionRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    List<Transaction> findAll(@RequestParam(required = false) String type) {
        if ("BUY".equalsIgnoreCase(type)) {
            return repository.findByTransactionType(TransactionType.BUY);
        }
        return repository.findAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    Transaction create(@Valid @RequestBody CreateTransactionRequest request) {
        Transaction tx = new Transaction(
                request.transactionType(),
                request.assetType(),
                request.symbol().trim().toUpperCase(),
                request.quantity(),
                request.pricePerUnit(),
                request.currency(),
                request.purchasedAt()
        );
        return repository.save(tx);
    }

    record CreateTransactionRequest(
            @NotNull TransactionType transactionType,
            @NotNull AssetType assetType,
            @NotBlank @Size(max = 20) String symbol,
            @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal quantity,
            @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal pricePerUnit,
            @NotBlank String currency,
            @NotNull Instant purchasedAt
    ) {
    }
}

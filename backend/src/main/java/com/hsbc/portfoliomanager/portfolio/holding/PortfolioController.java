package com.hsbc.portfoliomanager.portfolio.holding;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@RestController
@RequestMapping("/api/portfolio/items")
class PortfolioController {

    private final PortfolioService service;

    /**
     * 中文：注入持仓业务服务。
     * English: Injects the portfolio holding service used by this controller.
     */
    PortfolioController(PortfolioService service) {
        this.service = service;
    }

    /**
     * 中文：返回数据库中的全部当前持仓。
     * English: Returns all current portfolio holdings stored in the database.
     */
    @GetMapping
    List<PortfolioItemResponse> findAll() {
        return service.findAll();
    }

    /**
     * 中文：校验请求、查询资产元数据并创建一条持仓。
     * English: Validates the request, retrieves asset metadata, and creates a holding.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    PortfolioItemResponse create(@Valid @RequestBody CreatePortfolioItemRequest request) {
        return service.create(request);
    }

    /**
     * 中文：部分减仓时根据持仓 ID 更新剩余数量，同时保留交易历史。
     * English: Updates the remaining quantity for a partial removal while retaining transaction history.
     */
    @PutMapping("/{id}/quantity")
    PortfolioItemResponse updateQuantity(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePortfolioItemQuantityRequest request
    ) {
        return service.updateQuantity(id, request);
    }

    /**
     * 中文：根据持仓 ID 清仓，并同步删除该资产的全部交易历史。
     * English: Fully removes a holding by identifier and deletes all transaction history for that asset.
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable Long id) {
        service.delete(id);
    }
}

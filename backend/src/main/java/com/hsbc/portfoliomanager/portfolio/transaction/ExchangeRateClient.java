package com.hsbc.portfoliomanager.portfolio.transaction;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.util.Map;

@Component
class ExchangeRateClient {

    private final RestClient restClient;
    private final String exchangeRateUrl;

    /**
     * 中文：使用配置的汇率服务地址创建币种校验客户端。
     * English: Creates a currency validation client using the configured exchange-rate service URL.
     */
    ExchangeRateClient(
            @Value("${exchange-rate.url:https://open.er-api.com/v6/latest/USD}") String exchangeRateUrl
    ) {
        this.restClient = RestClient.create();
        this.exchangeRateUrl = exchangeRateUrl;
    }

    /**
     * 中文：校验币种是否可识别。USD 直接通过；其他币种通过汇率接口返回的 rates 键集合判断。
     * English: Validates whether a currency is recognized. USD is accepted directly; others are checked against
     * the exchange-rate API "rates" keys.
     */
    boolean isKnownCurrency(String currency) {
        if ("USD".equals(currency)) {
            return true;
        }

        try {
            ExchangeRateResponse response = restClient.get()
                    .uri(exchangeRateUrl)
                    .retrieve()
                    .body(ExchangeRateResponse.class);

            if (response == null || response.rates() == null) {
                return false;
            }
            return response.rates().containsKey(currency);
        } catch (RestClientException exception) {
            // 中文：向上抛出语义化异常，由全局异常处理器统一转换为 502。
            // English: Wrap and rethrow as a semantic exception, then map to HTTP 502 in global handler.
            throw new ExchangeRateUnavailableException("Failed to load exchange rates", exception);
        }
    }

    /**
     * 中文：只映射当前需要的字段，避免耦合第三方响应的全部结构。
     * English: Maps only required fields to avoid coupling with the full third-party response payload.
     */
    record ExchangeRateResponse(Map<String, BigDecimal> rates) {
    }
}

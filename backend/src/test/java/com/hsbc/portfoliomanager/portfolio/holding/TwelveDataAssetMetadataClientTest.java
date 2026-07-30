package com.hsbc.portfoliomanager.portfolio.holding;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * 中文：Twelve Data 资产解析客户端测试，覆盖全球代码搜索、市场自动选择和套餐错误提示。
 * English: Tests global ticker search, automatic market selection, and plan-aware errors for the Twelve Data client.
 */
class TwelveDataAssetMetadataClientTest {

    /**
     * 中文：验证输入 00700.HK 时自动选中香港 MIC，并保存 Twelve Data 的规范代码和币种。
     * English: Verifies 00700.HK automatically selects the Hong Kong MIC and returns canonical provider metadata.
     */
    @Test
    void shouldResolveHongKongSuffixWithoutExchangeInput() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        TwelveDataAssetMetadataClient client = client(builder);

        server.expect(requestTo(containsString("/symbol_search")))
                .andExpect(requestTo(containsString("symbol=00700")))
                .andExpect(requestTo(containsString("apikey=test-key")))
                .andRespond(withSuccess("""
                        {
                          "data": [
                            {
                              "symbol": "0700",
                              "instrument_name": "Tencent Holdings Ltd",
                              "exchange": "Frankfurt",
                              "mic_code": "XFRA",
                              "instrument_type": "Common Stock",
                              "country": "Germany",
                              "currency": "EUR"
                            },
                            {
                              "symbol": "0700",
                              "instrument_name": "Tencent Holdings Ltd",
                              "exchange": "HKEX",
                              "mic_code": "XHKG",
                              "instrument_type": "Common Stock",
                              "country": "Hong Kong",
                              "currency": "HKD"
                            }
                          ],
                          "status": "ok"
                        }
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo(containsString("/quote")))
                .andExpect(requestTo(containsString("symbol=0700")))
                .andExpect(requestTo(containsString("mic_code=XHKG")))
                .andRespond(withSuccess("""
                        {
                          "symbol": "0700",
                          "name": "Tencent Holdings Ltd",
                          "exchange": "HKEX",
                          "mic_code": "XHKG",
                          "currency": "HKD",
                          "status": "ok"
                        }
                        """, MediaType.APPLICATION_JSON));

        AssetMetadata metadata = client.findBySymbol("00700.HK");

        assertThat(metadata).isEqualTo(
                new AssetMetadata("0700", "Tencent Holdings Ltd", "HKEX", "HKD")
        );
        server.verify();
    }

    /**
     * 中文：验证搜索到股票但报价受套餐限制时返回可操作的错误信息。
     * English: Verifies a plan-restricted quote produces an actionable error after a listing is found.
     */
    @Test
    void shouldExplainWhenTheApiPlanCannotQuoteTheListing() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        TwelveDataAssetMetadataClient client = client(builder);

        server.expect(requestTo(containsString("/symbol_search")))
                .andRespond(withSuccess("""
                        {
                          "data": [{
                            "symbol": "7203",
                            "instrument_name": "Toyota Motor Corp",
                            "exchange": "JPX",
                            "mic_code": "XTKS",
                            "instrument_type": "Common Stock",
                            "country": "Japan",
                            "currency": "JPY"
                          }],
                          "status": "ok"
                        }
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo(containsString("/quote")))
                .andExpect(requestTo(containsString("mic_code=XTKS")))
                .andRespond(withSuccess("""
                        {
                          "code": 403,
                          "status": "error",
                          "message": "This symbol requires a higher plan"
                        }
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.findBySymbol("7203"))
                .isInstanceOf(AssetMetadataLookupException.class)
                .hasMessageContaining("could not quote an accessible listing")
                .hasMessageContaining("higher plan")
                .hasMessageContaining("Twelve Data market-data plan");
        server.verify();
    }

    /**
     * 中文：创建使用模拟 HTTP 构建器和测试 API key 的客户端。
     * English: Creates a client using the mocked HTTP builder and test API key.
     */
    private static TwelveDataAssetMetadataClient client(RestClient.Builder builder) {
        return new TwelveDataAssetMetadataClient(
                new TwelveDataProperties("https://api.twelvedata.test", "test-key"),
                builder
        );
    }
}

# US-06 & US-07 实现文档

> 作者：Serena| 分支：`serena` | 日期：2026-07-28

---

## 目录

1. [概述](#概述)
2. [API 接口](#api-接口)
3. [代码结构](#代码结构)
4. [业务逻辑](#业务逻辑)
5. [外部 API 集成](#外部-api-集成)
6. [异常处理](#异常处理)
7. [配置说明](#配置说明)
8. [测试](#测试)
9. [启动方式](#启动方式)

---

## 概述

实现了投资组合管理的两个核心功能：

| 用户故事 | 接口 | 说明 |
|----------|------|------|
| US-06 | `GET /api/portfolio/value` | 查看每项持仓的当前价格和市值 |
| US-07 | `GET /api/portfolio/performance` | 分析总成本、盈亏、收益率和资产占比 |

**依赖**：US-05（购买历史）提供成本基础数据。

**外部数据源**：
- [TwelveData](https://twelvedata.com) — 实时股价
- [Open Exchange Rates](https://openexchangerates.org) — 汇率换算（统一为 USD）

---

## API 接口

### US-06：查看当前价值

```http
GET /api/portfolio/value
```

**成功响应** `200 OK`：

```json
{
  "currency": "USD",
  "priceUpdatedAt": "2026-07-28T01:56:00.969Z",
  "status": "COMPLETE",
  "assets": [
    {
      "symbol": "AAPL",
      "assetType": "STOCK",
      "quantity": 10.00,
      "currentPrice": 336.91,
      "marketValue": 3369.10,
      "currency": "USD"
    },
    {
      "symbol": "TSLA",
      "assetType": "STOCK",
      "quantity": 5.00,
      "currentPrice": 309.22,
      "marketValue": 1546.10,
      "currency": "USD"
    }
  ],
  "missingPrices": []
}
```

**状态说明**：

| status | 含义 |
|--------|------|
| `COMPLETE` | 所有持仓均有实时价格 |
| `PARTIAL` | 部分持仓缺少行情 |
| `UNAVAILABLE` | 全部行情不可用 |

---

### US-07：投资组合表现分析

```http
GET /api/portfolio/performance
```

**成功响应** `200 OK`：

```json
{
  "currency": "USD",
  "totalCost": 3030.00,
  "currentValue": 4915.20,
  "unrealizedProfitLoss": 1885.20,
  "returnPercentage": 62.22,
  "status": "COMPLETE",
  "priceUpdatedAt": "2026-07-28T01:56:00.969Z",
  "assets": [
    {
      "symbol": "AAPL",
      "quantity": 10.00,
      "averageCost": 180.50,
      "currentPrice": 336.91,
      "costBasis": 1805.00,
      "currentValue": 3369.10,
      "unrealizedProfitLoss": 1564.10,
      "returnPercentage": 86.65,
      "allocationPercentage": 68.54
    },
    {
      "symbol": "TSLA",
      "quantity": 5.00,
      "averageCost": 245.00,
      "currentPrice": 309.22,
      "costBasis": 1225.00,
      "currentValue": 1546.10,
      "unrealizedProfitLoss": 321.10,
      "returnPercentage": 26.21,
      "allocationPercentage": 31.46
    }
  ],
  "missingPrices": []
}
```

**状态说明**：

| status | 含义 |
|--------|------|
| `COMPLETE` | 所有持仓数据完整 |
| `PARTIAL` | 部分持仓缺少行情，数据不完整 |

---

## 代码结构

```
backend/src/main/java/com/hsbc/portfoliomanager/
├── transaction/                              # 交易记录
│   ├── Transaction.java                      # JPA 实体（transactions 表）
│   ├── TransactionRepository.java            # 数据访问层
│   ├── TransactionController.java            # POST/GET /api/transactions
│   └── TransactionType.java                  # BUY / SELL 枚举
│
├── marketdata/                               # 外部行情服务
│   ├── MarketDataService.java                # 接口（含 PriceData record）
│   ├── MarketDataServiceImpl.java            # 实现：委托给具体 Client
│   ├── TwelveDataPriceService.java           # TwelveData API 客户端
│   ├── ExchangeRateService.java              # OpenExchangeRates 客户端
│   └── MarketDataConfig.java                 # RestTemplate Bean + API Key
│
├── portfolio/                                # 核心业务（扩展已有包）
│   ├── AnalyticsController.java              # GET /value + /performance
│   ├── AnalyticsService.java                 # 计算逻辑
│   ├── PortfolioValueResponse.java           # US-06 响应 DTO
│   ├── PortfolioPerformanceResponse.java     # US-07 响应 DTO
│   └── MarketDataUnavailableException.java   # 行情不可用异常
│
└── common/
    └── GlobalExceptionHandler.java           # 统一异常处理（已扩展）
```

---

## 业务逻辑

### 加权平均成本计算

```
总花费 = Σ(每次购买数量 × 每次买入单价)
总数量 = Σ(每次购买数量)
加权平均成本 = 总花费 ÷ 总数量
```

示例：AAPL 先后以 $180 买 10 股、$190 买 5 股

```
加权平均成本 = (10×180 + 5×190) ÷ 15 = $183.33
```

### 单项计算

```
成本基础 = 持有数量 × 加权平均成本
当前价值 = 持有数量 × 当前价格
未实现盈亏 = 当前价值 − 成本基础
收益率   = 未实现盈亏 ÷ 成本基础 × 100%
资产占比 = 单项当前价值 ÷ 组合总价值 × 100%
```

### 组合汇总

```
总成本    = Σ(各项成本基础)
总市值    = Σ(各项当前价值)
总盈亏    = 总市值 − 总成本
总收益率  = 总盈亏 ÷ 总成本 × 100%
```

### 精度与舍入

- 全部使用 `BigDecimal`，避免浮点数误差
- 舍入模式：`RoundingMode.HALF_UP`
- 单价/成本保留 4 位小数，收益率保留 4 位小数

### 边界处理

| 场景 | 处理方式 |
|------|---------|
| 空持仓 | 返回 0 值，不产生除零错误 |
| 无交易记录 | averageCost = null，returnPercentage = 0 |
| 成本为 0 | 不计算收益率，返回 0 |
| 行情缺失 | status = PARTIAL，missingPrices 列出缺失符号 |
| 全部行情不可用 | status = UNAVAILABLE |

---

## 外部 API 集成

### TwelveData

```
GET https://api.twelvedata.com/quote?symbol={SYMBOL}&apikey={KEY}
```

**返回示例**：

```json
{
  "symbol": "AAPL",
  "name": "Apple Inc.",
  "exchange": "NASDAQ",
  "currency": "USD",
  "close": "336.91",
  "datetime": "2026-07-27"
}
```

**缓存策略**：60 秒内存缓存，避免重复调用消耗 API 额度。

### Open Exchange Rates

```
GET https://openexchangerates.org/api/latest.json?app_id={APP_ID}
```

免费版以 USD 为基准货币。非 USD 价格除以对应汇率即可转为 USD：

```
GBP → USD：amount ÷ GBP_rate
```

**使用示例**：£100，GBP rate = 0.79 → 100 ÷ 0.79 = $126.58

**缓存策略**：5 分钟缓存，汇率为所有货币共用。

---

## 异常处理

| 异常 | HTTP 状态码 | 说明 |
|------|------------|------|
| `MarketDataUnavailableException` | 503 | 外部行情服务不可用 |
| 十二要素 API 401/403 | 静默降级，不抛异常 | 返回 UNAVAILABLE |
| 网络超时 | 静默降级，不抛异常 | 返回 UNAVAILABLE |

行情不可用时**不会导致持仓查询失败**——持仓数据照常返回，仅标记 status 并列出 missingPrices。

---

## 配置说明

### application.yml

```yaml
marketdata:
  twelvedata:
    api-key: ${TWELVE_DATA_API_KEY:默认key}
  openexchangerates:
    api-key: ${OPENEXCHANGERATES_API_KEY:默认key}
```

支持通过环境变量覆盖（生产环境使用）：

```bash
export TWELVE_DATA_API_KEY=你的key
export OPENEXCHANGERATES_API_KEY=你的key
```

### 数据库迁移文件

| 版本 | 文件 | 说明 |
|------|------|------|
| V1 | `V1__create_portfolio_items.sql` | 持仓表 |
| V2 | `V2__create_transactions.sql` | 交易记录表 |

---

## 测试

### 单元测试：`AnalyticsServiceTest`（14 个用例）

| 场景 | 验证点 |
|------|--------|
| 全部行情可用 | COMPLETE，市值计算正确 |
| 部分行情缺失 | PARTIAL，missingPrices 列表 |
| 全部行情不可用 | UNAVAILABLE |
| 空持仓 | 零值，不除零 |
| 加权平均成本 | 多次不同价格买入 |
| 未实现亏损 | 现价低于成本 |
| 无交易记录 | 成本为 0，不报错 |
| 资产占比 | 多资产分配比例总和 = 100% |
| 非 USD 转换 | 汇率换算 |
| 更新时间戳 | 取最新行情时间 |

### 集成测试：`AnalyticsControllerIntegrationTest`（6 个用例）

使用 H2 内存数据库 + Mockito 模拟行情服务，验证完整的 HTTP 请求-响应链路。

### 运行测试

```bash
# Docker 方式（推荐）
docker run --rm \
  -v "$(pwd)/backend:/workspace" \
  -v "$HOME/.m2:/root/.m2" \
  -w /workspace \
  maven:3.9-eclipse-temurin-21 \
  mvn test

# 本地方式（需 Java 21 + Maven）
cd backend && mvn test
```

**测试结果**：23 个测试全部通过，0 失败 0 跳过。

---

## 启动方式

```bash
# 1. 复制并填写 .env
cp .env.example .env
# 编辑 .env，填入密码和 API Key

# 2. 启动全部服务
docker compose up -d --build

# 3. 确认运行
docker compose ps

# 4. 测试 API
curl -X POST http://localhost:8080/api/portfolio/items \
  -H "Content-Type: application/json" \
  -d '{"assetType":"STOCK","symbol":"AAPL","quantity":10}'

curl -X POST http://localhost:8080/api/transactions \
  -H "Content-Type: application/json" \
  -d '{"transactionType":"BUY","assetType":"STOCK","symbol":"AAPL","quantity":10,"pricePerUnit":180.50,"currency":"USD","purchasedAt":"2026-07-25T10:30:00Z"}'

curl http://localhost:8080/api/portfolio/value
curl http://localhost:8080/api/portfolio/performance
```

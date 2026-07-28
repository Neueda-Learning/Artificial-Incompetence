# Portfolio Manager User Stories

## Epic

**作为一名投资者，**  
我希望能够记录、查看和管理自己的投资组合，并了解投资组合的基本表现，  
以便掌握自己持有哪些资产以及这些资产当前的价值。

## US-01：查看投资组合

**作为一名投资者，**  
我希望查看投资组合中的所有资产，  
以便快速了解自己当前的持仓。


### 验收标准

```gherkin
Given 数据库中存在投资组合资产
When 用户请求查看投资组合
Then 系统返回每项资产的id、assetType、symbol和quantity
And HTTP状态码为200
```

```gherkin
Given 数据库中没有投资组合资产
When 用户请求查看投资组合
Then 系统返回空数组
And HTTP状态码为200
```

对应接口：

```http
GET /api/portfolio/items
```

## US-02：添加资产

**作为一名投资者，**  
我希望输入资产类型、股票代码和持有数量来添加一项资产，  
以便把自己的持仓记录到投资组合中。

### 验收标准

```gherkin
Given 用户提供有效的assetType、symbol和quantity
When 用户提交新增资产请求
Then 系统将资产保存到数据库
And symbol被去除首尾空格并转换为大写
And 系统返回带有id的新资产
And HTTP状态码为201
```

```gherkin
Given symbol为空或者quantity小于等于0
When 用户提交新增资产请求
Then 系统不保存数据
And 返回字段级错误信息
And HTTP状态码为400
```

对应接口：

```http
POST /api/portfolio/items
```

示例请求：

```json
{
  "assetType": "STOCK",
  "symbol": "AAPL",
  "quantity": 10
}
```

## US-03：删除资产

**作为一名投资者，**  
我希望从投资组合中删除一项不再持有的资产，  
以便让投资组合记录保持准确。

### 验收标准

```gherkin
Given 指定id的资产存在
When 用户删除该资产
Then 系统从数据库中删除记录
And HTTP状态码为204
```

```gherkin
Given 指定id的资产不存在
When 用户尝试删除该资产
Then 系统返回明确的未找到错误
And HTTP状态码为404
```

对应接口：

```http
DELETE /api/portfolio/items/{id}
```

## US-04：持久化投资组合

**作为一名投资者，**  
我希望自己的投资组合数据在应用重启后仍然存在，  
以便以后继续查看和管理持仓。

### 验收标准

```gherkin
Given 用户已经添加一项资产
When Spring Boot和MySQL容器重新启动
Then 用户仍然可以查询到该资产
```

技术要求：

- 使用MySQL保存投资组合数据。
- 使用Docker命名卷保存MySQL数据。
- 普通的`docker compose down`不应删除数据库数据。

## US-05：查看购买历史（第二阶段）

**作为一名投资者，**  
我希望按时间查看自己的股票购买记录，  
以便了解自己在什么时候、以什么价格、购买了多少资产。

### 验收标准

```gherkin
Given 用户曾经记录过一笔或多笔购买交易
When 用户查看购买历史
Then 系统按照购买时间从新到旧返回记录
And 每条记录包含id、assetType、symbol、quantity、pricePerUnit、currency和purchasedAt
And HTTP状态码为200
```

```gherkin
Given 用户还没有购买记录
When 用户查看购买历史
Then 系统返回空数组
And HTTP状态码为200
```

```gherkin
Given 用户已经删除某项当前持仓
When 用户查看购买历史
Then 该资产以前的购买记录仍然存在
```

```gherkin
Given 用户提供的quantity或pricePerUnit小于等于0
When 用户提交购买记录
Then 系统不保存该记录
And 返回字段级错误信息
And HTTP状态码为400
```

建议接口：

```http
POST /api/transactions
GET  /api/transactions?type=BUY
```

购买记录示例：

```json
{
  "transactionType": "BUY",
  "assetType": "STOCK",
  "symbol": "AAPL",
  "quantity": 10,
  "pricePerUnit": 180.50,
  "currency": "USD",
  "purchasedAt": "2026-07-27T10:30:00Z"
}
```

技术说明：

- 购买历史应使用独立的`transactions`表保存，不能只从当前持仓推算。
- 当前持仓表示“现在持有多少”，购买历史表示“过去发生过什么”。
- 删除当前持仓时，不应连带删除历史交易。
- 后续增加卖出功能时，可以在同一张表中使用`SELL`交易类型。

建议的最小交易数据模型：

```text
Transaction
- id
- transactionType
- assetType
- symbol
- quantity
- pricePerUnit
- currency
- purchasedAt
```

## US-06：查看当前价值（第二阶段）

**作为一名投资者，**  
我希望看到每项股票的当前价格和持仓市值，  
以便了解投资组合当前值多少钱。

### 验收标准

```gherkin
Given 投资组合中存在有效股票代码和持有数量
And 行情服务能够返回当前价格
When 用户查看投资组合
Then 系统显示当前价格
And 系统显示quantity乘以currentPrice得到的市场价值
And 响应中标明价格货币和更新时间
```

```gherkin
Given 外部行情服务暂时不可用
When 用户查看投资组合
Then 已保存的持仓数据仍然可以显示
And 系统明确标记行情暂时不可用
```

## US-07：分析投资组合表现（第二阶段）

**作为一名投资者，**  
我希望看到投资组合及每项资产的成本、当前价值、盈亏和收益率，  
以便评估自己的投资表现并了解哪些资产对整体结果影响最大。

### 验收标准

```gherkin
Given 用户存在购买记录
And 行情服务能够返回当前价格
When 用户查看Performance分析
Then 系统返回总投入成本
And 系统返回投资组合当前总价值
And 系统返回未实现盈亏金额
And 系统返回未实现收益率
And 响应中标明货币和行情更新时间
And HTTP状态码为200
```

```gherkin
Given 用户以不同价格多次购买同一股票
When 系统计算该股票的成本
Then 系统使用加权平均买入成本
And 正确计算该股票的未实现盈亏和收益率
```

```gherkin
Given 投资组合中包含多项资产
When 用户查看Performance分析
Then 系统返回每项资产的成本、当前价值、盈亏和收益率
And 系统返回每项资产占投资组合当前价值的比例
```

```gherkin
Given 用户当前没有任何持仓
When 用户查看Performance分析
Then 系统返回总成本和当前价值为0
And 不产生除以0错误
And HTTP状态码为200
```

```gherkin
Given 部分资产缺少当前行情
When 用户查看Performance分析
Then 系统将结果标记为PARTIAL
And 明确列出缺少行情的股票代码
And 不把缺失价格错误地当作0
```

建议接口：

```http
GET /api/portfolio/performance
```

响应示例：

```json
{
  "currency": "USD",
  "totalCost": 1805.00,
  "currentValue": 1950.00,
  "unrealizedProfitLoss": 145.00,
  "returnPercentage": 8.03,
  "status": "COMPLETE",
  "priceUpdatedAt": "2026-07-27T10:35:00Z",
  "assets": [
    {
      "symbol": "AAPL",
      "quantity": 10,
      "averageCost": 180.50,
      "currentPrice": 195.00,
      "currentValue": 1950.00,
      "unrealizedProfitLoss": 145.00,
      "returnPercentage": 8.03,
      "allocationPercentage": 100.00
    }
  ],
  "missingPrices": []
}
```

基础计算规则：

```text
单项成本 = 持有数量 × 加权平均买入价格
单项当前价值 = 持有数量 × 当前价格
未实现盈亏 = 当前价值 - 当前持仓成本
收益率 = 未实现盈亏 ÷ 当前持仓成本 × 100%
资产占比 = 单项当前价值 ÷ 投资组合当前总价值 × 100%
```

技术说明：

- 该功能依赖US-05中的购买历史和US-06中的当前行情。
- 金额和比例计算应使用`BigDecimal`，不要使用`double`。
- 多币种资产不能直接相加；第一版可以只支持一种基础货币。
- Performance响应应包含行情更新时间，避免用户误以为数据是实时的。
- 历史收益曲线需要历史行情或每日组合快照，可以作为后续增强功能。
- 页面可以使用折线图展示组合价值变化，并使用饼图展示资产占比。

## MVP完成定义

以下条件全部满足时，第一版MVP可以视为完成：

- `GET /api/portfolio/items`能够查询全部资产。
- `POST /api/portfolio/items`能够校验并保存资产。
- `DELETE /api/portfolio/items/{id}`能够删除资产。
- 不合法请求返回统一错误格式。
- MySQL容器重启后数据不会丢失。
- Maven测试通过。
- Docker镜像能够构建。
- GitHub CI检查通过。
- README包含启动方法和API调用示例。

US-05、US-06和US-07属于第二阶段，不阻塞第一版MVP交付。

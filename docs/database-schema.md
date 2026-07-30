# Portfolio Manager Database Schema

## 1. 统一方式

数据库结构由Flyway迁移文件统一管理：

```text
backend/src/main/resources/db/migration/
├── V1__create_portfolio_items.sql
├── V2__create_transactions.sql
├── V3__add_asset_metadata_to_portfolio_items.sql
├── V4__create_market_history_tables.sql
├── V5__create_portfolio_activities.sql
└── V6__remove_redundant_transactions_table.sql
```

三名后端开发者都使用自己的MySQL容器和数据，但启动同一版本代码时，Flyway会按版本顺序执行相同的迁移。

不要直接在个人数据库中手动增加或修改字段。结构变更应新增迁移文件，例如：

```text
V3__add_transaction_notes.sql
```

已经在其他环境执行过的迁移文件不能修改或重命名。

## 2. 表关系

```text
portfolio_items
    当前持仓快照
    一种资产只保留一行

portfolio_activities
    新增、购买和移除的统一历史账本
    每次业务操作保留一行
```

两张表不使用级联删除。删除当前持仓时，`portfolio_activities`历史流水仍然保留。

`symbol`、`asset_type`和`currency`共同表示资产身份。第一版统一使用USD，但数据库保留`currency`字段。

## 3. portfolio_items

| 字段 | 类型 | 规则 | 用途 |
|---|---|---|---|
| `id` | `BIGINT` | 主键、自增 | 持仓ID |
| `asset_type` | `VARCHAR(20)` | STOCK/BOND/CASH | 资产类型 |
| `symbol` | `VARCHAR(20)` | 非空 | 股票或资产代码 |
| `company_name` | `VARCHAR(255)` | 可空 | 公司名称，由行情API首次获取 |
| `exchange` | `VARCHAR(50)` | 可空 | 交易所，由行情API首次获取 |
| `quantity` | `DECIMAL(19,6)` | 大于0 | 当前持有数量 |
| `currency` | `VARCHAR(3)` | 默认USD | ISO货币代码 |
| `created_at` | `TIMESTAMP(6)` | 自动生成 | 创建时间 |
| `updated_at` | `TIMESTAMP(6)` | 自动更新 | 最后修改时间 |

唯一约束：

```text
asset_type + symbol + currency
```

重复购买同一资产时，应更新现有持仓数量，而不是插入第二行。

## 4. portfolio_activities

| 字段 | 类型 | 规则 | 用途 |
|---|---|---|---|
| `id` | `BIGINT` | 主键、自增 | 活动ID |
| `action_type` | `VARCHAR(10)` | ADDED/REMOVED | 活动类型 |
| `asset_type` | `VARCHAR(20)` | STOCK/BOND/CASH | 资产类型 |
| `symbol` | `VARCHAR(20)` | 非空 | 活动发生时的资产代码 |
| `quantity` | `DECIMAL(19,6)` | 大于0 | 新增或移除数量 |
| `price_per_unit` | `DECIMAL(19,4)` | ADDED时有值 | 购买时每单位价格 |
| `currency` | `VARCHAR(3)` | 可空 | 活动货币 |
| `remaining_quantity` | `DECIMAL(19,6)` | REMOVED时有值 | 移除后的剩余持仓 |
| `occurred_at` | `TIMESTAMP(6)` | 非空 | 活动发生时间 |
| `created_at` | `TIMESTAMP(6)` | 自动生成 | 记录创建时间 |

索引支持：

- 按活动时间查询Recent Activity和History。
- 按股票代码和时间重建成本及历史Performance。

`transactions`是V2创建的旧表。V5先将旧记录复制到
`portfolio_activities`，V6确认迁移顺序后再删除旧表。

## 5. 写入规则

记录一笔购买时，应在同一个Spring事务中：

1. 插入一条`ADDED`活动流水。
2. 新增或更新对应的`portfolio_items`。

```text
第一次购买AAPL 10股
    portfolio_items：AAPL 10
    portfolio_activities：ADDED AAPL 10

再次购买AAPL 5股
    portfolio_items：AAPL 15
    portfolio_activities：再增加一条ADDED AAPL 5
```

部分减仓或清仓会增加`REMOVED`流水。清仓只删除`portfolio_items`，
不能删除`portfolio_activities`。

## 6. Performance数据来源

```text
当前数量        portfolio_items.quantity
历史成本        portfolio_activities中的ADDED/REMOVED流水
当前股票价格    外部行情API
当前汇率        Open Exchange Rates API
```

当前股票价格和汇率会频繁变化，不保存在这两张核心业务表中。

## 7. 本地已有旧表的处理

如果MySQL数据卷之前已经由Hibernate创建过表，Flyway不会接管不受版本管理的旧结构。项目仍在初期且旧数据不重要时，各开发者应先备份需要的数据，再删除本地开发数据卷并从迁移重新初始化。

删除Volume会永久删除该成员本地数据库数据，因此只能由每位成员确认后自行执行。

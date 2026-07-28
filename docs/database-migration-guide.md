# 数据库表结构修改操作指南

本文档规定团队成员修改MySQL表结构时的统一操作流程。

## 1. 基本原则

- 数据库表结构由Flyway迁移文件统一管理。
- 不要只在个人MySQL中手动修改表结构。
- 每次结构变更都必须创建一个新的迁移文件。
- 已经提交或被其他成员执行过的迁移文件不能修改、删除或重命名。
- 迁移文件、对应的Java Entity、DTO、Repository和测试应尽量放在同一个分支或Pull Request中。
- 密码等敏感信息只放在本地`.env`中，不能写进迁移文件或提交到Git。

迁移文件目录：

```text
backend/src/main/resources/db/migration/
```

当前迁移：

```text
V1__create_portfolio_items.sql
V2__create_transactions.sql
```

## 2. 修改前的操作

### 第一步：说明修改需求

成员先在群聊、Issue或Pull Request中说明：

- 需要修改哪张表。
- 增加、修改或删除什么字段。
- 修改原因。
- 是否影响已有数据。
- 是否需要同步修改接口和Java代码。

### 第二步：更新本地代码

切换到自己的开发分支，然后拉取公共分支的最新代码：

```bash
git switch <你的分支名>
git fetch origin
git merge origin/main
```

如果团队使用的公共分支不是`main`，将命令中的`main`替换成实际分支名。

### 第三步：确认下一个版本号

查看迁移目录：

```bash
ls backend/src/main/resources/db/migration
```

如果当前最后一个版本是`V2`，下一次迁移使用`V3`。

同一个版本号不能同时用于两个文件。创建文件前应在团队内确认版本号，防止两名成员同时创建不同的`V3`。

## 3. 创建迁移文件

文件名格式：

```text
V<版本号>__<英文描述>.sql
```

版本号和描述之间使用两个下划线。

例如，给`transactions`增加备注字段：

```text
V3__add_notes_to_transactions.sql
```

文件内容：

```sql
ALTER TABLE transactions
    ADD COLUMN notes VARCHAR(255) NULL;
```

其他示例：

```sql
-- 创建索引
CREATE INDEX idx_portfolio_items_symbol
    ON portfolio_items (symbol);

-- 增加非空字段时为已有记录提供默认值
ALTER TABLE transactions
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'COMPLETED';

-- 修改字段长度
ALTER TABLE portfolio_items
    MODIFY COLUMN symbol VARCHAR(30) NOT NULL;
```

## 4. 同步修改Spring Boot代码

数据库字段发生变化后，检查是否需要同步修改：

- JPA Entity及其`@Column`配置。
- Request和Response DTO。
- Repository查询。
- Service业务逻辑和`@Transactional`事务范围。
- Controller接口。
- 单元测试和集成测试。

例如数据库增加了`notes`字段，对应Entity可以增加：

```java
@Column(length = 255)
private String notes;
```

Java字段类型、长度、是否允许为空以及精度应与迁移文件保持一致。项目中的Hibernate配置为`ddl-auto: validate`，启动时会检查Entity和数据库结构是否兼容，但不会替团队自动修改表。

## 5. 本地验证

构建并启动服务：

```bash
docker compose up --build
```

启动时的自动执行顺序：

```text
MySQL启动
    ↓
Spring Boot启动
    ↓
Flyway读取flyway_schema_history
    ↓
执行尚未运行的新迁移
    ↓
Hibernate校验表结构
    ↓
后端开始提供API
```

查看容器状态：

```bash
docker compose ps
```

进入MySQL：

```bash
docker compose exec database \
  mysql -uportfolio_user -p portfolio
```

如果个人`.env`使用了不同的数据库名或用户名，应替换命令中的`portfolio_user`和`portfolio`。

检查表结构和迁移记录：

```sql
SHOW TABLES;
DESCRIBE transactions;
SELECT version, description, success
FROM flyway_schema_history
ORDER BY installed_rank;
```

然后运行后端测试：

```bash
cd backend
mvn test
```

验证要求：

- Spring Boot正常启动。
- 新迁移在`flyway_schema_history`中的`success`为`1`。
- 表结构与Java Entity一致。
- 原有接口仍能正常工作。
- 新功能测试通过。

## 6. 提交和团队同步

确认验证通过后提交：

```bash
git status
git add backend/src/main/resources/db/migration
git add <其他相关代码和测试>
git commit -m "Add notes to transactions"
git push
```

迁移合并到公共分支后，其他成员执行：

```bash
git pull
docker compose up --build
```

Flyway只会执行该成员数据库中尚未执行的迁移，不会重复执行已经成功的版本。

## 7. 冲突和失败处理

### 两个人使用了相同版本号

如果两个分支都创建了`V3`，后合并的成员应在合并前把自己的文件改成当前可用的下一个版本，例如`V4`。

如果原来的迁移只在该成员个人数据库执行过，改名后需要重建个人开发数据库，或者在确认没有共享影响后处理本地迁移记录。不要修改其他成员的数据库。

### 迁移尚未共享

如果错误迁移还没有推送，且只在创建者本地执行过，可以修正文件并重建创建者自己的本地数据库。

清除Volume会永久删除本地数据库。只有确认数据不需要或已经备份后，才可以由该成员自行执行：

```bash
docker compose down -v
docker compose up --build
```

### 迁移已经被其他成员执行

不要修改原迁移。创建新的迁移进行修正：

```text
V4__fix_transaction_notes.sql
```

例如：

```sql
ALTER TABLE transactions
    MODIFY COLUMN notes VARCHAR(500) NULL;
```

### 需要删除字段或表

删除字段、删除表、缩短字段长度以及修改数据类型都可能造成数据丢失。执行前必须：

1. 通知所有成员。
2. 确认代码已经不再使用目标字段或表。
3. 检查并备份需要保留的数据。
4. 在个人开发数据库验证迁移。
5. 通过代码审查后再合并。

## 8. 禁止事项

团队成员不能：

- 直接使用数据库工具修改表后不提交迁移。
- 修改已经共享或执行过的`V1`、`V2`等历史文件。
- 删除`flyway_schema_history`。
- 随意执行Flyway repair来掩盖迁移校验错误。
- 将生产数据或个人密码写入SQL迁移。
- 未经确认执行`docker compose down -v`。
- 在迁移失败后手动伪造一条成功记录。

## 9. 操作流程总结

```text
提出结构修改需求
    ↓
拉取公共分支最新代码
    ↓
团队确认下一个迁移版本号
    ↓
创建新的Flyway迁移
    ↓
同步修改Entity、DTO、Repository和测试
    ↓
本地启动MySQL和Spring Boot
    ↓
检查表结构及flyway_schema_history
    ↓
运行测试
    ↓
提交并合并
    ↓
其他成员拉取代码并自动迁移
```

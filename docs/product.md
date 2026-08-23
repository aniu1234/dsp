# DSP 产品说明

> 文档版本：1.0
>
> 产品阶段：Developer Preview（开发者预览）
>
> 适用版本：`1.0-SNAPSHOT`

## 1. 产品概述

DSP 是一个使用 Java 构建的模块化 SQL 数据服务原型。它通过 MySQL Wire Protocol
对外提供数据库访问入口，使用 Apache Calcite 完成 SQL 解析、校验和执行计划生成，
并通过可插拔存储接口接入 Lucene 本地存储。

产品当前主要用于：

- 学习和验证数据库中间件、查询优化器与执行器设计；
- 快速搭建可通过 MySQL 客户端访问的本地 SQL 实验环境；
- 验证存储引擎 SPI、Schema Connector SPI 和资源限制策略；
- 作为进一步演进分布式 SQL、联邦查询与控制面的技术底座。

DSP 当前不是面向生产核心业务的通用关系数据库，不承诺事务、主从复制、在线扩缩容、
完整 MySQL 语法兼容或生产级可用性 SLA。

## 2. 目标用户

| 用户 | 主要诉求 | DSP 当前价值 |
| --- | --- | --- |
| 数据库/中间件开发者 | 理解 SQL 到物理执行的完整链路 | 提供可运行的解析、优化、Operator 和存储实现 |
| 架构与技术团队 | 验证模块边界与插件化方案 | 提供存储 SPI、Connector SPI 和 Maven 边界约束 |
| 教学与研究人员 | 演示查询计划、JOIN、聚合和存储行为 | 可使用 MySQL 客户端执行 SQL 并查看计划 |
| 原型开发者 | 快速获得轻量 SQL 服务 | 支持本地目录持久化、基础 DDL/DML 和查询 |

不建议将当前版本用于财务记账、订单交易、强一致库存、敏感数据托管或其他生产关键场景。

## 3. 核心产品能力

### 3.1 MySQL 客户端接入

- 默认监听 `3016` 端口，可通过环境变量或 JVM 参数修改；
- 支持 MySQL Native Password 认证；
- 可使用 `mysql` CLI 及兼容 MySQL Wire Protocol 的客户端连接；
- 支持连接时选择数据库和执行 `USE` 切换数据库；
- 未配置用户名和密码时拒绝登录，避免默认匿名访问。

当前认证模型为单一静态账号，不包含用户、角色、授权、审计或 TLS 管理。

### 3.2 Catalog 与 DDL

当前支持：

- `CREATE DATABASE`、`DROP DATABASE`、`USE`；
- `CREATE TABLE`、`DROP TABLE`；
- `SHOW DATABASES`、`SHOW TABLES`、`SHOW CREATE TABLE`；
- 表引擎、分片数、列默认值、空值、无符号和字符长度定义；
- 默认使用本地 JSON Catalog 持久化库表元数据；
- 可选使用外部 MySQL 保存元数据。

Catalog 与表数据在进程重启后恢复。DDL 失败时会尝试回滚内存目录、元数据和表目录变更。

### 3.3 数据写入

当前支持 `INSERT ... VALUES`：

- 单行与多行批量写入；
- 指定列、大小写不敏感列名和反引号列名；
- `DEFAULT`、显式 `NULL`、正负数值字面量；
- 非空、无符号、整数精确转换和字符串字符长度校验；
- 整条 INSERT 在进入存储前完成转换与校验；
- 单条 INSERT 的全部行路由到同一分片，保留存储批次原子边界；
- 返回实际写入行数；
- 可配置同步 flush 和单次 INSERT 最大行数。

当前不支持 `INSERT ... SELECT`、`UPDATE`、`DELETE`、UPSERT、唯一键约束和跨语句事务。
系统会明确拒绝 `BEGIN`、`COMMIT`、`ROLLBACK` 和 `START TRANSACTION`，不会模拟成功。

### 3.4 SQL 查询

已经由端到端测试覆盖的主要能力包括：

- 投影、过滤、表达式与排序；
- `INNER JOIN`、`LEFT JOIN`、`RIGHT JOIN`；
- 等值多列 JOIN 和有限的非等值 JOIN；
- `GROUP BY`；
- `COUNT`、`SUM`、`MIN`、`MAX` 和部分 `DISTINCT` 聚合；
- `UNION` 与去重；
- 日期、时间戳、布尔、字符串和基础数值类型；
- `EXPLAIN` 查看优化后的执行计划；
- 流式返回 MySQL Result Set。

SQL 兼容性以项目测试覆盖为准，不应假设支持完整 MySQL 方言、窗口函数、子查询、CTE、
存储过程、触发器或所有 Calcite 能解析的语法。

### 3.5 查询与写入资源保护

系统提供以下进程级保护参数：

| 配置 | 默认值 | 作用 |
| --- | ---: | --- |
| `DSP_QUERY_TIMEOUT_MS` | `30000` | 单次查询超时；`0` 表示关闭超时 |
| `DSP_QUERY_MAX_RESULT_ROWS` | `100000` | 单次查询最多返回行数 |
| `DSP_QUERY_MAX_MATERIALIZED_ROWS` | `100000` | 排序、JOIN 构建侧、聚合和去重最大内存条目数 |
| `DSP_WRITE_MAX_ROWS_PER_INSERT` | `10000` | 单条 INSERT 最大行数 |
| `DSP_STORAGE_SCAN_PAGE_SIZE` | `512` | Lucene 单次扫描页大小 |
| `DSP_STORAGE_SYNC_WRITES` | `true` | INSERT 成功响应前是否 flush |

这些参数用于限制单进程原型的资源风险，不等同于租户隔离、配额系统或完整工作负载管理。

### 3.6 存储与扩展

- `dsp-storage-api` 定义存储引擎接口、工厂和写入结果契约；
- Lucene 是当前主运行链的默认存储插件；
- 存储实现通过 Java `ServiceLoader` 注册；
- 表支持配置分片数，写入按语句进行轮询分片路由；
- 后台 flush 服务只负责持久化调度，Catalog 负责表存储生命周期。

`dsp-schema` 提供独立的外部数据 Connector SPI。文件和 MySQL Connector 已有实现，
Hive Connector 目前可发现但状态为不可用。Connector 尚未接入主 MySQL 协议运行链，
因此属于技术预览能力，不应作为当前产品入口宣传。

## 4. 典型使用流程

### 4.1 启动服务

```bash
export DSP_AUTH_USERNAME='root'
export DSP_AUTH_PASSWORD='change-me'
export DSP_DATA_DIR="$HOME/.dsp/data"
export DSP_SERVER_PORT='3016'

mvn -pl dsp-protocol -am package
```

在 IDE 中运行 `com.qinyadan.system.dsp.FrontEndMain`，或使用发行包中的启动脚本。

### 4.2 连接与建表

```bash
mysql -h127.0.0.1 -P3016 -uroot -p --ssl-mode=disabled
```

```sql
CREATE DATABASE demo;
USE demo;

CREATE TABLE users (
  id INTEGER UNSIGNED NOT NULL,
  name VARCHAR(32) NOT NULL DEFAULT 'guest'
) ENGINE = lucene SHARD = 2;
```

### 4.3 写入与查询

```sql
INSERT INTO users(id, name) VALUES
  (1, 'Alice'),
  (2, DEFAULT);

SELECT id, name
FROM users
WHERE id > 0
ORDER BY id;

EXPLAIN SELECT id, name FROM users WHERE id = 1;
```

### 4.4 运维检查

```sql
SHOW DATABASES;
SHOW TABLES;
SHOW CREATE TABLE users;
```

当前版本没有独立管理控制台、指标接口和在线备份命令。运行状态主要通过进程日志、
客户端错误码、数据目录和 Maven 测试结果检查。

## 5. 部署与配置

### 5.1 必需配置

| 环境变量 | 说明 |
| --- | --- |
| `DSP_AUTH_USERNAME` | MySQL 登录用户名 |
| `DSP_AUTH_PASSWORD` | MySQL 登录密码 |
| `DSP_DATA_DIR` | Catalog 和表数据目录；生产式调试应显式配置 |

### 5.2 可选元数据库

| 环境变量 | 说明 |
| --- | --- |
| `DSP_META_JDBC_URL` | 元数据 MySQL JDBC URL；未配置时使用本地 JSON Catalog |
| `DSP_META_JDBC_USERNAME` | 元数据库用户名 |
| `DSP_META_JDBC_PASSWORD` | 元数据库密码 |

外部元数据库只保存 Catalog 元数据，表数据仍由存储插件管理。

### 5.3 数据安全建议

- 使用独立数据目录和最小权限运行账号；
- 不要在源码、脚本或版本库中提交真实密码；
- 不要直接编辑运行中的 Catalog JSON 或 Lucene 目录；
- 升级、迁移和删除数据前先离线备份整个 `DSP_DATA_DIR`；
- 当前协议链路未提供产品化 TLS 配置，不应暴露到不可信网络。

## 6. 产品边界

| 能力 | 当前状态 | 说明 |
| --- | --- | --- |
| 单节点 SQL 服务 | 可用（预览） | 主运行形态 |
| MySQL Wire Protocol | 部分兼容 | 以已测试命令为准 |
| 本地持久化 | 可用（预览） | JSON Catalog + Lucene |
| 多分片表 | 基础支持 | 语句级轮询写入，查询合并扫描 |
| ACID 事务 | 不支持 | 事务命令明确报错 |
| UPDATE / DELETE | 不支持 | 当前只有 INSERT VALUES 写入路径 |
| 高可用与复制 | 未接入 | Raft/Register 尚属独立子系统 |
| 联邦查询 | 独立实验能力 | Connector 未接入主产品入口 |
| 多租户与权限系统 | 不支持 | 当前为单静态账号 |
| 在线运维平台 | 不支持 | 无管理 UI、指标与备份 API |

## 7. 版本验收基线

每次主链变更至少应满足：

1. `mvn test` 全量构建通过；
2. Maven Enforcer 的 core、engine、connector 边界规则通过；
3. MySQL 协议端到端测试覆盖建库、建表、写入、查询、聚合、JOIN 与错误路径；
4. `git diff --check` 无空白错误；
5. 新增产品能力同步更新本文件和运行手册；
6. 规划中能力不得写成已支持能力。

## 8. 产品路线图

### P1：稳定服务边界

- Query/Catalog 服务 DTO 化，减少 Calcite 类型向协议层泄露；
- 完善 `WriteService` 错误模型、幂等键和 statement/batch 原子性契约；
- 增加统一配置对象和启动时配置校验；
- 增加服务健康检查与基础运行指标。

### P2：完善单节点数据库能力

- 增加 UPDATE、DELETE 和更多数据类型；
- 明确并实现事务模型；
- 增加索引、约束和统计信息管理；
- 增加备份、恢复和数据迁移工具。

### P3：接入扩展与控制面

- 将 Connector 以只读 Catalog 数据源接入主运行链；
- 通过中立服务端口接入节点注册、拓扑和 Raft 状态；
- 基于实际压测决定是否拆分协议、查询和存储进程；
- 在具备故障恢复、可观测性和一致性验证后再讨论生产可用等级。

## 9. 相关文档

- [项目概览](../README.md)
- [架构边界](architecture.md)
- [协议运行手册](../dsp-protocol/README.md)

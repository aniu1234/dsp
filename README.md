# DSP

DSP 是一个 Java 模块化 SQL 数据服务原型，通过 MySQL Wire Protocol 提供访问入口，
使用 Apache Calcite 完成 SQL 解析与规划，并通过存储 SPI 使用 Lucene 持久化数据。

当前版本定位为 Developer Preview，适合数据库中间件学习、架构验证和本地实验，
不应直接用于生产核心业务。产品能力、限制和路线图见
[产品说明](docs/product.md)，代码边界见[架构说明](docs/architecture.md)。

## 模块结构

```
dsp (parent)
├── dsp-core              # 数据模型层：DataType、Value、Row、Block、Column
├── dsp-storage-api       # 存储抽象层：StorageEngine、Query、EngineRegistry
├── dsp-storage-lucene    # 基于公共存储 API 的 Lucene 实现
├── dsp-engine            # Calcite、SQL 执行、表达式与元数据管理
├── dsp-schema/           # Schema 适配层
│   ├── common-schema
│   ├── file-schema
│   ├── hive-schema
│   ├── mysql-schema
│   └── test-schema
├── dsp-raft              # Raft / gRPC 集群通信
├── dsp-register          # 注册中心
└── dsp-protocol          # 协议层 / 客户端入口
```

## 依赖关系

箭头表示左侧依赖右侧：

```
dsp-protocol ─────> dsp-engine ─────> dsp-core
      │                  └──────────> dsp-storage-api ──> dsp-core
      └─(runtime)─> dsp-storage-lucene ──> dsp-storage-api
```

- **dsp-core**：框架无关的数据模型，不依赖 Calcite
- **dsp-storage-api**：依赖 dsp-core，定义存储引擎接口
- **dsp-storage-lucene**：实现公共存储接口，由 ServiceLoader 注册引擎工厂
- **dsp-engine**：负责 SQL 解析、规划、执行与应用服务，并在边界适配 Calcite 类型
- **dsp-protocol**：应用装配层，运行时加载 Lucene 插件并提供 MySQL 协议入口

更完整的模块职责、依赖规则和后续拆分方向见 [架构边界说明](docs/architecture.md)。

## 构建

```bash
mvn test
```

## 快速体验

```bash
export DSP_AUTH_USERNAME='root'
export DSP_AUTH_PASSWORD='change-me'
export DSP_DATA_DIR="$HOME/.dsp/data"
```

在 IDE 中运行 `com.qinyadan.system.dsp.FrontEndMain`，然后连接：

```bash
mysql -h127.0.0.1 -P3016 -uroot -p --ssl-mode=disabled
```

详细启动、元数据和资源限制配置见 [dsp-protocol/README.md](dsp-protocol/README.md)。

## 重构进度

- [x] Phase 1: 创建 dsp-core（数据模型层）
- [x] Phase 2: 创建并接入 dsp-storage-api
- [x] Phase 3: 迁移 Lucene 到独立 module
- [x] Phase 4: dsp-engine 主链路切换到公共存储 API
- [x] Phase 5: 更新 protocol 依赖并清理旧存储实现
- [x] Phase 6: 收敛 dsp-core 与 dsp-engine 中重复的 DataType/Value
- [x] Phase 7: 移除 dsp-core 的 Calcite 依赖并增加构建边界
- [x] Phase 8: 增加 Query/Catalog/Environment/Write 服务门面
- [ ] Phase 9: 服务 DTO 化、事务契约与可观测性

## DSP Specification

```text
该项目设计是为了学习和体验实现中间件的开发过程。
```

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

```
dsp-core ────────────────> dsp-engine ───────────────> dsp-protocol
   └─> dsp-storage-api ───> dsp-engine
          └─> dsp-storage-lucene ──(runtime / SPI)──> dsp-protocol
```

- **dsp-core**：无内部模块依赖，仅依赖 Guava、Lombok、Calcite 等基础库
- **dsp-storage-api**：依赖 dsp-core，定义存储引擎接口
- **dsp-storage-lucene**：实现公共存储接口，由 ServiceLoader 注册引擎工厂
- **dsp-engine**：只依赖存储 API，负责 SQL 执行，并在边界适配核心 Row/Value
- **dsp-protocol**：应用装配层，运行时加载 Lucene 插件并提供 MySQL 协议入口

更完整的模块职责、依赖规则和后续拆分方向见 [架构边界说明](docs/architecture.md)。

## 构建

```bash
mvn test
```

## 重构进度

- [x] Phase 1: 创建 dsp-core（数据模型层）
- [x] Phase 2: 创建并接入 dsp-storage-api
- [x] Phase 3: 迁移 Lucene 到独立 module
- [x] Phase 4: dsp-engine 主链路切换到公共存储 API
- [x] Phase 5: 更新 protocol 依赖并清理旧存储实现
- [x] Phase 6: 收敛 dsp-core 与 dsp-engine 中重复的 DataType/Value

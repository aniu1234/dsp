## DSP Specification

```text
该项目设计是为了学习和体验实现中间件的开发过程。
```

## 模块结构

```
dsp (parent)
├── dsp-core              # 数据模型层：DataType、Value、Row、Block、Column
├── dsp-storage-api       # 存储抽象层：StorageEngine、Query、EngineRegistry
├── dsp-runtime           # 执行引擎（Calcite 集成、表达式/函数体系）
├── dsp-storage/
│   ├── base-storage      # 存储核心实现（Calcite parser、Lucene 引擎、元数据）
│   ├── lucene-storage    # Lucene 存储扩展
│   ├── rocksdb-storage   # RocksDB 存储扩展
│   └── mysql-storage     # MySQL 存储扩展
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
dsp-core
    ↑
dsp-storage-api
    ↑
dsp-runtime ──→ base-storage ──→ lucene-storage / rocksdb-storage / mysql-storage
    ↑
dsp-protocol
```

- **dsp-core**：无内部模块依赖，仅依赖 Guava、Lombok、Calcite 等基础库
- **dsp-storage-api**：依赖 dsp-core，定义存储引擎接口
- **dsp-runtime**：依赖 dsp-core，保留执行引擎与表达式体系
- **base-storage**：依赖 dsp-core + dsp-storage-api + dsp-runtime
- **dsp-protocol**：依赖 dsp-runtime + base-storage + lucene-storage

## 构建

```bash
mvn compile -DskipTests
```

## 重构进度

- [x] Phase 1: 创建 dsp-core（数据模型层）
- [x] Phase 2: 创建 dsp-storage-api（存储抽象层，部分完成）
- [ ] Phase 3: 迁移 Lucene 到独立 module
- [ ] Phase 4: 创建 dsp-engine（执行引擎层）
- [ ] Phase 5: 更新 protocol 依赖
- [ ] Phase 6: 清理旧模块（dsp-runtime 重复代码）

详细计划见 `docs/superpowers/plans/2026-01-02-storage-refactor-plan.md`

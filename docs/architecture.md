# DSP 架构边界

## 主运行链

| 模块 | 职责 | 允许依赖的内部模块 |
| --- | --- | --- |
| `dsp-core` | 框架无关的 Row/Value/DataType 共享数据模型 | 无 |
| `dsp-storage-api` | 存储接口、元数据接口、引擎 SPI | `dsp-core` |
| `dsp-storage-lucene` | Lucene 存储插件 | `dsp-core`、`dsp-storage-api` |
| `dsp-engine` | SQL 解析、计划、执行、目录与表生命周期 | `dsp-core`、`dsp-storage-api` |
| `dsp-protocol` | MySQL 协议、命令入口、进程启动与运行时装配 | `dsp-core`、`dsp-engine`；运行时加载存储插件 |

主链遵循依赖倒置：`dsp-engine` 通过 `StorageEngine`、`StorageEngineFactory`
和 `EngineRegistry` 使用存储能力，不引用 Lucene 类。具体存储实现由
`dsp-protocol` 在最终应用运行时引入，并通过 `ServiceLoader` 注册。

```text
dsp-core ────────────────> dsp-engine ───────────────> dsp-protocol
   └─> dsp-storage-api ───> dsp-engine
          └─> dsp-storage-lucene ──(runtime / SPI)──> dsp-protocol
```

元数据恢复也分成两个阶段：`LocalMetadataStore` / `TableMeta` 只负责反序列化表定义，
`SlothSchemaHolder` 作为生命周期协调者统一启动存储引擎并注册 Calcite Schema。

## 协议与引擎边界

- `QueryService` 统一承担 SQL 解析、计划生成、查询游标、EXPLAIN、INSERT 校验和写入。
- `QueryExecution` 只向协议层暴露列名、JDBC 类型和逐行值，不暴露 `SlothRel`、`Operator` 或 `SlothRow`。
- `CatalogService` 统一承担 Schema/Table 生命周期和只读元数据查询。
- 协议命令处理器只负责 MySQL 包转换和错误码映射，不直接访问
  `SlothSchemaHolder`、`SlothSchema`、`SlothTableEngine` 或执行计划内部类。

## 独立子系统

- `dsp-schema/*` 是一套独立的 Calcite Schema/外部数据源适配代码，目前不参与主协议运行链。
  新代码不应从主链反向依赖这些实验性适配模块。
- `dsp-raft` 和 `dsp-register` 是集群控制面与独立进程入口，目前不依赖主 SQL 引擎。
  在真正接入前保持独立，避免把 gRPC/集群依赖传入查询引擎。

## 构建约束

- `dsp-core` 的 Maven Enforcer 规则禁止 Calcite、Lucene、Netty 和上层内部模块渗入共享模型。
- `dsp-engine` 的 Maven Enforcer 规则禁止依赖 `dsp-storage-lucene`，防止具体实现重新渗入引擎。
- 每个模块必须显式声明源码直接使用的第三方库，不依赖其他模块偶然传递出来的类库。
- 实现插件和日志/JDBC 驱动在最终应用中使用 `runtime` scope；测试工具使用 `test` scope。
- 存储实现专属测试放在实现模块；跨模块完整 SQL 路径测试放在应用入口模块。

## 后续建议

1. 将协议处理器依赖的 Calcite/DDL AST 进一步收敛为引擎命令 DTO，使协议层完全脱离 Calcite。
2. 将 `dsp-schema` 明确为 connector 子系统或 Maven profile，清理其中与主引擎重复的 Operator/Value 模型。
3. 集群能力接入时通过服务接口连接主链，不让 `dsp-engine` 直接依赖 `dsp-raft` 或 `dsp-register`。

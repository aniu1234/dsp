# DSP 架构边界

## 主运行链

| 模块 | 职责 | 允许依赖的内部模块 |
| --- | --- | --- |
| `dsp-core` | 通用数据模型、类型和值语义 | 无 |
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

## 独立子系统

- `dsp-schema/*` 是独立的 Calcite connector 子系统，目前不参与主协议运行链。
  `common-schema` 只提供配置、类型转换、结果集和枚举器等连接器公共能力；
  `file-schema`、`mysql-schema`、`hive-schema` 是彼此隔离的具体适配器。
  新代码不应从主链反向依赖这些模块，连接器之间也不能互相依赖。
- 文件连接器在 Schema 创建时完成目录和表定义校验，表清单是不可变快照；
  CSV/JSON reader 负责把外部值严格转换为声明类型。
- MySQL 连接器按元数据读取和数据查询分别获取、关闭 JDBC 连接，不在 Schema 或 Table
  对象中长期持有连接。Hive 尚未实现，工厂必须快速失败而不是返回空对象。
- `dsp-raft` 和 `dsp-register` 是集群控制面与独立进程入口，目前不依赖主 SQL 引擎。
  在真正接入前保持独立，避免把 gRPC/集群依赖传入查询引擎。

## 构建约束

- `dsp-engine` 的 Maven Enforcer 规则禁止依赖 `dsp-storage-lucene`，防止具体实现重新渗入引擎。
- 每个模块必须显式声明源码直接使用的第三方库，不依赖其他模块偶然传递出来的类库。
- 实现插件和日志/JDBC 驱动在最终应用中使用 `runtime` scope；测试工具使用 `test` scope。
- 存储实现专属测试放在实现模块；跨模块完整 SQL 路径测试放在应用入口模块。

## 后续建议

1. 将 `SqlTypeName` 映射从 `dsp-core` 移到 `dsp-engine`，进一步让核心数据模型脱离 Calcite。
2. 为协议层增加稳定的 `QueryService` / `CatalogService` 门面，逐步移除命令处理器对引擎单例的直接访问。
3. 为 connector 增加统一的 SPI 和健康检查；正式接入主链前仍保持独立部署与依赖边界。
4. 集群能力接入时通过服务接口连接主链，不让 `dsp-engine` 直接依赖 `dsp-raft` 或 `dsp-register`。

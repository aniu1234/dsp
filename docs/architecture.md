# DSP 架构边界

## 主运行链

| 模块 | 职责 | 允许依赖的内部模块 |
| --- | --- | --- |
| `dsp-core` | 通用数据模型、类型和值语义 | 无 |
| `dsp-storage-api` | 存储接口、元数据接口、引擎 SPI | `dsp-core` |
| `dsp-storage-lucene` | Lucene 存储插件 | `dsp-core`、`dsp-storage-api` |
| `dsp-engine` | 应用服务门面、SQL 解析/计划/执行、目录与表生命周期 | `dsp-core`、`dsp-storage-api` |
| `dsp-protocol` | MySQL 协议适配、命令入口、进程启动与运行时装配 | `dsp-core`、`dsp-engine`；运行时加载存储插件 |

主链遵循依赖倒置：`dsp-engine` 通过 `StorageEngine`、`StorageEngineFactory`
和 `EngineRegistry` 使用存储能力，不引用 Lucene 类。具体存储实现由
`dsp-protocol` 在最终应用运行时引入，并通过 `ServiceLoader` 注册。

下图箭头表示“左侧依赖右侧”：

```text
dsp-protocol ───────────────> dsp-engine ───────────────> dsp-core
      │                           │
      │                           └──────────────> dsp-storage-api ──> dsp-core
      │
      └─(runtime / SPI)──> dsp-storage-lucene ──> dsp-storage-api
                                      └──────────> dsp-core
```

协议适配器通过 `CatalogService`、`QueryService`、`EnvironmentService` 和 `WriteService`
完成目录、查询规划、全局环境访问与写入。Calcite 的 Schema Holder、Parser Factory、
环境变量 Holder 和表存储引擎属于引擎内部实现，不再由协议命令直接编排。INSERT 的字面量
解析与 MySQL 错误码映射仍在协议适配层，类型化批次通过只读 `WriteTable`/`WriteColumn` DTO
提交给 `WriteService` 并再次校验。Query 服务仍返回部分 Calcite 对象，这是兼容当前执行模型的
过渡边界，后续可逐步替换成完全框架无关的请求/响应 DTO。

元数据恢复也分成两个阶段：`LocalMetadataStore` / `TableMeta` 只负责反序列化表定义，
`SlothSchemaHolder` 在引擎内部恢复存储实例并注册 Calcite Schema。运行时由
`CatalogService` 统一暴露目录生命周期；新建表时，`SlothSchema` 在同一临界区内完成
存储打开、元数据持久化和 Calcite 注册，协议层不直接初始化或关闭存储。

## 独立子系统

- `dsp-schema/*` 是独立的 Calcite connector 子系统，目前不参与主协议运行链。
  `common-schema` 只提供配置、类型转换、结果集和枚举器等连接器公共能力；
  `file-schema`、`mysql-schema`、`hive-schema` 是彼此隔离的具体适配器。
  新代码不应从主链反向依赖这些模块，连接器之间也不能互相依赖。
- 文件连接器在 Schema 创建时完成目录和表定义校验，表清单是不可变快照；
  CSV/JSON reader 负责把外部值严格转换为声明类型。
- MySQL 连接器按元数据读取和数据查询分别获取、关闭 JDBC 连接，不在 Schema 或 Table
  对象中长期持有连接。Hive 尚未实现，工厂必须快速失败而不是返回空对象。
- `common-schema` 通过 `SchemaConnector` 定义稳定扩展契约；具体 connector 使用
  `ServiceLoader` 注册，由 `SchemaConnectorRegistry` 按稳定 ID 发现、去重、创建并执行健康检查。
  当前 ID 为 `file-csv`、`file-json`、`mysql`、`hive`。注册中心只依赖公共契约，不引用任何
  实现类；Hive 可以被发现，但健康状态明确为 `UNAVAILABLE`。
- Calcite model 统一引用 `RegistrySchemaFactory`，并在 operand 中使用 `connector` ID；model
  不再保存具体实现类名。注册入口会移除公共的 `connector` 参数，再把剩余配置交给实现。
- Connector 依赖方向固定为 `file/mysql/hive-schema -> common-schema -> Calcite API`，
  `common-schema` 不依赖任何具体实现，具体 connector 之间也不互相依赖。新增 connector 时只需
  实现 SPI 并增加 `META-INF/services` 声明，不修改注册中心。
- `dsp-schema` 父模块通过 Maven Enforcer 禁止 connector 直接依赖其他 connector、主引擎、
  协议、集群或存储模块。只有负责跨 connector 集成验证的 `test-schema` 显式跳过该规则。
- `dsp-raft` 和 `dsp-register` 是集群控制面与独立进程入口，目前不依赖主 SQL 引擎。
  在真正接入前保持独立，避免把 gRPC/集群依赖传入查询引擎。

## 构建约束

- `dsp-core` 的 Maven Enforcer 规则禁止 Calcite 依赖；SQL 框架到内部类型的转换集中在
  `dsp-engine` 的 `CalciteTypeMapper`。
- `dsp-engine` 的 Maven Enforcer 规则禁止依赖 `dsp-storage-lucene`，防止具体实现重新渗入引擎。
- 每个模块必须显式声明源码直接使用的第三方库，不依赖其他模块偶然传递出来的类库。
- 实现插件和日志/JDBC 驱动在最终应用中使用 `runtime` scope；测试工具使用 `test` scope。
- 存储实现专属测试放在实现模块；跨模块完整 SQL 路径测试放在应用入口模块。

## 架构演进点

### 本阶段已收敛

1. **核心模型框架无关**：`dsp-core` 已移除 Calcite 依赖，类型适配下沉到引擎边界，
   并用构建规则防止回流。
2. **协议层改为端口适配器**：目录、查询规划、全局环境变量统一经过引擎服务门面，
   命令处理器不再访问 Holder/Factory 单例。
3. **表存储生命周期归目录域所有**：建表调用只提交表定义，存储初始化、元数据写入、
   Calcite 注册及失败回滚由目录域内部完成；后台 `StorageService` 只负责 flush 调度，
   不再承担目录所有权。
4. **存储实现保持插件化**：引擎仅依赖 `dsp-storage-api`，Lucene 由最终应用通过 SPI 装配。
5. **写入入口服务化**：协议层通过只读表描述准备批次，`WriteService` 统一执行资源上限、
   类型、非空、无符号和长度复核，再调用表存储引擎。
6. **建表请求 DTO 化**：协议层提交框架无关的 `CreateTableDefinition`，目录服务负责构造
   Calcite 表对象和存储生命周期，减少内部对象向适配层泄露。

### 下一阶段（按优先级）

1. **查询与目录响应 DTO 化**：移除 `QueryService`、SHOW/EXPLAIN 路径中残留的 Calcite、
   `SlothSchema`、`SlothTable` 返回类型；建表和写入入口已经先行 DTO 化。
2. **统一 connector 接入方式**：在应用装配层消费 `SchemaConnectorRegistry` 的健康状态，
   将外部 Schema 作为 Catalog 的只读数据源接入，而不是让主链依赖具体 connector。
3. **明确事务能力边界**：继续为 `WriteService` 定义幂等键、提交日志和跨分片事务语义；
   在此之前保持 statement/batch 原子边界，并继续显式拒绝事务命令。
4. **控制面通过端口接入**：为节点注册、拓扑和领导者状态定义 engine-neutral 接口，
   由应用层适配 `dsp-raft` / `dsp-register`，禁止查询引擎反向依赖集群实现。
5. **拆分部署保持可选**：当前先维持单进程模块化，只有当资源隔离、独立伸缩或故障域
   数据证明有收益时，再把协议、查询和存储拆成独立进程。

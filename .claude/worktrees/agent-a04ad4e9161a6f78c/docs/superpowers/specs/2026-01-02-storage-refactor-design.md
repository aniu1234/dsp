# DSP 存储层重构设计文档

> 日期：2026-01-02
> 状态：待审批
> 范围：全项目（core / storage / engine / protocol / cluster）

## 1. 背景与目标

### 1.1 现状

DSP (Data Serving Platform) 是一个基于 Apache Calcite 实现的 MySQL 兼容分布式分析查询引擎。
当前代码库约 287 个 Java 文件、17,630 行代码，分为 10+ 个 Maven module。

**核心问题**：

- **循环依赖**：`protocol → base-storage → runtime → protocol` 形成循环
- **职责混杂**：`base-storage` 同时包含存储实现、SQL 解析、执行算子、元数据管理
- **接口不清晰**：`StorageEngine` 混入 `LifeCycle`，`Value` 类有 10+ 个转换方法
- **难以扩展**：新增存储引擎需要修改多处代码，无法独立测试
- **JDK 兼容**：使用 `sun.misc.Unsafe` 分配原生内存，JDK 17+ 不可用

### 1.2 目标

1. **模块化拆分**：按层次垂直切分为 7 个独立 module
2. **接口标准化**：定义清晰的 `StorageEngine` 接口和 `Operator` 接口
3. **消除循环依赖**：建立单向依赖链
4. **可测试性**：每层可独立单元测试
5. **可扩展性**：新增存储引擎只需实现 `dsp-storage-api` 接口

### 1.3 非目标

- 不修改 Raft 协议和 gRPC 交互逻辑
- 不修改 MySQL 协议编解码细节
- 不引入 Spring 或任何 DI 框架
- 不重写 Calcite 集成逻辑（只移动和重命名）

---

## 2. 模块划分

```
dsp (parent POM)
├── dsp-core              # 数据模型层
├── dsp-storage-api       # 存储抽象接口
├── dsp-storage-lucene    # Lucene 存储实现
├── dsp-storage-rocksdb   # RocksDB 存储实现（骨架）
├── dsp-engine            # 执行引擎层
├── dsp-protocol          # MySQL 协议层（基本不动）
└── dsp-cluster           # 分布式协调层（基本不动）
```

### 2.1 dsp-core（数据模型）

从 `dsp-runtime` 拆分出的纯数据模型层。

**包结构**：
```
core/
├── data/          # Value, Row, DataType, DataTypes
├── block/         # AbstractBlock → IntBlock, LongBlock, StringBlock
└── column/        # AbstractColumn → InMemoryColumn
```

**关键变更**：
- `Value` 从 class 改为 record，线程安全
- `DataType` 从 abstract class 改为 sealed interface
- `Block` 用 `ByteBuffer` 替代 `sun.misc.Unsafe`
- `Row` 去掉 EOF_ROW 哨兵模式

### 2.2 dsp-storage-api（存储抽象）

纯接口定义模块，零外部依赖。

**包结构**：
```
engine/      # StorageEngine, StorageEngineFactory, EngineRegistry
query/       # Query, QueryContext, BaseQuery
iterator/    # ResultSetIterator, MergeResultSetIterator
meta/        # MetadataStore, TableInfo, ColumnInfo (接口)
service/     # StorageManager, FlushScheduler
```

**关键变更**：
- `StorageEngine` 去掉 `LifeCycle` 接口，改为 `append/scan/flush/close`
- 元数据接口化（`MetadataStore`），不再单例
- `Query` 改为函数式接口（predicate + columns）

### 2.3 dsp-storage-lucene（Lucene 实现）

Lucene 存储的具体实现。

**包结构**：
```
LuceneStorageEngine.java    # 实现 StorageEngine
LuceneDocumentMapper.java   # 行 ↔ 文档转换（独立类）
```

**关键变更**：
- 移除对 `SlothTableEngine` 的直接依赖
- 文档转换逻辑抽取为 `LuceneDocumentMapper`
- 通过 `EngineConfig` 接收配置

### 2.4 dsp-storage-rocksdb（RocksDB 骨架）

RocksDB 存储的预留实现。

```
RocksDbStorageEngine.java   # 实现 StorageEngine（TODO）
```

### 2.5 dsp-engine（执行引擎）

从 `base-storage/parser` + `base-storage/parser/rel` + `base-storage/parser/operator` + `base-storage/parser/rules` 拆分。

**包结构**：
```
calcite/    # SlothParser, SlothSchema, SlothTable, Convention
rel/        # SlothTableScan, SlothFilter, SlothProject, SlothJoin, SlothAggregate, SlothSort
optimizer/  # SlothRules, cbo/, rbo/
converter/  # SlothConverterRule (RelNode → Operator)
execution/  # Operator 接口 + 所有算子实现
```

**关键变更**：
- `Operator` 统一接口：`open()/hasNext()/next()/close()`
- 算子工厂 `OperatorFactory` 从 RelNode 创建算子链
- `ExecutionPlan` 封装完整的算子链

### 2.6 dsp-protocol（MySQL 协议层）

基本不动，只修改依赖声明。

**当前**：依赖 `dsp-runtime` + `base-storage` + `lucene-storage`
**之后**：依赖 `dsp-engine` + `dsp-storage-lucene`

### 2.7 dsp-cluster（分布式协调层）

基本不动，只修改依赖声明。

合并原有的 `dsp-raft` + `dsp-register` 为一个 module。

---

## 3. 模块依赖关系

```
dsp-core
  ↑ 被所有其他模块依赖

dsp-storage-api → dsp-core
  ↑ 被 storage-lucene, storage-rocksdb, engine 依赖

dsp-storage-lucene → dsp-storage-api + dsp-core
dsp-storage-rocksdb → dsp-storage-api + dsp-core

dsp-engine → dsp-storage-api + dsp-core
  ↑ 被 protocol 依赖

dsp-protocol → dsp-engine + dsp-storage-lucene
dsp-cluster → dsp-core
```

**依赖方向永远是单向的**，从上往下依赖。不存在任何循环。

---

## 4. 核心接口设计

### 4.1 dsp-core 数据模型

```java
// 数据类型 — sealed interface
public sealed interface DataType<T> permits
    IntegerType, LongType, StringType, DoubleType, ... {
    int id();
    String name();
    Class<T> javaType();
    T coerce(Object value);
    boolean isNull(Object value);
}

// 值 — 不可变记录
public record Value(DataType<?> type, Object raw) {
    public static Value nullValue(DataType<?> type);
    public boolean isNull();
}

// 行 — 不可变
public final class Row {
    private final Value[] values;
    public static Row of(Value... values);
    public static Row empty(int columnCount);
    public int size();
    public Value get(int index);
    public Value[] values();
}

// 块 — ByteBuffer 替代 Unsafe
public abstract class Block {
    protected ByteBuffer buffer;
    protected BitSet nulls;
    public abstract void append(Object value);
    public abstract Object get(int index);
    public abstract boolean isNull(int index);
    public abstract void free();
}

// 列
public interface Column {
    int size();
    Object get(int index);
    boolean isNull(int index);
    void append(Object value);
    void flush();
}
```

### 4.2 dsp-storage-api 存储接口

```java
// 存储引擎接口
public interface StorageEngine {
    WriteResult append(Row... rows);
    <R> Iterator<R> scan(Query<R> query);
    long estimateRowCount();
    void flush();
    void close();
}

public record WriteResult(int inserted, long timestamp) {}

// 查询谓词
public interface Query<R> {
    Predicate<Row> predicate();
    Set<String> columns();
    Class<R> returnType();
}

// 元数据接口
public interface MetadataStore {
    void createSchema(String schemaName);
    void dropSchema(String schemaName);
    List<String> listSchemas();
    void createTable(CreateTableRequest request);
    void dropTable(String schema, String table);
    TableInfo getTable(String schema, String table);
    List<TableInfo> listTables(String schema);
}

public record TableInfo(String catalog, String schema, String name,
                        String engine, int shards, String comment,
                        List<ColumnInfo> columns) {}

public record ColumnInfo(String name, DataType<?> type,
                         boolean nullable, Object defaultValue,
                         String comment) {}

// 引擎注册表
public final class EngineRegistry {
    public static void register(StorageEngineFactory factory);
    public static StorageEngine create(String type, EngineConfig config);
}

public record EngineConfig(String storagePath, int shardNum,
                           List<DataType<?>> columnTypes,
                           Map<String, Object> properties) {}
```

### 4.3 dsp-engine 执行接口

```java
// 算子接口
public interface Operator {
    void open();
    boolean hasNext();
    Row next();
    void close();
    SchemaInspector inspector();
}

// 执行计划
public class ExecutionPlan {
    private final Operator head;
    private final SchemaInspector inspector;
    public Iterator<Row> execute();
    public SchemaInspector schema();
}

// 算子工厂
public interface OperatorFactory {
    Operator create(RelNode relNode, StorageManager storageManager);
}
```

---

## 5. 查询执行流程

```
SQL 字符串
  → SlothParser.getPlan()
    → RelNode (Calcite 逻辑计划)
      → HepPlanner (RBO 优化: 常量折叠/谓词下推)
        → VolcanoPlanner (CBO 优化: Join选择/TableScan选择)
          → RelNode with SlothConvention
            → OperatorFactory.create()
              → Operator chain
                → Operator.open() → next() → close()
                  ↑
                  └── TableScanOperator → StorageEngine.scan()
                  ↑
                  └── FilterOperator → 谓词求值
                  ↑
                  └── ProjectOperator → 列投影
```

---

## 6. 迁移步骤

### Phase 1: 拆分 core（无外部影响）
1. 创建 `dsp-core` module
2. 从 `dsp-runtime` 提取 `core.data`（Value/Row/DataType/DataTypes）
3. 从 `dsp-runtime` 提取 `core.block`（AbstractBlock/IntBlock/StringBlock）
4. 从 `dsp-runtime` 提取 `core.column`（AbstractColumn）
5. 应用重构（record/sealed/ByteBuffer）
6. 更新 `dsp-runtime` pom 依赖 `dsp-core`

### Phase 2: 定义 storage-api（最小侵入）
7. 创建 `dsp-storage-api` module
8. 提取 `StorageEngine` 接口（精简版）
9. 提取 `Query`/`QueryContext`/`ResultSetIterator`
10. 提取 `MetadataStore` 接口（替代 jOOQ 单例）
11. 提取 `StorageManager`/`FlushScheduler`
12. 更新 `base-storage` pom 依赖 `dsp-storage-api`

### Phase 3: 重构 Lucene 实现
13. `LuceneStorageEngine` 实现新的 `StorageEngine` 接口
14. 抽取 `LuceneDocumentMapper` 独立类
15. 移除对 `SlothTableEngine` 的直接依赖

### Phase 4: 拆分 engine
16. 创建 `dsp-engine` module
17. 从 `base-storage/parser` 提取到 `dsp-engine/calcite`
18. 从 `base-storage/parser/rel` 提取到 `dsp-engine/rel`
19. 从 `base-storage/parser/operator` 提取到 `dsp-engine/execution`
20. 从 `base-storage/parser/rules` 提取到 `dsp-engine/optimizer`
21. 统一 `Operator` 接口
22. 更新 `base-storage` 只剩存储实现

### Phase 5: 清理 protocol
23. `dsp-protocol` 改为依赖 `dsp-engine` + `dsp-storage-lucene`
24. 移除对 `base-storage` 的直接依赖

### Phase 6: 清理 cluster
25. 合并 `dsp-raft` + `dsp-register` → `dsp-cluster`
26. 移除对存储后端的依赖

### Phase 7: 清理旧模块
27. 删除 `dsp-runtime`（功能已迁移）
28. 删除 `dsp-storage` 父 module 和 `base-storage`
29. 评估 `dsp-schema` 去留
30. 全量编译验证

---

## 7. 风险与缓解

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| Calcite 集成代码量大，迁移容易出错 | 引擎层不可用 | 先迁移数据模型，再迁移 Calcite 集成 |
| jOOQ 生成的代码较多 | 元数据层迁移慢 | 保留 jOOQ 但只作为 MySQL 实现，接口层用自定义 POJO |
| Operator 链重构可能引入 bug | 查询结果错误 | 每个算子写单元测试 |
| 模块数量从 10+ 变为 7 | 构建时间可能增加 | 并行编译，Maven reactor |

---

## 8. 成功标准

- [ ] 7 个 module 各自独立编译通过
- [ ] 无任何循环依赖
- [ ] `dsp-storage-api` 零外部依赖
- [ ] `dsp-core` 可独立于所有其他模块测试
- [ ] 至少有一个存储引擎（Lucene）的端到端测试
- [ ] 现有 SELECT/INSERT 查询功能正常
- [ ] `StorageEngine` 接口可被 Mock，支持单元测试

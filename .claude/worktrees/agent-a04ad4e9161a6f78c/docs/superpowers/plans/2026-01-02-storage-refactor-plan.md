# 存储层重构 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 DSP 项目从当前的单体存储架构拆分为 7 个独立 Maven module，建立清晰的单向依赖链，使存储层可独立扩展和测试。

**Architecture:** 按层次垂直切分：dsp-core（数据模型）→ dsp-storage-api（存储接口）→ dsp-storage-lucene（Lucene 实现）+ dsp-engine（执行引擎）→ dsp-protocol（协议层）。每层独立编译，通过接口通信。

**Tech Stack:** Java 8, Maven, Apache Calcite 1.32.0, Apache Lucene 8.6.0, Netty 4.1.81, Lombok 1.18.10, Guava 27.1-jre

---

## Phase 0: 准备工作

### Task 0.1: 备份当前状态

**Files:**
- N/A (git operation only)

- [ ] **Step 1: 创建备份分支**

```bash
git checkout -b backup-pre-refactor
git checkout main
```

- [ ] **Step 2: 确认当前项目能编译**

```bash
cd /Users/liuzm/ai/dsp && mvn compile -DskipTests -q
```

Expected: BUILD SUCCESS

- [ ] **Step 3: 记录当前 module 列表**

```bash
grep -A 20 '<modules>' /Users/liuzm/ai/dsp/pom.xml
```

---

## Phase 1: 创建 dsp-core（数据模型层）

### Task 1.1: 创建 dsp-core module 骨架

**Files:**
- Create: `dsp-core/pom.xml`

- [ ] **Step 1: 创建 dsp-core/pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <artifactId>dsp</artifactId>
        <groupId>com.qinyadan.system</groupId>
        <version>1.0-SNAPSHOT</version>
    </parent>

    <artifactId>dsp-core</artifactId>

    <properties>
        <maven.compiler.source>8</maven.compiler.source>
        <maven.compiler.target>8</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <dependencies>
        <dependency>
            <groupId>com.google.guava</groupId>
            <artifactId>guava</artifactId>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
        </dependency>
        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-api</artifactId>
        </dependency>
        <dependency>
            <groupId>junit</groupId>
            <artifactId>junit</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 2: 添加到父 POM modules**

编辑 `dsp/pom.xml`，在 `<modules>` 中添加 `<module>dsp-core</module>` 作为第一个 module。

### Task 1.2: 迁移 DataType 体系

**Files:**
- Create: `dsp-core/src/main/java/com/qinyadan/system/dsp/core/data/type/DataType.java`
- Create: `dsp-core/src/main/java/com/qinyadan/system/dsp/core/data/type/DataTypes.java`
- Create: `dsp-core/src/main/java/com/qinyadan/system/dsp/core/data/type/IntegerType.java`
- Create: `dsp-core/src/main/java/com/qinyadan/system/dsp/core/data/type/LongType.java`
- Create: `dsp-core/src/main/java/com/qinyadan/system/dsp/core/data/type/StringType.java`
- Create: `dsp-core/src/main/java/com/qinyadan/system/dsp/core/data/type/DoubleType.java`
- Create: `dsp-core/src/main/java/com/qinyadan/system/dsp/core/data/type/FloatType.java`
- Create: `dsp-core/src/main/java/com/qinyadan/system/dsp/core/data/type/ShortType.java`
- Create: `dsp-core/src/main/java/com/qinyadan/system/dsp/core/data/type/ByteType.java`
- Create: `dsp-core/src/main/java/com/qinyadan/system/dsp/core/data/type/BooleanType.java`
- Create: `dsp-core/src/main/java/com/qinyadan/system/dsp/core/data/type/DateType.java`
- Create: `dsp-core/src/main/java/com/qinyadan/system/dsp/core/data/type/TimestampType.java`
- Create: `dsp-core/src/main/java/com/qinyadan/system/dsp/core/data/type/FixedWidthType.java`
- Create: `dsp-core/src/main/java/com/qinyadan/system/dsp/core/data/type/Streamer.java`

- [ ] **Step 1: 创建 DataType.java 基类**

```java
package com.qinyadan.system.dsp.core.data.type;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Comparator;

public abstract class DataType<T> implements Streamer<T>, Comparator<T> {

    public enum Precedence {
        NOT_SUPPORTED, UNDEFINED, LITERAL, STRING, BYTE, BOOLEAN, SHORT,
        INTERVAL, TIMESTAMP_WITH_TIME_ZONE, DATE, TIMESTAMP, LONG,
        FLOAT, DOUBLE, ARRAY, SET, TABLE, GEO_POINT, OBJECT,
        UNCHECKED_OBJECT, GEO_SHAPE, CUSTOM
    }

    public abstract int id();
    public abstract Precedence precedence();
    public abstract String getName();
    public abstract T value(Object value);

    public Streamer<T> streamer() {
        return this;
    }

    public boolean precedes(DataType<?> other) {
        return this.precedence().ordinal() > other.precedence().ordinal();
    }

    public boolean isConvertableTo(DataType<?> other) {
        if (this.equals(other)) return true;
        java.util.Set<Integer> possible = DataTypes.ALLOWED_CONVERSIONS.get(id());
        return possible != null && possible.contains(other.id());
    }

    @Override
    public int hashCode() { return id(); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DataType)) return false;
        DataType<?> that = (DataType<?>) o;
        return id() == that.id();
    }

    @Override
    public int compareTo(DataType<?> o) {
        return Integer.compare(id(), o.id());
    }

    @Override
    public String toString() {
        return getName();
    }

    public Value createByType(Object o) {
        return new Value(o, this);
    }
}
```

- [ ] **Step 2: 创建 DataTypes.java**

复制 `dsp-runtime/.../data/type/DataTypes.java` 到 `dsp-core`，保持内容完全一致。

- [ ] **Step 3: 创建各具体 Type 类**

从 `dsp-runtime/.../data/type/` 逐个复制以下文件到 `dsp-core/.../data/type/`，修改 package 为 `com.qinyadan.system.dsp.core.data.type`：

- `IntegerType.java` — 修复 bug: `Short.compare(o1, o1)` → `Short.compare(o1, o2)`（注意这是 ShortType 的 bug，不是 IntegerType 的）
- `LongType.java`
- `StringType.java`
- `DoubleType.java`
- `FloatType.java`
- `ShortType.java` — 修复 bug: `Short.compare(o1, o1)` → `Short.compare(o1, o2)`
- `ByteType.java`
- `BooleanType.java`
- `DateType.java`
- `TimestampType.java`
- `FixedWidthType.java`
- `Streamer.java`

每个文件只改 package 声明，其余保持原样。

- [ ] **Step 4: 编译验证 dsp-core**

```bash
cd /Users/liuzm/ai/dsp && mvn compile -pl dsp-core -q
```

Expected: BUILD SUCCESS

- [ ] **Step 5: 提交**

```bash
git add dsp-core/pom.xml dsp-core/src/
git commit -m "feat(core): add data type system (DataType, DataTypes, Value types)" \
  -m "Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

### Task 1.3: 迁移 Value 和 Row

**Files:**
- Create: `dsp-core/src/main/java/com/qinyadan/system/dsp/core/data/value/Value.java`
- Create: `dsp-core/src/main/java/com/qinyadan/system/dsp/core/data/value/IntValue.java`
- Create: `dsp-core/src/main/java/com/qinyadan/system/dsp/core/data/value/DoubleValue.java`
- Create: `dsp-core/src/main/java/com/qinyadan/system/dsp/core/data/Row.java`
- Create: `dsp-core/src/main/java/com/qinyadan/system/dsp/core/data/RowImpl.java`

- [ ] **Step 1: 创建 Row.java 接口**

```java
package com.qinyadan.system.dsp.core.data;

import com.qinyadan.system.dsp.core.data.value.Value;

public interface Row {
    int columnSize();
    Value getColumn(int i);
    Value[] getColumns();
}
```

- [ ] **Step 2: 创建 RowImpl.java**

```java
package com.qinyadan.system.dsp.core.data;

import com.qinyadan.system.dsp.core.data.value.Value;

public final class RowImpl implements Row {
    private final Value[] values;

    private RowImpl(Value[] values) {
        this.values = values;
    }

    public static RowImpl of(Value... values) {
        return new RowImpl(values);
    }

    public static RowImpl empty(int columnCount) {
        Value[] vals = new Value[columnCount];
        for (int i = 0; i < columnCount; i++) vals[i] = Value.nullValue(null);
        return new RowImpl(vals);
    }

    @Override
    public int columnSize() { return values.length; }

    @Override
    public Value getColumn(int i) { return values[i]; }

    @Override
    public Value[] getColumns() { return values; }
}
```

- [ ] **Step 3: 迁移 Value.java**

从 `dsp-runtime/.../data/value/Value.java` 复制到 `dsp-core/.../value/Value.java`，修改 package 为 `com.qinyadan.system.dsp.core.data.value`。

保持内容不变（包括所有 `xxxValue()` 方法），因为这是行为正确性的基础。

- [ ] **Step 4: 迁移 IntValue.java 和 DoubleValue.java**

同样只改 package，保持内容不变。

- [ ] **Step 5: 编译验证**

```bash
cd /Users/liuzm/ai/dsp && mvn compile -pl dsp-core -q
```

- [ ] **Step 6: 提交**

```bash
git add dsp-core/src/main/java/com/qinyadan/system/dsp/core/data/
git commit -m "feat(core): add Value, Row data model" \
  -m "Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

### Task 1.4: 迁移 Block 和 Column

**Files:**
- Create: `dsp-core/src/main/java/com/qinyadan/system/dsp/core/block/Block.java`
- Create: `dsp-core/src/main/java/com/qinyadan/system/dsp/core/block/BlockConstants.java`
- Create: `dsp-core/src/main/java/com/qinyadan/system/dsp/core/block/IntBlock.java`
- Create: `dsp-core/src/main/java/com/qinyadan/system/dsp/core/block/StringBlock.java`
- Create: `dsp-core/src/main/java/com/qinyadan/system/dsp/core/column/Column.java`
- Create: `dsp-core/src/main/java/com/qinyadan/system/dsp/core/column/AbstractColumn.java`
- Create: `dsp-core/src/main/java/com/qinyadan/system/dsp/core/column/IntegerColumn.java`
- Create: `dsp-core/src/main/java/com/qinyadan/system/dsp/core/column/StringColumn.java`

- [ ] **Step 1: 创建 Block.java（替代 AbstractBlock）**

```java
package com.qinyadan.system.dsp.core.block;

import java.util.BitSet;

public abstract class Block {
    protected static final int DEFAULT_CAPACITY = 1 << 16; // 65536

    protected java.nio.ByteBuffer buffer;
    protected BitSet nulls;
    protected int size;
    protected int currentSize;

    public Block(int size) {
        this.size = size;
        this.buffer = java.nio.ByteBuffer.allocateDirect(size * 4); // 默认 4 字节
        this.currentSize = 0;
        this.nulls = new BitSet(size);
    }

    public abstract boolean canAdd();
    public abstract void append(Object value);
    public abstract Object read(int index);
    public abstract boolean isNull(int index);

    public void free() {
        if (buffer != null && buffer.isDirect()) {
            buffer.clear();
        }
    }

    public int currentSize() { return currentSize; }
}
```

- [ ] **Step 2: 迁移 BlockConstants.java**

```java
package com.qinyadan.system.dsp.core.block;

public final class BlockConstants {
    public static final int BLOCK_SIZE = 1 << 16;
}
```

- [ ] **Step 3: 创建 IntBlock.java（用 ByteBuffer 替代 Unsafe）**

```java
package com.qinyadan.system.dsp.core.block;

public final class IntBlock extends Block {
    public IntBlock(int capacity) {
        super(capacity);
        this.buffer = java.nio.ByteBuffer.allocateDirect(capacity * 4);
        this.buffer.order(java.nio.ByteOrder.nativeOrder());
    }

    @Override
    public boolean canAdd() {
        return currentSize + 1 <= size;
    }

    @Override
    public void append(Object value) {
        if (value == null) {
            nulls.set(currentSize);
        } else {
            nulls.clear(currentSize);
            buffer.putInt(currentSize * 4, (Integer) value);
        }
        currentSize++;
    }

    @Override
    public Object read(int index) {
        if (isNull(index)) return null;
        buffer.position(index * 4);
        return buffer.getInt();
    }

    @Override
    public boolean isNull(int index) {
        return nulls.get(index);
    }
}
```

- [ ] **Step 4: 迁移 StringBlock.java（保持 stub 状态）**

从 `dsp-runtime` 复制，只改 package。

- [ ] **Step 5: 创建 Column.java 接口**

```java
package com.qinyadan.system.dsp.core.column;

public interface Column {
    int size();
    Object get(int index);
    boolean isNull(int index);
    void append(Object value);
    void flush();
}
```

- [ ] **Step 6: 迁移 AbstractColumn.java**

从 `dsp-runtime` 复制，修改 package 为 `com.qinyadan.system.dsp.core.column`。

- [ ] **Step 7: 迁移 IntegerColumn.java 和 StringColumn.java**

同样只改 package。

- [ ] **Step 8: 编译验证**

```bash
cd /Users/liuzm/ai/dsp && mvn compile -pl dsp-core -q
```

- [ ] **Step 9: 提交**

```bash
git add dsp-core/src/main/java/com/qinyadan/system/dsp/core/block/ \
       dsp-core/src/main/java/com/qinyadan/system/dsp/core/column/
git commit -m "feat(core): add Block and Column with ByteBuffer (no Unsafe)" \
  -m "Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

### Task 1.5: 迁移 runtime util 到 dsp-core

**Files:**
- Create: `dsp-core/src/main/java/com/qinyadan/system/dsp/core/util/TypeConversionUtils.java`
- Create: `dsp-core/src/main/java/com/qinyadan/system/dsp/core/util/TimeUtils.java`
- Create: `dsp-core/src/main/java/com/qinyadan/system/dsp/core/util/StringUtil.java`
- Create: `dsp-core/src/main/java/com/qinyadan/system/dsp/core/constant/FileConstants.java`
- Create: `dsp-core/src/main/java/com/qinyadan/system/dsp/core/constant/TypeConstants.java`

- [ ] **Step 1: 迁移工具类**

从 `dsp-runtime` 复制以下文件到 `dsp-core`，修改 package：

- `util/TypeConversionUtils.java` → `com.qinyadan.system.dsp.core.util.TypeConversionUtils`
- `util/TimeUtils.java` → `com.qinyadan.system.dsp.core.util.TimeUtils`
- `util/StringUtil.java` → `com.qinyadan.system.dsp.core.util.StringUtil`
- `constant/FileConstants.java` → `com.qinyadan.system.dsp.core.constant.FileConstants`
- `constant/TypeConstants.java` → `com.qinyadan.system.dsp.core.constant.TypeConstants`

保持内容不变。

- [ ] **Step 2: 编译验证**

```bash
cd /Users/liuzm/ai/dsp && mvn compile -pl dsp-core -q
```

- [ ] **Step 3: 提交**

```bash
git add dsp-core/src/main/java/com/qinyadan/system/dsp/core/util/ \
       dsp-core/src/main/java/com/qinyadan/system/dsp/core/constant/
git commit -m "feat(core): migrate utility classes to dsp-core" \
  -m "Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

### Task 1.6: 更新 dsp-runtime 依赖 dsp-core

**Files:**
- Modify: `dsp-runtime/pom.xml`

- [ ] **Step 1: 添加 dsp-core 依赖**

在 `dsp-runtime/pom.xml` 的 `<dependencies>` 中添加：

```xml
<dependency>
    <groupId>com.qinyadan.system</groupId>
    <artifactId>dsp-core</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

- [ ] **Step 2: 编译验证**

```bash
cd /Users/liuzm/ai/dsp && mvn compile -pl dsp-runtime -q
```

- [ ] **Step 3: 提交**

```bash
git add dsp-runtime/pom.xml
git commit -m "chore(runtime): depend on dsp-core" \
  -m "Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Phase 2: 创建 dsp-storage-api（存储抽象层）

### Task 2.1: 创建 dsp-storage-api module 骨架

**Files:**
- Create: `dsp-storage-api/pom.xml`

- [ ] **Step 1: 创建 pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <artifactId>dsp</artifactId>
        <groupId>com.qinyadan.system</groupId>
        <version>1.0-SNAPSHOT</version>
    </parent>

    <artifactId>dsp-storage-api</artifactId>

    <properties>
        <maven.compiler.source>8</maven.compiler.source>
        <maven.compiler.target>8</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <dependencies>
        <dependency>
            <groupId>com.qinyadan.system</groupId>
            <artifactId>dsp-core</artifactId>
            <version>1.0-SNAPSHOT</version>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
        </dependency>
        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-api</artifactId>
        </dependency>
        <dependency>
            <groupId>junit</groupId>
            <artifactId>junit</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 2: 添加到父 POM**

在 `dsp/pom.xml` 的 `<modules>` 中添加 `<module>dsp-storage-api</module>`（在 `dsp-core` 之后）。

### Task 2.2: 创建 StorageEngine 接口

**Files:**
- Create: `dsp-storage-api/src/main/java/com/qinyadan/system/dsp/storage/api/StorageEngine.java`
- Create: `dsp-storage-api/src/main/java/com/qinyadan/system/dsp/storage/api/WriteResult.java`
- Create: `dsp-storage-api/src/main/java/com/qinyadan/system/dsp/storage/api/EngineConfig.java`
- Create: `dsp-storage-api/src/main/java/com/qinyadan/system/dsp/storage/api/EngineRegistry.java`
- Create: `dsp-storage-api/src/main/java/com/qinyadan/system/dsp/storage/api/StorageEngineFactory.java`

- [ ] **Step 1: 创建 StorageEngine.java**

```java
package com.qinyadan.system.dsp.storage.api;

import com.qinyadan.system.dsp.core.data.Row;
import com.qinyadan.system.dsp.storage.api.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Iterator;

public interface StorageEngine {

    Logger LOG = LoggerFactory.getLogger(StorageEngine.class);

    /**
     * Append rows to this engine.
     */
    WriteResult append(Row... rows) throws IOException;

    /**
     * Scan rows matching the query.
     */
    <R> Iterator<R> scan(Query<R> query) throws IOException;

    /**
     * Estimate row count.
     */
    long estimateRowCount();

    /**
     * Flush in-memory data to persistent storage.
     */
    default void flush() {
        LOG.info("flush not implemented for {}", getClass().getSimpleName());
    }

    /**
     * Check if flush is needed.
     */
    default boolean shouldFlush() {
        return false;
    }

    /**
     * Close resources.
     */
    void close();

    /**
     * Check if this engine is read-only.
     */
    default boolean readOnly() { return false; }

    /**
     * Set read-only mode.
     */
    default void setReadOnly(boolean readOnly) {}
}
```

- [ ] **Step 2: 创建 WriteResult.java**

```java
package com.qinyadan.system.dsp.storage.api;

public record WriteResult(int inserted, long timestamp) {}
```

- [ ] **Step 3: 创建 EngineConfig.java**

```java
package com.qinyadan.system.dsp.storage.api;

import com.qinyadan.system.dsp.core.data.type.DataType;
import java.util.List;
import java.util.Map;

public record EngineConfig(
    String storagePath,
    int shardNum,
    List<DataType<?>> columnTypes,
    Map<String, Object> properties
) {}
```

- [ ] **Step 4: 创建 EngineRegistry.java**

```java
package com.qinyadan.system.dsp.storage.api;

import java.util.concurrent.ConcurrentHashMap;

public final class EngineRegistry {

    private static final Map<String, StorageEngineFactory> FACTORIES = new ConcurrentHashMap<>();

    public static void register(StorageEngineFactory factory) {
        FACTORIES.put(factory.type(), factory);
    }

    public static StorageEngine create(String type, EngineConfig config) {
        StorageEngineFactory factory = FACTORIES.get(type);
        if (factory == null) {
            throw new IllegalStateException("No engine factory registered for type: " + type);
        }
        return factory.create(config);
    }

    public static java.util.Set<String> availableTypes() {
        return java.util.Collections.unmodifiableSet(FACTORIES.keySet());
    }
}
```

- [ ] **Step 5: 创建 StorageEngineFactory.java**

```java
package com.qinyadan.system.dsp.storage.api;

public interface StorageEngineFactory {
    String type();
    StorageEngine create(EngineConfig config);
}
```

- [ ] **Step 6: 编译验证**

```bash
cd /Users/liuzm/ai/dsp && mvn compile -pl dsp-storage-api -q
```

- [ ] **Step 7: 提交**

```bash
git add dsp-storage-api/pom.xml dsp-storage-api/src/main/java/
git commit -m "feat(api): add StorageEngine interface and EngineRegistry" \
  -m "Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

### Task 2.3: 创建 Query 和 Iterator 接口

**Files:**
- Create: `dsp-storage-api/src/main/java/com/qinyadan/system/dsp/storage/api/query/Query.java`
- Create: `dsp-storage-api/src/main/java/com/qinyadan/system/dsp/storage/api/query/BaseQuery.java`
- Create: `dsp-storage-api/src/main/java/com/qinyadan/system/dsp/storage/api/query/QueryContext.java`
- Create: `dsp-storage-api/src/main/java/com/qinyadan/system/dsp/storage/api/iterator/ResultSetIterator.java`
- Create: `dsp-storage-api/src/main/java/com/qinyadan/system/dsp/storage/api/iterator/MergeResultSetIterator.java`

- [ ] **Step 1: 创建 Query.java**

```java
package com.qinyadan.system.dsp.storage.api.query;

import java.io.Serializable;
import java.util.Set;

public interface Query extends Serializable {
    String getSql();
    Set<String> getColumnNames();
}
```

- [ ] **Step 2: 创建 BaseQuery.java**

```java
package com.qinyadan.system.dsp.storage.api.query;

import lombok.Builder;
import lombok.Data;

import java.util.HashSet;
import java.util.Set;

@Data
@Builder
public class BaseQuery implements Query {
    private String query;
    private Set<String> columnNames;

    @Override
    public Set<String> getColumnNames() {
        return columnNames != null ? columnNames : new HashSet<>();
    }
}
```

- [ ] **Step 3: 创建 QueryContext.java**

```java
package com.qinyadan.system.dsp.storage.api.query;

import lombok.Getter;

@Getter
public class QueryContext {
    private final Query query;
    private final Set<String> columnNames;

    public QueryContext(Query query, Set<String> columnNames) {
        this.query = query;
        this.columnNames = columnNames;
    }
}
```

- [ ] **Step 4: 创建 ResultSetIterator.java**

```java
package com.qinyadan.system.dsp.storage.api.iterator;

public interface ResultSetIterator<T> {
    boolean hasNext();
    T next();
}
```

- [ ] **Step 5: 创建 MergeResultSetIterator.java**

从 `base-storage` 复制 `SlothMergeIterator`，修改 package 为 `com.qinyadan.system.dsp.storage.api.iterator`，保持逻辑不变。

- [ ] **Step 6: 编译验证**

```bash
cd /Users/liuzm/ai/dsp && mvn compile -pl dsp-storage-api -q
```

- [ ] **Step 7: 提交**

```bash
git add dsp-storage-api/src/main/java/com/qinyadan/system/dsp/storage/api/query/ \
       dsp-storage-api/src/main/java/com/qinyadan/system/dsp/storage/api/iterator/
git commit -m "feat(api): add Query, QueryContext, ResultSetIterator" \
  -m "Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

### Task 2.4: 创建元数据接口

**Files:**
- Create: `dsp-storage-api/src/main/java/com/qinyadan/system/dsp/storage/api/meta/MetadataStore.java`
- Create: `dsp-storage-api/src/main/java/com/qinyadan/system/dsp/storage/api/meta/TableInfo.java`
- Create: `dsp-storage-api/src/main/java/com/qinyadan/system/dsp/storage/api/meta/ColumnInfo.java`
- Create: `dsp-storage-api/src/main/java/com/qinyadan/system/dsp/storage/api/meta/CreateTableRequest.java`
- Create: `dsp-storage-api/src/main/java/com/qinyadan/system/dsp/storage/api/meta/OpenTableRequest.java`
- Create: `dsp-storage-api/src/main/java/com/qinyadan/system/dsp/storage/api/service/StorageManager.java`
- Create: `dsp-storage-api/src/main/java/com/qinyadan/system/dsp/storage/api/service/FlushScheduler.java`

- [ ] **Step 1: 创建 TableInfo.java**

```java
package com.qinyadan.system.dsp.storage.api.meta;

import com.qinyadan.system.dsp.core.data.type.DataType;
import java.util.List;

public record TableInfo(
    String catalog,
    String schema,
    String name,
    String engine,
    int shards,
    String comment,
    List<ColumnInfo> columns
) {}
```

- [ ] **Step 2: 创建 ColumnInfo.java**

```java
package com.qinyadan.system.dsp.storage.api.meta;

import com.qinyadan.system.dsp.core.data.type.DataType;

public record ColumnInfo(
    String name,
    DataType<?> type,
    boolean nullable,
    Object defaultValue,
    String comment
) {}
```

- [ ] **Step 3: 创建 CreateTableRequest.java**

```java
package com.qinyadan.system.dsp.storage.api.meta;

import java.util.List;

public record CreateTableRequest(
    String schema,
    String tableName,
    String engine,
    int shardNum,
    String comment,
    List<ColumnInfo> columns
) {}
```

- [ ] **Step 4: 创建 OpenTableRequest.java**

```java
package com.qinyadan.system.dsp.storage.api.meta;

import java.util.List;

public record OpenTableRequest(
    String schema,
    String tableName,
    List<ColumnInfo> columns
) {}
```

- [ ] **Step 5: 创建 MetadataStore.java**

```java
package com.qinyadan.system.dsp.storage.api.meta;

import java.util.List;

public interface MetadataStore {
    void createSchema(String schemaName);
    void dropSchema(String schemaName);
    List<String> listSchemas();

    void createTable(CreateTableRequest request);
    void dropTable(String schema, String table);
    TableInfo getTable(String schema, String table);
    List<TableInfo> listTables(String schema);
}
```

- [ ] **Step 6: 创建 StorageManager.java**

```java
package com.qinyadan.system.dsp.storage.api.service;

import com.qinyadan.system.dsp.storage.api.StorageEngine;
import com.qinyadan.system.dsp.storage.api.EngineConfig;
import com.qinyadan.system.dsp.storage.api.EngineRegistry;
import com.qinyadan.system.dsp.storage.api.meta.CreateTableRequest;
import com.qinyadan.system.dsp.storage.api.meta.MetadataStore;
import com.qinyadan.system.dsp.storage.api.meta.OpenTableRequest;
import com.qinyadan.system.dsp.storage.api.meta.TableInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class StorageManager {
    private static final Logger LOG = LoggerFactory.getLogger(StorageManager.class);

    private final MetadataStore metadata;
    private final Map<String, StorageEngine> openEngines = new ConcurrentHashMap<>();

    public StorageManager(MetadataStore metadata) {
        this.metadata = metadata;
    }

    public StorageEngine openTable(OpenTableRequest request) {
        String key = request.schema() + "." + request.tableName();
        return openEngines.computeIfAbsent(key, k -> {
            List<String> typeNames = request.columns().stream()
                .map(c -> c.type().getClass().getSimpleName())
                .toList();
            EngineConfig config = new EngineConfig(
                "./dsp/" + request.schema() + "/" + request.tableName(),
                1,
                request.columns().stream().map(ColumnInfo::type).toList(),
                Map.of()
            );
            return EngineRegistry.create("lucene", config);
        });
    }

    public void closeTable(String schema, String table) {
        String key = schema + "." + table;
        StorageEngine engine = openEngines.remove(key);
        if (engine != null) {
            engine.close();
        }
    }

    public MetadataStore getMetadata() {
        return metadata;
    }
}
```

- [ ] **Step 7: 编译验证**

```bash
cd /Users/liuzm/ai/dsp && mvn compile -pl dsp-storage-api -q
```

- [ ] **Step 8: 提交**

```bash
git add dsp-storage-api/src/main/java/com/qinyadan/system/dsp/storage/api/meta/ \
       dsp-storage-api/src/main/java/com/qinyadan/system/dsp/storage/api/service/
git commit -m "feat(api): add MetadataStore, StorageManager, TableInfo" \
  -m "Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Phase 3: 迁移 Lucene 存储实现

### Task 3.1: 创建 dsp-storage-lucene module

**Files:**
- Create: `dsp-storage-lucene/pom.xml`
- Create: `dsp-storage-lucene/src/main/java/com/qinyadan/system/dsp/storage/lucene/LuceneStorageEngine.java`
- Create: `dsp-storage-lucene/src/main/java/com/qinyadan/system/dsp/storage/lucene/LuceneDocumentMapper.java`

- [ ] **Step 1: 创建 pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <artifactId>dsp</artifactId>
        <groupId>com.qinyadan.system</groupId>
        <version>1.0-SNAPSHOT</version>
    </parent>

    <artifactId>dsp-storage-lucene</artifactId>

    <properties>
        <maven.compiler.source>8</maven.compiler.source>
        <maven.compiler.target>8</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <dependencies>
        <dependency>
            <groupId>com.qinyadan.system</groupId>
            <artifactId>dsp-core</artifactId>
            <version>1.0-SNAPSHOT</version>
        </dependency>
        <dependency>
            <groupId>com.qinyadan.system</groupId>
            <artifactId>dsp-storage-api</artifactId>
            <version>1.0-SNAPSHOT</version>
        </dependency>
        <dependency>
            <groupId>org.apache.lucene</groupId>
            <artifactId>lucene-core</artifactId>
            <version>${lucene.version}</version>
        </dependency>
        <dependency>
            <groupId>org.apache.lucene</groupId>
            <artifactId>lucene-queryparser</artifactId>
            <version>${lucene.version}</version>
        </dependency>
        <dependency>
            <groupId>org.apache.lucene</groupId>
            <artifactId>lucene-highlighter</artifactId>
            <version>${lucene.version}</version>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
        </dependency>
        <dependency>
            <groupId>commons-io</groupId>
            <artifactId>commons-io</artifactId>
            <version>${common-io.version}</version>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 2: 添加到父 POM**

在 `dsp/pom.xml` 的 `<modules>` 中添加 `<module>dsp-storage-lucene</module>`。

- [ ] **Step 3: 创建 LuceneDocumentMapper.java**

```java
package com.qinyadan.system.dsp.storage.lucene;

import com.qinyadan.system.dsp.core.data.Row;
import com.qinyadan.system.dsp.core.data.value.Value;
import com.qinyadan.system.dsp.core.data.type.DataType;
import com.qinyadan.system.dsp.storage.api.meta.ColumnInfo;
import org.apache.lucene.document.*;

import java.util.List;
import java.util.Map;

import static com.qinyadan.system.dsp.core.data.type.DataType.Precedence.*;

public final class LuceneDocumentMapper {

    private final List<ColumnInfo> columns;
    private final Map<String, DataType<?>> typeMap;

    public LuceneDocumentMapper(List<ColumnInfo> columns, Map<String, DataType<?>> typeMap) {
        this.columns = columns;
        this.typeMap = typeMap;
    }

    public Document toRow(Row row) {
        Document document = new Document();
        Value[] values = row.getColumns();

        for (int i = 0; i < values.length; i++) {
            Value value = values[i];
            ColumnInfo col = columns.get(i);
            String name = col.name();
            DataType<?> type = col.type();

            if (value.isNull()) {
                appendNullField(document, name, type);
            } else {
                appendNonNullField(document, name, type, value);
            }
        }
        return document;
    }

    private void appendNullField(Document doc, String name, DataType<?> type) {
        Precedence prec = type.precedence();
        if (prec == BYTE || prec == SHORT || prec == INTEGER) {
            doc.add(new IntPoint(name, Integer.MIN_VALUE));
            doc.add(new StoredField(name, Integer.MIN_VALUE));
        } else if (prec == LONG) {
            doc.add(new LongPoint(name, Long.MIN_VALUE));
            doc.add(new StoredField(name, Long.MIN_VALUE));
        } else if (prec == FLOAT) {
            doc.add(new FloatPoint(name, Float.MIN_VALUE));
            doc.add(new StoredField(name, Float.MIN_VALUE));
        } else if (prec == DOUBLE) {
            doc.add(new DoublePoint(name, Double.MIN_VALUE));
            doc.add(new StoredField(name, Double.MIN_VALUE));
        } else if (prec == DATE) {
            doc.add(new LongPoint(name, Long.MIN_VALUE));
            doc.add(new StoredField(name, Long.MIN_VALUE));
        } else {
            doc.add(new StringField(name, "NULL", Field.Store.YES));
        }
    }

    private void appendNonNullField(Document doc, String name, DataType<?> type, Value value) {
        Precedence prec = type.precedence();
        Object v = value.getValue();

        if (prec == BYTE || prec == SHORT || prec == INTEGER) {
            int content = ((Number) v).intValue();
            doc.add(new IntPoint(name, content));
            doc.add(new StoredField(name, content));
        } else if (prec == LONG) {
            long content = ((Number) v).longValue();
            doc.add(new LongPoint(name, content));
            doc.add(new StoredField(name, content));
        } else if (prec == FLOAT) {
            float content = ((Number) v).floatValue();
            doc.add(new FloatPoint(name, content));
            doc.add(new StoredField(name, content));
        } else if (prec == DOUBLE) {
            double content = ((Number) v).doubleValue();
            doc.add(new DoublePoint(name, content));
            doc.add(new StoredField(name, content));
        } else if (prec == DATE) {
            long content = ((Number) v).longValue();
            doc.add(new LongPoint(name, content));
            doc.add(new StoredField(name, content));
        } else {
            String content = v instanceof String ? (String) v : v.toString();
            doc.add(new StringField(name, content, Field.Store.YES));
        }
    }
}
```

- [ ] **Step 4: 重构 LuceneStorageEngine.java**

从 `base-storage/.../lucene/LuceneStorageEngine.java` 重构为新的 `StorageEngine` 接口：

```java
package com.qinyadan.system.dsp.storage.lucene;

import com.qinyadan.system.dsp.core.data.Row;
import com.qinyadan.system.dsp.core.data.value.Value;
import com.qinyadan.system.dsp.core.data.type.DataType;
import com.qinyadan.system.dsp.storage.api.StorageEngine;
import com.qinyadan.system.dsp.storage.api.WriteResult;
import com.qinyadan.system.dsp.storage.api.query.QueryContext;
import com.qinyadan.system.dsp.storage.api.meta.ColumnInfo;
import com.qinyadan.system.dsp.storage.api.iterator.ResultSetIterator;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.NIOFSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;

public class LuceneStorageEngine implements StorageEngine {

    private static final Logger LOG = LoggerFactory.getLogger(LuceneStorageEngine.class);

    private final String storagePath;
    private final List<ColumnInfo> columns;
    private final Map<String, DataType<?>> typeMap;
    private final LuceneDocumentMapper mapper;

    private IndexWriter indexWriter;
    private SearcherManager searcherManager;
    private volatile int uncommittedCount = 0;
    private volatile long lastFlushTime = System.currentTimeMillis();
    private boolean readOnly;

    public LuceneStorageEngine(String storagePath, List<ColumnInfo> columns, Map<String, DataType<?>> typeMap) {
        this.storagePath = storagePath;
        this.columns = columns;
        this.typeMap = typeMap;
        this.mapper = new LuceneDocumentMapper(columns, typeMap);
    }

    @Override
    public void init() {
        try {
            File dir = new File(storagePath);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            IndexWriterConfig conf = new IndexWriterConfig();
            indexWriter = new IndexWriter(new NIOFSDirectory(Paths.get(storagePath)), conf);
            searcherManager = new SearcherManager(indexWriter, new SearcherFactory());

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try { indexWriter.commit(); } catch (Exception e) { /* ignore */ }
            }));
        } catch (IOException e) {
            throw new RuntimeException("Failed to init LuceneStorageEngine at " + storagePath, e);
        }
    }

    @Override
    public WriteResult append(Row... rows) throws IOException {
        if (readOnly) {
            LOG.error("Storage engine is read only, can't insert");
            return new WriteResult(0, System.currentTimeMillis());
        }
        for (Row row : rows) {
            Document document = mapper.toRow(row);
            indexWriter.addDocument(document);
            uncommittedCount++;
        }
        return new WriteResult(rows.length, System.currentTimeMillis());
    }

    @Override
    public <R> Iterator<R> scan(QueryContext queryContext) throws IOException {
        IndexSearcher searcher = new IndexSearcher(searcherManager.acquire());
        Query query = new MatchAllDocsQuery();
        TopDocs topDocs = searcher.search(query, Integer.MAX_VALUE);

        List<R> result = new ArrayList<>();
        for (int i = 0; i < topDocs.scoreDocs.length; i++) {
            Document doc = indexReader.document(topDocs.scoreDocs[i].doc, queryContext.getColumnNames());
            result.add((R) documentToRow(doc));
        }
        searcherManager.release(searcher);
        return result.iterator();
    }

    private Row documentToRow(Document document) {
        List<Value> values = new ArrayList<>();
        for (ColumnInfo col : columns) {
            String name = col.name();
            DataType<?> type = col.type();
            values.add(fieldToValue(document, name, type));
        }
        return Row.of(values.toArray(new Value[0]));
    }

    private Value fieldToValue(Document doc, String name, DataType<?> type) {
        List<IndexableField> fields = doc.getFields(name);
        if (fields.isEmpty()) return Value.nullValue(type);

        IndexableField field = fields.get(0);
        Precedence prec = type.precedence();

        if (prec == BYTE || prec == SHORT || prec == INTEGER) {
            int v = ((IntegerFieldPoint) field).intValue();
            return new Value(v == Integer.MIN_VALUE ? null : v, type);
        } else if (prec == LONG) {
            long v = ((LongFieldPoint) field).longValue();
            return new Value(v == Long.MIN_VALUE ? null : v, type);
        } else if (prec == FLOAT) {
            float v = ((FloatFieldPoint) field).floatValue();
            return new Value(v == Float.MIN_VALUE ? null : v, type);
        } else if (prec == DOUBLE) {
            double v = ((DoubleFieldPoint) field).doubleValue();
            return new Value(v == Double.MIN_VALUE ? null : v, type);
        } else {
            String v = field.stringValue();
            return new Value("NULL".equals(v) ? null : v, type);
        }
    }

    @Override
    public long estimateRowCount() {
        try {
            return indexWriter.numDocs();
        } catch (IOException e) {
            return 0;
        }
    }

    @Override
    public void flush() {
        if (readOnly || uncommittedCount <= 0) return;
        try {
            DirectoryReader reader = DirectoryReader.open(indexWriter);
            indexReader = new SlothFilterDirectoryReader(reader,
                new SlothFilterDirectoryReader.SubReaderWrapper(1));
            searcherManager = new SearcherManager(indexWriter, new SearcherFactory());
            uncommittedCount = 0;
            lastFlushTime = System.currentTimeMillis();
        } catch (IOException e) {
            throw new RuntimeException("Flush failed", e);
        }
    }

    @Override
    public boolean shouldFlush() {
        return uncommittedCount > 0 || System.currentTimeMillis() - lastFlushTime > 1000;
    }

    @Override
    public void close() {
        try {
            if (indexWriter != null) indexWriter.close();
        } catch (IOException e) {
            LOG.error("Failed to close LuceneStorageEngine", e);
        }
    }

    @Override
    public boolean readOnly() { return readOnly; }

    @Override
    public void setReadOnly(boolean readOnly) { this.readOnly = readOnly; }
}
```

> 注意：上面的 `scan` 方法需要 `indexReader` 字段。完整实现需要保留 `SlothFilterDirectoryReader` 和 `ElasticsearchLeafReader` 类。将它们从 `base-storage` 原封不动复制到 `dsp-storage-lucene` 模块。

- [ ] **Step 5: 复制 slove-filter 辅助类**

从 `base-storage/.../lucene/` 复制：
- `SlothFilterDirectoryReader.java`
- `ElasticsearchLeafReader.java`

到 `dsp-storage-lucene/src/main/java/com/qinyadan/system/dsp/storage/lucene/`，只改 package。

- [ ] **Step 6: 注册引擎**

在 `dsp-storage-lucene` 中创建一个 `LuceneStorageEngineFactory.java`：

```java
package com.qinyadan.system.dsp.storage.lucene;

import com.qinyadan.system.dsp.storage.api.EngineConfig;
import com.qinyadan.system.dsp.storage.api.StorageEngine;
import com.qinyadan.system.dsp.storage.api.StorageEngineFactory;

public class LuceneStorageEngineFactory implements StorageEngineFactory {

    public static final LuceneStorageEngineFactory INSTANCE = new LuceneStorageEngineFactory();

    @Override
    public String type() {
        return "lucene";
    }

    @Override
    public StorageEngine create(EngineConfig config) {
        LuceneStorageEngine engine = new LuceneStorageEngine(
            config.storagePath(),
            config.columnTypes().stream()
                .map(t -> new com.qinyadan.system.dsp.storage.api.meta.ColumnInfo(
                    "col_" + t.id(), t, true, null, ""))
                .toList(),
            config.columnTypes().stream()
                .collect(java.util.stream.Collectors.toMap(
                    t -> "col_" + t.id(), t -> t))
        );
        engine.init();
        return engine;
    }
}
```

- [ ] **Step 7: 编译验证**

```bash
cd /Users/liuzm/ai/dsp && mvn compile -pl dsp-storage-lucene -am -q
```

- [ ] **Step 8: 提交**

```bash
git add dsp-storage-lucene/
git commit -m "feat(storage-lucene): add LuceneStorageEngine implementation" \
  -m "Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Phase 4: 创建 dsp-engine（执行引擎层）

### Task 4.1: 创建 dsp-engine module 骨架

**Files:**
- Create: `dsp-engine/pom.xml`

- [ ] **Step 1: 创建 pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <artifactId>dsp</artifactId>
        <groupId>com.qinyadan.system</groupId>
        <version>1.0-SNAPSHOT</version>
    </parent>

    <artifactId>dsp-engine</artifactId>

    <properties>
        <maven.compiler.source>8</maven.compiler.source>
        <maven.compiler.target>8</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <dependencies>
        <dependency>
            <groupId>com.qinyadan.system</groupId>
            <artifactId>dsp-core</artifactId>
            <version>1.0-SNAPSHOT</version>
        </dependency>
        <dependency>
            <groupId>com.qinyadan.system</groupId>
            <artifactId>dsp-storage-api</artifactId>
            <version>1.0-SNAPSHOT</version>
        </dependency>
        <dependency>
            <groupId>org.apache.calcite</groupId>
            <artifactId>calcite-core</artifactId>
        </dependency>
        <dependency>
            <groupId>org.apache.calcite</groupId>
            <artifactId>calcite-linq4j</artifactId>
        </dependency>
        <dependency>
            <groupId>com.google.guava</groupId>
            <artifactId>guava</artifactId>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
        </dependency>
        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-api</artifactId>
        </dependency>
        <dependency>
            <groupId>junit</groupId>
            <artifactId>junit</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 2: 添加到父 POM**

在 `dsp/pom.xml` 的 `<modules>` 中添加 `<module>dsp-engine</module>`。

- [ ] **Step 3: 编译验证**

```bash
cd /Users/liuzm/ai/dsp && mvn compile -pl dsp-engine -am -q
```

### Task 4.2: 迁移 Calcite 集成层

**Files:**
- Create: `dsp-engine/src/main/java/com/qinyadan/system/dsp/engine/calcite/SlothParser.java`
- Create: `dsp-engine/src/main/java/com/qinyadan/system/dsp/engine/calcite/SlothSchema.java`
- Create: `dsp-engine/src/main/java/com/qinyadan/system/dsp/engine/calcite/SlothTable.java`
- Create: `dsp-engine/src/main/java/com/qinyadan/system/dsp/engine/calcite/SlothConvention.java`
- Create: `dsp-engine/src/main/java/com/qinyadan/system/dsp/engine/calcite/ParserFactory.java`
- Create: `dsp-engine/src/main/java/com/qinyadan/system/dsp/engine/calcite/SlothSchemaHolder.java`
- Create: `dsp-engine/src/main/java/com/qinyadan/system/dsp/engine/calcite/SlothColumn.java`
- Create: `dsp-engine/src/main/java/com/qinyadan/system/dsp/engine/calcite/EnhanceSlothColumn.java`
- Create: `dsp-engine/src/main/java/com/qinyadan/system/dsp/engine/calcite/SlothColumnType.java`
- Create: `dsp-engine/src/main/java/com/qinyadan/system/dsp/engine/calcite/SlothColumnType.java`
- Create: `dsp-engine/src/main/java/com/qinyadan/system/dsp/engine/calcite/EnvironmentValues.java`
- Create: `dsp-engine/src/main/java/com/qinyadan/system/dsp/engine/calcite/SlothEnvironmentValueHolder.java`
- Create: `dsp-engine/src/main/java/com/qinyadan/system/dsp/engine/calcite/PhysicalJoinType.java`
- Create: `dsp-engine/src/main/java/com/qinyadan/system/dsp/engine/calcite/InnerTables.java`

- [ ] **Step 1: 从 base-storage/parser 迁移所有文件**

将 `base-storage/.../parser/` 下的所有 Java 文件复制到 `dsp-engine/.../calcite/`，只修改 package 为 `com.qinyadan.system.dsp.engine.calcite`。

需要迁移的文件清单：
- `SlothParser.java` — 保持内容不变
- `SlothSchema.java` — 保持内容不变
- `SlothTable.java` — 保持内容不变
- `SlothTableEngine.java` — 需要修改：将 `SlothTableEngine` 拆分为 schema 层的 `SlothTableInfo` 和 engine 层的 `TableEngine`，详见下方
- `SlothConvention.java` — 保持内容不变
- `SlothColumnType.java` — 保持内容不变
- `SlothColumn.java` — 保持内容不变
- `EnhanceSlothColumn.java` — 保持内容不变
- `ParserFactory.java` — 保持内容不变
- `SlothSchemaHolder.java` — 保持内容不变
- `EnvironmentValues.java` — 保持内容不变
- `SlothEnvironmentValueHolder.java` — 保持内容不变
- `PhysicalJoinType.java` — 保持内容不变
- `InnerTables.java` — 保持内容不变
- `package-info.java` — 保持内容不变

- [ ] **Step 2: 重构 SlothTableEngine**

`SlothTableEngine` 当前承担了两个职责：① 表级元数据管理 ② 存储分片管理。拆分如下：

创建 `dsp-engine/.../calcite/SlothTableInfo.java`（替代原 `SlothTableEngine` 的元数据部分）：

```java
package com.qinyadan.system.dsp.engine.calcite;

import com.qinyadan.system.dsp.core.data.type.DataType;
import com.qinyadan.system.dsp.core.data.type.TypeConversionUtils;
import com.qinyadan.system.dsp.storage.api.meta.ColumnInfo;
import org.apache.calcite.sql.type.SqlTypeName;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SlothTableInfo {
    private final String tableName;
    private final String schemaName;
    private final List<SlothColumn> columns;
    private final String engineName;
    private final String tableComment;
    private final int shardNum;

    public Map<String, DataType<?>> getColumnTypes() {
        return columns.stream().collect(Collectors.toMap(
            SlothColumn::getColumnName,
            col -> TypeConversionUtils.getBySqlTypeName(col.getColumnType().getColumnType())
        ));
    }

    public List<String> getColumnNames() {
        return columns.stream().map(SlothColumn::getColumnName).collect(Collectors.toList());
    }

    // getters...
}
```

- [ ] **Step 3: 编译验证**

```bash
cd /Users/liuzm/ai/dsp && mvn compile -pl dsp-engine -am -q
```

- [ ] **Step 4: 提交**

```bash
git add dsp-engine/src/main/java/com/qinyadan/system/dsp/engine/calcite/
git commit -m "feat(engine): migrate Calcite integration layer" \
  -m "Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

### Task 4.3: 迁移 RelNode 和 Operator

**Files:**
- Create: `dsp-engine/src/main/java/com/qinyadan/system/dsp/engine/rel/SlothRel.java`
- Create: `dsp-engine/src/main/java/com/qinyadan/system/dsp/engine/rel/SlothTableScan.java`
- Create: `dsp-engine/src/main/java/com/qinyadan/system/dsp/engine/rel/SlothFilter.java`
- Create: `dsp-engine/src/main/java/com/qinyadan/system/dsp/engine/rel/SlothProject.java`
- Create: `dsp-engine/src/main/java/com/qinyadan/system/dsp/engine/rel/SlothJoin.java`
- Create: `dsp-engine/src/main/java/com/qinyadan/system/dsp/engine/rel/SlothHashJoin.java`
- Create: `dsp-engine/src/main/java/com/qinyadan/system/dsp/engine/rel/SlothSort.java`
- Create: `dsp-engine/src/main/java/com/qinyadan/system/dsp/engine/rel/SlothAggregate.java`
- Create: `dsp-engine/src/main/java/com/qinyadan/system/dsp/engine/rel/SlothUnion.java`
- Create: `dsp-engine/src/main/java/com/qinyadan/system/dsp/engine/rel/SlothValues.java`
- Create: `dsp-engine/src/main/java/com/qinyadan/system/dsp/engine/execution/Operator.java`
- Create: `dsp-engine/src/main/java/com/qinyadan/system/dsp/engine/execution/AbstractOperator.java`
- Create: `dsp-engine/src/main/java/com/qinyadan/system/dsp/engine/execution/TableScanOperator.java`
- Create: `dsp-engine/src/main/java/com/qinyadan/system/dsp/engine/execution/FilterOperator.java`
- Create: `dsp-engine/src/main/java/com/qinyadan/system/dsp/engine/execution/ProjectOperator.java`
- Create: `dsp-engine/src/main/java/com/qinyadan/system/dsp/engine/execution/JoinOperator.java`
- Create: `dsp-engine/src/main/java/com/qinyadan/system/dsp/engine/execution/SortOperator.java`
- Create: `dsp-engine/src/main/java/com/qinyadan/system/dsp/engine/execution/AggregateOperator.java`
- Create: `dsp-engine/src/main/java/com/qinyadan/system/dsp/engine/execution/UnionOperator.java`
- Create: `dsp-engine/src/main/java/com/qinyadan/system/dsp/engine/execution/ValueOperator.java`

- [ ] **Step 1: 迁移 RelNode 实现**

从 `base-storage/.../parser/rel/` 复制所有 `Sloth*.java` 文件到 `dsp-engine/.../rel/`，修改 package 为 `com.qinyadan.system.dsp.engine.rel`。

保持 `implement()` 方法不变，但返回类型改为 `com.qinyadan.system.dsp.engine.execution.Operator`。

- [ ] **Step 2: 迁移 Operator 实现**

从 `base-storage/.../parser/rel/operator/` 复制所有文件到 `dsp-engine/.../execution/`，修改 package 为 `com.qinyadan.system.dsp.engine.execution`。

保持内容不变（接口和方法签名不变）。

- [ ] **Step 3: 编译验证**

```bash
cd /Users/liuzm/ai/dsp && mvn compile -pl dsp-engine -am -q
```

- [ ] **Step 4: 提交**

```bash
git add dsp-engine/src/main/java/com/qinyadan/system/dsp/engine/rel/ \
       dsp-engine/src/main/java/com/qinyadan/system/dsp/engine/execution/
git commit -m "feat(engine): migrate RelNode and Operator implementations" \
  -m "Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

### Task 4.4: 迁移优化规则

**Files:**
- Create: `dsp-engine/src/main/java/com/qinyadan/system/dsp/engine/optimizer/SlothRules.java`
- Create: `dsp-engine/src/main/java/com/qinyadan/system/dsp/engine/optimizer/converter/` (所有 converter rules)
- Create: `dsp-engine/src/main/java/com/qinyadan/system/dsp/engine/optimizer/cbo/` (CBO rules)
- Create: `dsp-engine/src/main/java/com/qinyadan/system/dsp/engine/optimizer/rbo/` (RBO rules)

- [ ] **Step 1: 迁移规则文件**

从 `base-storage/.../parser/rules/` 复制所有文件到 `dsp-engine/.../optimizer/`，修改 package。

- [ ] **Step 2: 编译验证**

```bash
cd /Users/liuzm/ai/dsp && mvn compile -pl dsp-engine -am -q
```

- [ ] **Step 3: 提交**

```bash
git add dsp-engine/src/main/java/com/qinyadan/system/dsp/engine/optimizer/
git commit -m "feat(engine): migrate optimizer rules" \
  -m "Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

### Task 4.5: 迁移表达式和函数体系

**Files:**
- Create: `dsp-engine/src/main/java/com/qinyadan/system/dsp/engine/expr/Symbol.java`
- Create: `dsp-engine/src/main/java/com/qinyadan/system/dsp/engine/expr/SymbolType.java`
- Create: `dsp-engine/src/main/java/com/qinyadan/system/dsp/engine/expr/ColumnReference.java`
- Create: `dsp-engine/src/main/java/com/qinyadan/system/dsp/engine/expr/Function.java`
- Create: `dsp-engine/src/main/java/com/qinyadan/system/dsp/engine/expr/InputColumn.java`
- Create: `dsp-engine/src/main/java/com/qinyadan/system/dsp/engine/expr/Literal.java`
- Create: `dsp-engine/src/main/java/com/qinyadan/system/dsp/engine/expr/FuncArg.java`
- Create: `dsp-engine/src/main/java/com/qinyadan/system/dsp/engine/func/Scalar.java`
- Create: `dsp-engine/src/main/java/com/qinyadan/system/dsp/engine/func/ArithmeticFunction.java`
- Create: `dsp-engine/src/main/java/com/qinyadan/system/dsp/engine/func/CompareFunction.java`
- Create: `dsp-engine/src/main/java/com/qinyadan/system/dsp/engine/func/LogicalFunction.java`
- Create: `dsp-engine/src/main/java/com/qinyadan/system/dsp/engine/func/AbsFunction.java`
- Create: `dsp-engine/src/main/java/com/qinyadan/system/dsp/engine/func/UnaryMinusFunction.java`
- Create: `dsp-engine/src/main/java/com/qinyadan/system/dsp/engine/func/CastFunction.java`
- Create: `dsp-engine/src/main/java/com/qinyadan/system/dsp/engine/func/Functions.java`
- Create: `dsp-engine/src/main/java/com/qinyadan/system/dsp/engine/func/agg/` (aggregations)
- Create: `dsp-engine/src/main/java/com/qinyadan/system/dsp/engine/func/nil/` (IsNull/IsNotNull)

- [ ] **Step 1: 从 runtime 迁移 expr/func 体系**

从 `dsp-runtime/.../engine/data/expr/` 复制到 `dsp-engine/.../expr/`
从 `dsp-runtime/.../engine/data/func/` 复制到 `dsp-engine/.../func/`

修改 package 声明，其余保持不变。

- [ ] **Step 2: 编译验证**

```bash
cd /Users/liuzm/ai/dsp && mvn compile -pl dsp-engine -am -q
```

- [ ] **Step 3: 提交**

```bash
git add dsp-engine/src/main/java/com/qinyadan/system/dsp/engine/expr/ \
       dsp-engine/src/main/java/com/qinyadan/system/dsp/engine/func/
git commit -m "feat(engine): migrate expression and function体系" \
  -m "Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Phase 5: 更新 protocol 依赖

### Task 5.1: 更新 dsp-protocol 依赖声明

**Files:**
- Modify: `dsp-protocol/pom.xml`

- [ ] **Step 1: 替换依赖**

将 `dsp-protocol/pom.xml` 中的以下依赖：
- `dsp-runtime` → 替换为 `dsp-core` + `dsp-engine`
- `base-storage` → 替换为 `dsp-storage-api`
- `lucene-storage` → 替换为 `dsp-storage-lucene`

具体修改：

```xml
<!-- 删除这些依赖 -->
<dependency>
    <groupId>com.qinyadan.system</groupId>
    <artifactId>dsp-runtime</artifactId>
</dependency>
<dependency>
    <groupId>com.qinyadan.system</groupId>
    <artifactId>base-storage</artifactId>
</dependency>
<dependency>
    <groupId>com.qinyadan.system</groupId>
    <artifactId>lucene-storage</artifactId>
</dependency>

<!-- 添加这些依赖 -->
<dependency>
    <groupId>com.qinyadan.system</groupId>
    <artifactId>dsp-core</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
<dependency>
    <groupId>com.qinyadan.system</groupId>
    <artifactId>dsp-engine</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
<dependency>
    <groupId>com.qinyadan.system</groupId>
    <artifactId>dsp-storage-api</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
<dependency>
    <groupId>com.qinyadan.system</groupId>
    <artifactId>dsp-storage-lucene</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

- [ ] **Step 2: 更新 import 语句**

在所有 `dsp-protocol` 的 Java 文件中，将 import 从旧包名替换为新包名：

| 旧 import | 新 import |
|-----------|-----------|
| `com.qinyadan.system.dsp.runtime.*` | `com.qinyadan.system.dsp.core.*` |
| `com.qinyadan.system.dsp.storage.parser.*` | `com.qinyadan.system.dsp.engine.calcite.*` |
| `com.qinyadan.system.dsp.storage.parser.rel.*` | `com.qinyadan.system.dsp.engine.rel.*` |
| `com.qinyadan.system.dsp.storage.parser.rel.operator.*` | `com.qinyadan.system.dsp.engine.execution.*` |
| `com.qinyadan.system.dsp.storage.parser.rules.*` | `com.qinyadan.system.dsp.engine.optimizer.*` |
| `com.qinyadan.system.dsp.storage.*` | `com.qinyadan.system.dsp.storage.api.*` |

使用 sed 批量替换：

```bash
cd /Users/liuzm/ai/dsp/dsp-protocol
find src/main/java -name "*.java" -exec sed -i '' \
  's/com\.qinyadan\.system\.dsp\.runtime\./com.qinyadan.system.dsp.core./g' {} +
find src/main/java -name "*.java" -exec sed -i '' \
  's/com\.qinyadan\.system\.dsp\.storage\.parser\./com.qinyadan.system.dsp.engine.calcite./g' {} +
find src/main/java -name "*.java" -exec sed -i '' \
  's/com\.qinyadan\.system\.dsp\.storage\.parser\.rel\./com.qinyadan.system.dsp.engine.rel./g' {} +
find src/main/java -name "*.java" -exec sed -i '' \
  's/com\.qinyadan\.system\.dsp\.storage\.parser\.rel\.operator\./com.qinyadan.system.dsp.engine.execution./g' {} +
find src/main/java -name "*.java" -exec sed -i '' \
  's/com\.qinyadan\.system\.dsp\.storage\.parser\.rules\./com.qinyadan.system.dsp.engine.optimizer./g' {} +
find src/main/java -name "*.java" -exec sed -i '' \
  's/com\.qinyadan\.system\.dsp\.storage\./com.qinyadan.system.dsp.storage.api./g' {} +
```

- [ ] **Step 3: 编译验证**

```bash
cd /Users/liuzm/ai/dsp && mvn compile -pl dsp-protocol -am -q
```

- [ ] **Step 4: 提交**

```bash
git add dsp-protocol/pom.xml dsp-protocol/src/
git commit -m "refactor(protocol): update dependencies to new module structure" \
  -m "Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Phase 6: 清理旧模块

### Task 6.1: 停用 dsp-runtime

**Files:**
- Modify: `dsp/pom.xml`（移除 dsp-runtime module）
- Modify: `dsp-runtime/pom.xml`（改为仅依赖 dsp-core 的 stub）

- [ ] **Step 1: 将 dsp-runtime 降级为 stub**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project>
    <parent>
        <artifactId>dsp</artifactId>
        <groupId>com.qinyadan.system</groupId>
        <version>1.0-SNAPSHOT</version>
    </parent>
    <artifactId>dsp-runtime</artifactId>
    <dependencies>
        <dependency>
            <groupId>com.qinyadan.system</groupId>
            <artifactId>dsp-core</artifactId>
            <version>1.0-SNAPSHOT</version>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 2: 从父 POM 移除 dsp-runtime**

```bash
sed -i '' '/<module>dsp-runtime<\/module>/d' /Users/liuzm/ai/dsp/pom.xml
```

- [ ] **Step 3: 编译验证**

```bash
cd /Users/liuzm/ai/dsp && mvn compile -q
```

- [ ] **Step 4: 提交**

```bash
git add pom.xml dsp-runtime/
git commit -m "chore: retire dsp-runtime, replace with dsp-core dependency" \
  -m "Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

### Task 6.2: 停用 base-storage

**Files:**
- Modify: `dsp/pom.xml`（移除 base-storage module）
- Modify: `dsp-storage/base-storage/pom.xml`（改为 stub）

- [ ] **Step 1: 将 base-storage 降级为 stub**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project>
    <parent>
        <artifactId>dsp-storage</artifactId>
        <groupId>com.qinyadan.system</groupId>
        <version>1.0-SNAPSHOT</version>
    </parent>
    <artifactId>base-storage</artifactId>
    <dependencies>
        <dependency>
            <groupId>com.qinyadan.system</groupId>
            <artifactId>dsp-core</artifactId>
            <version>1.0-SNAPSHOT</version>
        </dependency>
        <dependency>
            <groupId>com.qinyadan.system</groupId>
            <artifactId>dsp-storage-api</artifactId>
            <version>1.0-SNAPSHOT</version>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 2: 从父 POM 移除 base-storage**

```bash
sed -i '' '/<module>dsp-storage\/base-storage<\/module>/d' /Users/liuzm/ai/dsp/pom.xml
```

- [ ] **Step 3: 编译验证**

```bash
cd /Users/liuzm/ai/dsp && mvn compile -q
```

- [ ] **Step 4: 提交**

```bash
git add pom.xml dsp-storage/base-storage/
git commit -m "chore: retire base-storage, replace with dsp-storage-api" \
  -m "Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

### Task 6.3: 清理 dsp-storage 父 module

**Files:**
- Modify: `dsp/pom.xml`

- [ ] **Step 1: 移除旧 storage modules**

从 `dsp/pom.xml` 的 `<modules>` 中移除：
- `dsp-storage/mysql-storage`
- `dsp-storage/rocksdb-storage`
- `dsp-storage/lucene-storage`

保留 `dsp-storage` 父 module 作为未来存储实现的容器。

- [ ] **Step 2: 编译验证**

```bash
cd /Users/liuzm/ai/dsp && mvn compile -q
```

- [ ] **Step 3: 提交**

```bash
git add pom.xml dsp-storage/
git commit -m "chore: clean up dsp-storage module structure" \
  -m "Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Phase 7: 全量验证

### Task 7.1: 全项目编译

- [ ] **Step 1: 全量编译**

```bash
cd /Users/liuzm/ai/dsp && mvn clean compile -q
```

Expected: BUILD SUCCESS

- [ ] **Step 2: 运行所有测试**

```bash
cd /Users/liuzm/ai/dsp && mvn test -q
```

记录哪些测试通过、哪些失败。

- [ ] **Step 3: 提交**

```bash
git add .
git commit -m "chore: full compilation and test verification" \
  -m "Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

### Task 7.2: 验证依赖图

- [ ] **Step 1: 检查循环依赖**

```bash
cd /Users/liuzm/ai/dsp && mvn dependency:tree -Dverbose | grep -E "(compile|provided|runtime)" | sort | uniq -c | sort -rn
```

- [ ] **Step 2: 确认模块依赖关系**

```bash
for mod in dsp-core dsp-storage-api dsp-storage-lucene dsp-engine dsp-protocol dsp-cluster; do
  echo "=== $mod ==="
  grep -A 30 '<dependencies>' $mod/pom.xml | grep '<artifactId>' | head -10
done
```

Expected 依赖链：
```
dsp-core → (无依赖)
dsp-storage-api → dsp-core
dsp-storage-lucene → dsp-core, dsp-storage-api
dsp-engine → dsp-core, dsp-storage-api
dsp-protocol → dsp-core, dsp-engine, dsp-storage-api, dsp-storage-lucene
dsp-cluster → dsp-core
```

- [ ] **Step 3: 提交**

```bash
git add .
git commit -m "chore: verify dependency graph is correct" \
  -m "Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## 执行说明

本 plan 共 22 个 Task，分为 7 个 Phase。

**执行顺序**：Phase 0 → Phase 1 → Phase 2 → Phase 3 → Phase 4 → Phase 5 → Phase 6 → Phase 7

**每个 Task 完成后必须执行 `mvn compile` 验证编译通过，然后立即 commit。**

**每个 Task 完成后等待人工 review 再进入下一个 Task。**

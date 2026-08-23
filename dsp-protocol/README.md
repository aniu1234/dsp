### 1. 简介

本项目主要实现一个Java版本的'MySQL', 即支持MySQL协议， 以mysql-client、jdbc等形式访问数据库，目前进度如下:

- 简单实现在MySQL连接协议
- 支持建表、建db、show tables
- 支持查询
- 支持查看逻辑执行计划
- 简单实现了RBO与CBO
- 实现了物理执行计划Operator(MPP)

当前版本是开发者预览，完整产品能力、限制和路线图见
[产品说明](../docs/product.md)。目前仍在进行服务边界、存储优化与列类型扩展。

### 2.如何启动该数据库

#### 2.1. 元数据存储

默认使用 `DSP_DATA_DIR/_meta/catalog-v1.json` 持久化库表元数据，
与 Lucene 表数据一起在重启后自动恢复，不需要额外部署元数据库。

也可选择使用 MySQL 存放元数据：

```bash
export DSP_META_JDBC_URL='jdbc:mysql://127.0.0.1:3306/sloth'
export DSP_META_JDBC_USERNAME='sloth'
export DSP_META_JDBC_PASSWORD='change-me'
```

外部 MySQL 配置成功时优先使用 MySQL；未配置时使用内置 catalog。

#### 2.2. 编译项目

```
mvn clean package
```

#### 2.3. 启动

##### 2.3.1 本地调试

启动前必须配置登录账号密码；未配置时服务会拒绝所有登录：

```bash
export DSP_AUTH_USERNAME='root'
export DSP_AUTH_PASSWORD='change-me'
export DSP_DATA_DIR="$HOME/.dsp/data"
# 可选，默认 3016
export DSP_SERVER_PORT='3016'
```

可选的查询和存储资源边界：

```bash
# 默认 30 秒，设为 0 可关闭超时
export DSP_QUERY_TIMEOUT_MS='30000'
# 单次查询最多返回行数
export DSP_QUERY_MAX_RESULT_ROWS='100000'
# Sort、JOIN 构建侧、聚合分组/去重状态、UNION DISTINCT 的最大内存条目数
export DSP_QUERY_MAX_MATERIALIZED_ROWS='100000'
# Lucene 每次扫描页大小
export DSP_STORAGE_SCAN_PAGE_SIZE='512'
# 默认 true；返回 INSERT 成功前提交到 Lucene
export DSP_STORAGE_SYNC_WRITES='true'
# 单条 INSERT 最大行数
export DSP_WRITE_MAX_ROWS_PER_INSERT='10000'
```

在IDE中找到`FrontEndMain`, 直接启动main函数即可

##### 2.3.2 服务部署

打包项目后，解压 `dsp-protocol-1.0-SNAPSHOT-RELEASE.tar.gz`，执行：

- `bin/dsp.sh start` 启动
- `bin/dsp.sh stop` 停止

### 3.连接

#### 3.1 连接地址

```bash
mysql -h127.0.0.1 -uroot -p -P3016 --ssl-mode=disabled
```

当前服务器不宣告事务能力，`BEGIN`/`COMMIT`/`ROLLBACK` 会返回明确的不支持错误。

#### 3.2 使用范例


 

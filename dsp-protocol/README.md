### 1. 简介

本项目主要实现一个Java版本的'MySQL', 即支持MySQL协议， 以mysql-client、jdbc等形式访问数据库，目前进度如下:

- 简单实现在MySQL连接协议
- 支持建表、建db、show tables
- 支持查询
- 支持查看逻辑执行计划
- 简单实现了RBO与CBO
- 实现了物理执行计划Operator(MPP)

项目地址: [待定](https://www.baidu.com),
欢迎小伙伴们一起参加

目前仍在进行分布式能力、存储优化与列类型扩展。

### 2.如何启动该数据库

#### 2.1. 准备元数据数据库

本项目可以使用 MySQL 存放元数据，通过环境变量配置：

```bash
export DSP_META_JDBC_URL='jdbc:mysql://127.0.0.1:3306/sloth'
export DSP_META_JDBC_USERNAME='sloth'
export DSP_META_JDBC_PASSWORD='change-me'
```

**说明:**
**如果没有配置MySQL用来存储元数据, 所有的db、table数据均存放在内存之中，无法
在重启后恢复**

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

#### 3.2 使用范例


 

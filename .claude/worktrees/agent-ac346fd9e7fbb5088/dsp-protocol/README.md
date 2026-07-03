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

目前正在进行中

- 数据持久化(表数据持久化已完成, 表元数据todo中)与存储优化
- 支持插入语法与功能(done)
- 列类型丰富

### 2.如何启动该数据库

#### 2.1. 准备元数据数据库

本项目采用MySQL存放元数据, 建表语句如下:

启动数据库，并替换MySQL相应的配置, 请参考`MysqlConnection`

**说明:**
**如果没有配置MySQL用来存储元数据, 所有的db、table数据均存放在内存之中，无法
在重启后恢复**

#### 2.2. 编译项目

```
mvn clean package
```

#### 2.3. 启动

##### 2.3.1 本地调试

在IDE中找到`FrontEndMain`, 直接启动main函数即可

##### 2.3.2 服务部署

打包项目后， 解压mysql-protocol-1.0-SNAPSHOT-RELEASE.tar.gz, 执行:

- `bin/dsp.sh start` 启动
- `bin/dsp.sh stop` 停止

### 3.连接

#### 3.1 连接地址

```sql
 mysql
-h127.0.0.1 -uroot -psxx -P3016
```

目前没有对用户名与密码进行验证，任何用户名与密码登陆都没有问题

#### 3.2 使用范例


 


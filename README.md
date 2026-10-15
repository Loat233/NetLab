# NetLab 网络实验室设备预约与借还管理平台

NetLab 是一个基于 Spring Boot、Spring Security、MyBatis 和 Thymeleaf 开发的实验室设备管理系统，为学生提供设备查询、预约、借用、归还和故障上报功能，为管理员提供预约审核、借出归还确认、设备维护和故障处理功能。

## 核心功能

### 普通用户

- 邮箱验证码登录
- Remember Me 自动登录
- 查询设备及设备详情
- 按时间段提交设备预约
- 查看和取消自己的预约
- 申请归还设备
- 上报设备故障
- 查看个人预约与故障记录

### 管理员

- 添加、编辑设备
- 审核或拒绝预约申请
- 确认设备借出
- 确认设备归还
- 查看逾期记录
- 登记和处理设备故障
- 查看系统统计数据

## 核心业务设计

### 预约冲突检测

创建和审核预约时检查同一设备的有效预约时间段是否重叠：

```text
newStart < existingEnd
&&
newEnd > existingStart
```

防止同一设备在相同时间段内被重复预约。

### 并发控制

预约审核、设备借出和确认归还操作使用 Spring 事务与 MySQL 行锁。

通过 `SELECT ... FOR UPDATE` 锁定设备、预约和借用记录，并结合带状态条件的更新语句，避免多个管理员同时处理同一业务造成重复借出或状态覆盖。

### 状态流转

预约状态：

```text
PENDING → APPROVED → BORROWED → RETURN_PENDING → COMPLETED
    ├──→ REJECTED
    └──→ CANCELLED
```

设备状态：

```text
AVAILABLE → BORROWED → AVAILABLE
BORROWED → DAMAGED → MAINTENANCE → AVAILABLE
```

Service 层负责校验状态是否允许流转，Controller 只负责接收请求和页面跳转。

### 逾期处理

借用记录保存应归还时间和申请归还时间。

用户申请归还时，根据以下条件判断是否逾期：

```text
requestedAt > dueAt
```

系统还通过 Spring Scheduled 定时扫描超过应归还时间但尚未申请归还的记录。

### 权限控制

系统通过 Spring Security 区分普通用户和管理员：

- `USER`：预约、取消、申请归还、故障上报
- `ADMIN`：预约审核、确认借出与归还、设备和故障管理

`/admin/**` 路径只允许管理员访问。

## 技术栈

- Java 17
- Spring Boot 4.1.1
- Spring MVC
- Spring Security
- MyBatis
- MySQL 8
- Thymeleaf
- Layui / Pear Admin
- Maven

## 项目结构

```text
src/main/java/org/lostwind/netlab
├── configuration    安全和 MVC 配置
├── controller       请求处理和页面跳转
├── service          业务规则、事务和状态校验
├── mapper           MyBatis 数据访问
├── entity           数据库实体
├── dto              请求数据对象
├── enums            业务状态枚举
├── filter           登录验证码过滤器
├── component        逾期定时任务
└── util             编号生成工具

src/main/resources
├── static           CSS、JavaScript、图片等静态资源
├── templates        Thymeleaf 页面
└── application.yaml 本地配置文件，不提交到仓库
```

## 数据库

项目主要包含以下数据表：

- `account`：用户账号
- `laboratory`：实验室
- `device_category`：设备分类
- `device`：设备信息
- `reservation`：预约记录
- `borrow_record`：借用记录
- `device_fault_record`：设备故障记录
- `review`：用户留言
- `persistent_logins`：Remember Me 登录令牌

初始化脚本位于：

```text
netlab_demo.sql
```

该脚本会创建 `netlab_db` 数据库、相关数据表及答辩展示数据。

## 本地运行

### 1. 环境要求

- JDK 17
- MySQL 8
- Maven 3.9 或更高版本

### 2. 初始化数据库

在 MySQL 中执行：

```text
netlab_demo.sql
```

### 3. 创建本地配置

复制配置示例：

```powershell
Copy-Item src/main/resources/application-example.yaml `
          src/main/resources/application.yaml
```

然后填写本机数据库账号、密码和邮箱 SMTP 授权码。

### 4. 启动项目

```bash
mvn spring-boot:run
```

浏览器访问：

```text
http://localhost:8080/login
```

## 登录说明

系统使用邮箱地址作为用户名，并在登录时发送邮箱验证码。

用于登录的账号必须填写能够实际接收验证码的邮箱。公开仓库中不要提交个人邮箱密码或 SMTP 授权码。

## 后续计划

- 补充预约冲突和状态流转测试
- 补充 Spring Security 权限测试
- 增加统一异常处理
- 完善操作日志
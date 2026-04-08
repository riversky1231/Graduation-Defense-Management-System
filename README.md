<a id="english"></a>

# Graduation Defense Management System

[Chinese](#chinese)

Graduation Defense Management System (Spring Boot + MyBatis + Thymeleaf).  
It supports end-to-end thesis/project defense workflows for department admins, teachers, and students, including student import, group assignment, scoring, comments, material upload, and export.

## Tech Stack

- Java 17
- Spring Boot 2.7.5
- Spring Security
- MyBatis
- Thymeleaf
- MySQL 8.x
- Redis (session persistence in production)
- SpringDoc OpenAPI (development environment)

## Core Features

- Authentication and role-based access control (super admin / department admin / teacher / student)
- Department and user management (with Excel import)
- Student data management and advisor preference allocation
- Defense group management and member assignment
- Scoring workflow (group scoring / large-group scoring)
- Comment and score summary
- Material upload and template export (Word/PDF related processing)

## Requirements

- JDK 17+
- Maven 3.8+
- MySQL 8.x (recommended charset: `utf8mb4`)
- Redis (required in production profile)

## Quick Start (Development)

1. Create database and import seed data:
```sql
SOURCE src/main/resources/data.sql;
```

2. Configure database connection (supports environment variable overrides):
- `DB_URL` (default: `jdbc:mysql://localhost:3306/defense_management?...`)
- `DB_USERNAME` (default: `root`)
- `DB_PASSWORD` (default: `root`)

3. Start the application:
```bash
mvn spring-boot:run
```

4. Access endpoints:
- Home/Login: `http://localhost:8080/`
- Health check: `http://localhost:8080/actuator/health`
- Swagger UI (dev): `http://localhost:8080/swagger-ui/index.html`

## Default Account Notes

- A seeded super admin account exists in `data.sql`: `admin`
- In development, the initial privileged password is controlled by `INITIAL_PRIVILEGED_PASSWORD`, defaulting to `123456`
- Change passwords immediately after first login; set production passwords explicitly via environment variables

## Production Notes

- Run with `prod` profile:
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```
- In `prod`, Swagger is disabled by default, session storage switches to Redis, and logging is more conservative

## Test

```bash
mvn test
```

## Docs

- [Database Schema Description](./数据库表结构说明.md)
- [Excel Import Format Guide](./Excel导入格式说明.md)

---

<a id="chinese"></a>

# 毕业答辩管理系统

[Back to English](#english)

毕业答辩管理系统（Spring Boot + MyBatis + Thymeleaf），用于院系管理员、教师、学生的毕业设计/论文答辩流程管理，包括学生导入、分组分配、评分、评语、材料上传与导出等功能。

## 技术栈

- Java 17
- Spring Boot 2.7.5
- Spring Security
- MyBatis
- Thymeleaf
- MySQL 8.x
- Redis（生产环境 Session 持久化）
- SpringDoc OpenAPI（开发环境）

## 主要功能

- 登录鉴权与权限分层（超级管理员 / 院系管理员 / 教师 / 学生）
- 院系与用户管理（支持 Excel 导入）
- 学生信息管理与导师志愿分配
- 答辩小组管理与成员编排
- 评分流程（小组评分 / 大组评分）
- 评语与成绩汇总
- 材料上传与模板导出（含 Word/PDF 处理）

## 运行环境

- JDK 17+
- Maven 3.8+
- MySQL 8.x（推荐字符集 `utf8mb4`）
- Redis（仅生产环境必需）

## 快速启动（开发环境）

1. 创建数据库并导入初始化数据：
```sql
SOURCE src/main/resources/data.sql;
```

2. 配置数据库连接（支持环境变量覆盖）：
- `DB_URL`（默认：`jdbc:mysql://localhost:3306/defense_management?...`）
- `DB_USERNAME`（默认：`root`）
- `DB_PASSWORD`（默认：`root`）

3. 启动项目：
```bash
mvn spring-boot:run
```

4. 访问系统：
- 应用首页/登录页：`http://localhost:8080/`
- 健康检查：`http://localhost:8080/actuator/health`
- Swagger UI（开发环境）：`http://localhost:8080/swagger-ui/index.html`

## 默认账号说明

- `data.sql` 中预置了超级管理员账号：`admin`
- 开发环境下初始高权限密码默认来自配置 `INITIAL_PRIVILEGED_PASSWORD`，默认值为 `123456`
- 建议首次登录后立即修改密码；生产环境请通过环境变量显式设置密码

## 生产环境说明

- 启动时建议使用 `prod` 配置：
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```
- `prod` 配置下默认关闭 Swagger，Session 存储切换为 Redis，日志级别更保守

## 测试

```bash
mvn test
```

## 项目文档

- [数据库表结构说明](./数据库表结构说明.md)
- [Excel导入格式说明](./Excel导入格式说明.md)

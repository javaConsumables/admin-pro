# admin-pro 后台管理系统（个人实战项目）

基于 SpringBoot 3 + MyBatis-Plus + MySQL 8 + Redis + JWT + Docker 的后台管理系统实战项目。

## 技术栈

JDK 17+ · SpringBoot 3.2.x · MyBatis-Plus 3.5.x · MySQL 8 · Redis 7 · JWT (jjwt) · Redisson · Docker

## 里程碑进度

- [x] Day 1-2 工程搭建：SpringBoot 3 工程、MySQL 连接、统一返回体、全局异常、MyBatis-Plus 分页、健康检查
- [ ] Day 3-4 登录鉴权：用户表 + JWT 签发/校验 + 拦截器
- [ ] Day 5-6 RBAC：角色/权限 + 注解 + AOP 校验
- [ ] Day 7-8 操作日志 AOP
- [ ] Day 9-10 文件上传 + Redis 缓存 + 防重复提交
- [ ] Day 11-12 Docker 部署 + README
- [ ] Day 13-14 演示走通 + 收尾

## 本机 Redis（Windows 原生版）

> 本机 Docker Desktop 端口转发异常，已改用原生 Windows Redis（redis-windows 8.10.1，msys2 构建）。
> 位置：`C:\Users\yang\Documents\Deepseek\redis-win`

- 启动：双击 `redis-win\start-redis.bat`（或后台运行 `redis-server.exe --port 6379`）
- 停止：双击 `redis-win\stop-redis.bat`
- 验证：`redis-win\Redis-8.10.1-Windows-x64-msys2\redis-cli.exe ping` → `PONG`
- Docker 修复后可切回：`docker start admin-pro-redis`（先停原生 Redis，避免 6379 冲突）

## 快速开始

1. 启动基础设施（Docker）：
   ```bash
   docker compose up -d
   ```
   或使用本机已有的 MySQL/Redis（通过环境变量 DB_URL / DB_USERNAME / DB_PASSWORD / REDIS_HOST / REDIS_PORT 覆盖默认值）。

2. 启动应用：
   ```bash
   mvn spring-boot:run
   ```

3. 验证：
   ```bash
   curl http://localhost:8080/api/health
   ```

## 目录结构

```text
src/main/java/com/adminpro/
├── AdminProApplication.java   # 启动类
├── common/                    # 统一返回体、返回码、全局异常
└── config/                    # MyBatis-Plus 等配置
src/main/resources/
└── application.yml            # 数据库/Redis 等配置（支持环境变量覆盖）
```

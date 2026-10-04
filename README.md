# 区域血透中心患者排班透析机消毒耗材与异常上报平台（HDMS）

面向区域血透中心高频、固定又易被突发情况打断的工作节奏，覆盖 **排班调度 → 透析全流程记录 → 机器消毒/工程 → 耗材库存 → 异常上报 → 收费核对 → 管理报表** 的完整闭环。患者、护士、医生、工程、收费窗口的状态全部汇入同一条**治疗记录**。

## 技术栈

| 层 | 技术 |
|---|---|
| 前端 | Vue 3 + Vite + Element Plus + Pinia + ECharts |
| 后端 | Spring Boot 3 (Java 17) + Spring Data JPA + Spring Security (JWT) |
| 数据库 | PostgreSQL 15 |

## 一键启动（Docker Compose）

```bash
docker compose up -d --build
```

启动后访问 **http://localhost**（前端 80 端口，后端 API 8080，数据库 5432）。
首次启动自动建表并写入演示数据（含今日排班、在透患者、异常、账单）。

## 本地开发启动

```bash
# 1. 数据库（PostgreSQL 15，库名 hdms / 用户 hdms / 密码 hdms123）
createdb -U hdms hdms        # 或 docker run -p 5432:5432 -e POSTGRES_DB=hdms -e POSTGRES_USER=hdms -e POSTGRES_PASSWORD=hdms123 postgres:15-alpine

# 2. 后端（默认连 jdbc:postgresql://localhost:5432/hdms，可用 DB_URL/DB_USER/DB_PASS 覆盖）
cd backend && mvn spring-boot:run

# 3. 前端（Vite 开发服务器，/api 代理到 8080）
cd frontend && npm install && npm run dev   # http://localhost:5173
```

## 测试账号

| 账号 | 密码 | 角色 | 主要页面 |
|---|---|---|---|
| admin | admin123 | 管理员 | 全部 |
| headnurse | head123 | 护士长 | 排班看板、调班影响预览、报表 |
| nurse1 / nurse2 | nurse123 | 护士 | 透析工作台、耗材预领、异常上报 |
| doctor1 | doctor123 | 医生 | 工作台（医嘱变更）、异常处理 |
| eng1 | eng123 | 工程 | 机器状态、消毒/滤芯/水处理记录 |
| fin1 | fin123 | 财务 | 收费核对 |

## 本题验证方式（已实际执行）

仓库根目录提供 `verify.sh` 端到端验证脚本（需后端已启动），覆盖 13 组共 26 项检查，本次交付前在本地（PostgreSQL 15 + 后端 jar + Vite dev）实际执行，**26/26 全部通过**：

1. **登录**：6 个角色账号登录，JWT 鉴权，角色菜单/接口双重隔离（护士访问财务、报表接口返回 403）。
2. **排班看板**（headnurse）：查看某日 机器×班次 看板；「自动排班」按风险/分区/接送窗口分配空机；患者卡片 →「调班/换机」→「影响预览」，可见目标机位队列、受影响患者、被压缩的消毒时间线、已预领耗材，确认后执行调班；感染分区不匹配会被拒绝。
3. **透析全流程**（nurse1）：透析工作台 → 透析前评估（体重/血压/通路/上次不适/抗凝/是否调整脱水）→ 上机 → 监测记录（血压/心率/报警/补液/抽血/不适/医嘱变更）→ 下机（透后体重/止血/离开方式/下次排班）。
4. **异常联动**：录入收缩压 <90 自动生成「透析中低血压」异常；机器报故障、耗材低于安全库存、患者迟到/未到/请假、医嘱变更均自动生成异常并可在异常页知晓/解决。
5. **耗材**：预领关联排班，治疗完成自动转使用；排班取消/患者未到自动退回库存（已验证库存 -2 后退回复原）；损耗登记计入管理报表。
6. **工程**（eng1）：下机后机器自动转「待消毒」，登记消毒合格自动恢复「空闲」；滤芯更换含下次到期；水处理记录可查。
7. **收费**（fin1）：下机自动生成账单（医保基准 + 自费耗材），补收费用登记与核对完成已验证。
8. **管理报表**（admin/headnurse）：机位利用率、异常率、耗材损耗、消毒合规率四项指标输出正常。

前端验证：`npm run build` 构建通过；`npm run dev` 启动后首页 HTTP 200、经 Vite 代理登录成功。
说明：本环境无 Docker 守护进程，`docker compose up -d --build` 未在本机执行；Compose 配置与本地验证使用相同的环境变量与端口（db 5432 / backend 8080 / frontend 80）。

## 核心业务规则

- **排班约束**：患者感染分区（清洁/乙肝/丙肝/隔离）必须与机器分区一致；故障/维护机器不可排班；同一患者同班次唯一。
- **上机顺序**：同机同班次按 seqOrder 依次上机，两台次之间保留 30 分钟消毒间隔（`DISINFECTION_GAP_MINUTES` 可配）；自动排班按 高危优先 → 接送窗口 → 分区匹配 分配。
- **调班影响预览**：模拟目标机器全天时间线，报告开机延误、超出班次窗口（压缩后续消毒/接诊）、受影响患者与已预领耗材。
- **状态联动**：上机 → 机器「透析中」；下机 → 机器「待消毒」→ 消毒合格 → 「空闲」；下机同时生成待核对账单并把预领耗材转使用。

## API 概览（全部需 `Authorization: Bearer <token>`，除登录）

- `POST /api/auth/login` 登录；`GET /api/auth/me` 当前用户
- `GET /api/schedules/board?date=` 看板；`POST /api/schedules` 新增；`POST /api/schedules/auto` 自动排班；`POST /api/schedules/{id}/move/preview|move` 调班；`POST /api/schedules/{id}/cancel|absent|late`
- `POST /api/treatments/pre` 透前评估；`POST /api/treatments/{id}/start|entries|complete`；`GET /api/treatments/{id}` 完整治疗记录
- `GET/PUT /api/engineering/machines*` 机器状态；`POST/GET /api/engineering/records` 消毒/滤芯/水处理
- `GET /api/consumables`；`POST /api/consumables/txns` 预领/使用/退回/损耗/入库
- `GET/POST /api/exceptions`；`POST /api/exceptions/{id}/handle`
- `GET /api/billing`；`POST /api/billing/{id}/adjust|confirm`
- `GET /api/dashboard/today` 今日总览；`GET /api/reports/overview?from=&to=` 管理报表
- `GET/POST/PUT /api/patients` 患者档案

## 目录结构

```
backend/   Spring Boot 后端（entity/repo/service/controller/config）
frontend/  Vue 3 前端（views 按角色页面划分）
docker-compose.yml  一键启动：db + backend + frontend(nginx)
```

## 说明

- 演示数据由 `DataSeeder` 在空库时写入（`SEED_DEMO_DATA=false` 可关闭），排班/治疗记录按**当天日期**生成，任何一天启动都有可演示数据。
- 本项目为全栈交付（含前端页面），按题目要求不强制提供 Swagger UI；上表为接口清单。

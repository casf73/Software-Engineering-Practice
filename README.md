# 在线购物系统

Vue 3 + Element Plus 前端，Spring Boot + H2 后端。源码沿用 `project/mvp-v1.0` 目录，当前为第二轮验收功能：三项基础功能完善。

## 第二轮功能

| 功能 | 当前实现 |
| --- | --- |
| 买家修改联系信息 | 等待中凭口令码修改姓名、电话，不影响原排位。 |
| 卖家手动冻结/解冻 | 暂停某件商品新意向，保留其队列；不能手动解除交易冻结。 |
| 卖家修改密码 | 原密码校验、随机盐 PBKDF2 持久化，旧会话失效。 |

详细规则与本轮验收范围：[第二轮功能说明](project/mvp-v1.0/第二轮功能说明.md)。本轮成果展示三项基础功能；后续需求见需求文档。

## 运行环境

- Node.js 22 及以上；前端依赖以 package-lock.json 为准。
- Java 17 及以上、Maven 3.6.3 及以上。
- 初始卖家账号 `admin`、密码 `123456`，仅首次建库使用。可通过 `APP_SELLER_PASSWORD` 覆盖初始密码；修改密码后重启不会重置。

## 开发启动

在 `project/mvp-v1.0/backend` 运行：

```powershell
mvn spring-boot:run
```

在 `project/mvp-v1.0/frontend` 运行：

```powershell
npm ci
npm run dev
```

访问 `http://localhost:5173`，开发服务器转发 `/api` 和 `/uploads` 到后端 8080。

| 入口 | 地址 |
| --- | --- |
| 商品浏览、搜索、购买、口令码查询 | `/` |
| 卖家登录 | `/seller/login` |
| 商品、队列、分类与账号管理 | `/seller/dashboard` |

集成运行：在 `project/mvp-v1.0/release-iteration2` 运行 `java -jar app.jar`，访问 `http://localhost:8080`。集成包包含本轮前端资源。

## 业务约定

1. 买家免注册，使用口令码查询与修改等待中的姓名和电话；原口令码、编号和排位保持。
2. 手动冻结暂停新意向并保留原队列；只有手动冻结可由卖家解冻，交易冻结不能手动解除。
3. 交易中的买家不能修改或撤销；等待者在手动冻结或他人交易期间仍可修改联系信息。
4. 修改密码先验证原密码，新密码持久化并使旧卖家会话失效。
5. H2 数据库和上传目录默认位于启动工作目录的 `data`，重新启动需保留该目录。

## 文档和成果物

| 成果物 | 链接 |
| --- | --- |
| 总体需求规格 | [需求文档目录](requirement) |
| 第二轮功能完成与验收范围 | [第二轮功能说明](project/mvp-v1.0/第二轮功能说明.md) |
| 本轮后端接口与请求示例 | [后端接口说明](project/mvp-v1.0/backend/后端接口说明.md) |
| 数据模型图及 Mermaid 源码 | [数据模型说明](project/mvp-v1.0/backend/docs/数据模型图.md) |
| 数据模型 SVG | [模型图](project/mvp-v1.0/backend/docs/数据模型图.svg) |
| 前端源码 | [frontend](project/mvp-v1.0/frontend) |
| 后端源码 | [backend](project/mvp-v1.0/backend) |
| 第二轮集成运行包 | [app.jar](project/mvp-v1.0/release-iteration2/app.jar) |
| 首轮测试资料 | [测试目录](tests) |


## 构建与测试记录

第二轮展示 3 个用例，每例 3 个实例，共 9 个文件驱动实例，范围为联系信息修改、手动冻结与解冻和密码修改。数据使用 `tests/第二轮验收测试数据/R201～R203`。

在项目根目录执行：

```bat
tests\run-tests.cmd
```

文件驱动测试入口（保留现有开发测试范围，本轮报告仅引用 R201～R203）：

```bat
tests\run-iteration2-tests.cmd
```

测试使用独立内存 H2，正常业务数据不受影响。具体执行结果及时间见 [第二轮测试说明](tests/第二轮测试说明.md)。前一轮文档和结果作为首轮历史资料保留，第二轮使用下面的新成果物。

| 测试成果物 | 链接 |
| --- | --- |
| 第二轮测试说明与结果 | [测试说明](tests/第二轮测试说明.md) |
| 测试用例 Word | [第二轮测试用例](tests/在线购物系统第二轮验收测试用例.docx) |
| 测试用例 Markdown | [完整 Markdown](tests/在线购物系统第二轮验收测试用例.md) |
| 测试数据 Excel | [数据清单](tests/第二轮测试用例数据清单.xlsx) |
| 测试实例文件目录 | [第二轮验收测试数据](tests/第二轮验收测试数据) |

构建第二轮集成包：`powershell -ExecutionPolicy Bypass -File project/mvp-v1.0/scripts/build-iteration2.ps1 -Offline`。输出 `project/mvp-v1.0/release-iteration2/app.jar`。浏览器交互和真实服务重启需另行验收，后端自动化通过不等同于人工界面验收通过。

2026-10-21 实际记录中，本轮引用的 9 个文件驱动实例和 9 项基础补充回归均通过。前端生产构建、后端打包成功。本次文档调整没有重新运行测试或构建。

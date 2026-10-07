# 单卖家排队购物系统

首轮验收实现：Vue 3 + Element Plus 前端、Spring Boot 后端。买家免注册，填写姓名和电话后按先到先得顺序进入队列；卖家账号固定为 admin，初始密码为 123456。

## 环境要求

- Node.js 22 及以上（当前锁定依赖中 `@vueuse` 要求 Node.js 22 及以上）
- Java 17 及以上
- Maven 3.6.3 及以上

## 启动后端

在 backend 目录运行：

    mvn spring-boot:run

后端地址为 http://localhost:8080   H2 数据库文件和上传图片保存在 backend/data，应用重启后仍保留。

## 启动前端

在 frontend 目录运行：

    npm install
    npm run dev

访问 http://localhost:5173  前端开发服务器会把 /api 和 /uploads 请求转发到后端。

## 已实现的前端功能

### 买家页面（`/`）

- 免注册查看当前商品的名称、价格、描述、图片和状态；无商品时显示空状态。商品进入交易后仍可查看，但页面暂停接收新意向。
- 在商品在售时填写姓名、联系电话提交购买意向，提交成功后弹窗展示口令码，并提供复制按钮。买家应自行保存口令码，以便之后查询进度。
- 输入口令码查询自己的意向状态、当前排位和商品状态。仍在排队的买家可在确认弹窗中撤销，即使商品正在与队首买家交易；已经进入交易的买家不能撤销。

### 卖家页面（`/seller/login`、`/seller/dashboard`）

- 使用固定卖家账号登录；后台提供“概览、商品管理、购买意向”侧边菜单，可查看当前商品、等待人数、交易状态并退出登录。
- 发布或编辑当前商品，填写必需的名称和价格；描述、图片可选。首轮每件商品最多 1 张 JPG/PNG 图片，单张不超过 5 MB。商品交易中不能编辑。
- 查看按排队顺序排列的买家姓名、联系电话、排队时间和状态；对排队意向可选择作废或排到队尾，也可由卖家主动开启与队首买家的交易。
- 登记交易成功或失败；失败时选择作废当前意向或重新排到队尾。商品在售时可主动下架，交易中不可下架。

前端使用 Vue 3、Element Plus 和 Vue Router；对需要确认的操作使用弹窗。公共组件位于 `frontend/src/components/common`，见下节。

## 已实现的后端功能与规则

- Spring Boot 提供买家公开接口和卖家接口。卖家登录使用服务端会话；除登录、会话检查外，`/api/seller/**` 接口均需已登录。初始账号为 `admin`，初始密码为 `123456`，配置见 `backend/src/main/resources/application.yml`。
- 同时只允许一件商品处于在售或交易中。商品可发布、编辑、上传图片、主动下架；商品信息及购买意向保存在文件型 H2 数据库 `backend/data` 中。图片上传到 `backend/data/uploads`，通过 `/uploads/` 路径访问。
- 买家提交意向后生成唯一口令码，等待队列按排队时间及编号排序。卖家开启交易时系统自动选取队首，商品进入交易中并拒绝新意向；其余等待买家仍可撤销。
- 撤销或作废意向后，口令码立即失效。重新排队时将原意向移到队尾，原口令码继续有效；交易失败后商品恢复在售，卖家可再次决定何时开启交易。
- 交易成功时商品下架，其他等待意向标记为交易失败，本商品口令码失效。卖家主动下架时，等待意向标记为商品下架并使口令码失效。当前工作台只返回在售或交易中的商品及其队列。
- 后端校验必填字段、价格、图片格式和 5 MB 大小上限，并拒绝给商品关联超过 1 张图片。后续迭代计划将每件商品图片上限扩展至 5 张；当前代码尚未开放该功能。

## 共用组件与弹窗

公共组件位于 frontend/src/components/common。AppHeader、PageHeader、AppCard、StatusBadge、ProductImage、ProductGallery 和 EmptyState 供多个页面复用。AppDialog 封装 Element Plus Dialog：默认插槽承载正文，title 插槽自定义标题，footer 插槽放置页面所需操作按钮。

## 后端接口清单

接口基址为 `http://localhost:8080`。下表路径包含 `/api` 前缀；请求体字段使用 JSON，图片上传使用 `multipart/form-data`。卖家登录后需在后续请求中携带会话 Cookie，前端请求已配置 `withCredentials`。`{passcode}` 是买家口令码，`{intentId}` 是卖家工作台返回的意向编号。

### 买家公开接口

| 方法 | 路径 | 请求 | 返回与作用 |
| --- | --- | --- | --- |
| `GET` | `/api/public/products/current` | 无 | 当前商品的编号、名称、价格、描述、图片、状态、发布时间；无当前商品时返回 404。 |
| `POST` | `/api/public/intents` | `{"name":"姓名","phone":"联系电话"}` | 创建排队意向；返回 201、`passcode` 和初始 `position`。商品交易中不接收新意向。 |
| `GET` | `/api/public/intents/{passcode}` | 路径中的口令码 | 返回买家姓名、意向状态、等待排位及商品状态；无效或失效口令码返回 404。 |
| `DELETE` | `/api/public/intents/{passcode}` | 路径中的口令码 | 撤销仍在排队中的意向，返回 204，口令码立即失效；已进入交易则拒绝撤销。 |

### 卖家接口

| 方法 | 路径 | 请求 | 返回与作用 |
| --- | --- | --- | --- |
| `POST` | `/api/seller/login` | `{"username":"admin","password":"123456"}` | 校验账号密码并建立会话，返回 `{"authenticated":true}`。 |
| `GET` | `/api/seller/session` | 无 | 返回当前会话的 `authenticated` 布尔值；无需先登录。 |
| `POST` | `/api/seller/logout` | 无 | 退出并注销会话，返回 204。 |
| `GET` | `/api/seller/workbench` | 无 | 返回当前商品、队列 `intents`、等待人数 `waitingCount` 和当前交易意向 `activeIntent`。 |
| `POST` | `/api/seller/products` | `{"name":"商品名称","price":99.00,"description":"描述","images":[]}` | 发布商品，返回 201 和商品信息；已有在售或交易中商品时拒绝重复发布。 |
| `PUT` | `/api/seller/products/current` | 同发布商品的请求体 | 修改当前商品；交易中不可修改。`images` 最多包含 1 个上传后返回的图片地址。 |
| `POST` | `/api/seller/products/image` | 表单字段 `file`，JPG/PNG，最大 5 MB | 上传单个图片文件，返回 `{"url":"/uploads/文件名"}`。上传后仍需在发布或编辑商品时提交该地址。 |
| `POST` | `/api/seller/intents/{intentId}/action` | `{"action":"VOID"}` 或 `{"action":"REQUEUE"}` | 作废排队意向，或保留原口令码并排到队尾；返回 204。 |
| `POST` | `/api/seller/trades/start` | 无 | 自动选取队首买家开始交易，返回当前交易意向；商品进入交易中。 |
| `POST` | `/api/seller/trades/{intentId}/result` | 成功：`{"success":true}`；失败：`{"success":false,"disposition":"VOID"}` 或 `{"success":false,"disposition":"REQUEUE"}` | 登记线下交易结果，返回 204；失败时必须选择当前意向的后续处理方式。 |
| `POST` | `/api/seller/products/current/delist` | 无 | 主动下架当前商品并使等待意向的口令码失效，返回 204；交易中不可下架。 |

接口通过 `message` 字段返回错误说明：常见状态为 400（参数无效）、401（未登录或账号密码错误）、404（商品或口令码不存在）、409（当前状态不允许操作）。

## 首轮验收测试

测试的目的是核对买家免注册提交意向、口令码查询、FIFO 排队、买家撤销、卖家登录及商品管理、交易成功/失败和商品下架等基础流程，防止状态或排位错误、口令码失效不及时。自动化测试还检查必填字段、单件商品限制和图片上传类型。

测试文件及详细用例清单位于 [tests/测试说明.md](tests/测试说明.md)。在项目根目录运行 `tests\run-tests.cmd`，或进入 `backend` 目录执行 `mvn test`。自动化测试使用独立的内存 H2 数据库，不会清空日常使用的 `backend/data`；测试报告由 Maven 输出到 `backend/target/surefire-reports/`。

2026-10-07 本机执行结果：10 个后端集成测试通过、0 个失败，前端 `npm run build` 成功。浏览器操作和服务重启检查仍需按测试文档执行并留存证据。

网页实际显示、窄屏布局和服务重启后的数据保留，需要按测试文档的人工检查表另行记录并留存截图或录屏。首轮商品图片上限已统一为 1 张；姓名、电话等字段的校验差异仍在测试文档中列为待处理问题。

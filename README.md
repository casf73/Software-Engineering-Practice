# 在线购物系统

Vue3与Element Plus前端，Spring Boot与H2后端。当前第三次迭代完成挑战需求A.1商品管理升级与A.2客户升级，源码沿用project/mvp-v1.0。

## 三轮功能

| 轮次 | 验收内容 |
| --- | --- |
| 首轮 | 访客浏览、购买意向与口令码、FIFO队列、交易结果及基础上传。 |
| 第二轮 | 等待联系信息修改、手动冻结解冻、卖家密码修改。 |
| 第三轮 | 多商品、两级分类、库存与售罄下架、搜索、多图富文本、客户注册登录和历史、卖家客户管理。 |

## 开发启动

需要Java17+、Maven3.6.3+、Node22+。后端目录project/mvp-v1.0/backend执行mvn spring-boot:run；前端目录project/mvp-v1.0/frontend执行npm ci、npm run dev。打开http://localhost:5173。

| 入口 | 路径 |
| --- | --- |
| 商品浏览与意向 | / |
| 客户注册 | /customer/register |
| 客户登录 | /customer/login |
| 我的购买历史 | /customer/account |
| 卖家登录 | /seller/login |
| 商品、分类、库存、队列、客户及账号管理 | /seller/dashboard |

初始卖家admin/123456仅首次建库使用，改密后不会因重启恢复初始密码。日常H2数据库及上传目录在启动目录data中。

## 文档和成果物

| 成果物 | 链接 |
| --- | --- |
| 需求规格 | [完整需求](requirement/在线购物系统需求规格说明书.md) |
| 第三轮需求补充 | [第三轮需求](requirement/第三轮/第三轮需求补充说明.md) |
| 第三轮功能说明 | [功能说明](project/mvp-v1.0/第三轮功能说明.md) |
| 子系统划分 | [子系统说明](subsystem/在线购物系统子系统划分说明.md) |
| 后端接口 | [接口说明](project/mvp-v1.0/backend/后端接口说明.md) |
| 数据模型 | [字段与关系](project/mvp-v1.0/backend/docs/数据模型图.md) |
| 三轮测试方案 | [测试总方案](tests/三轮测试总方案.md) |
| 首轮测试 | [首轮用例](tests/在线购物系统首轮验收测试用例.md) |
| 第二轮测试 | [第二轮用例](tests/在线购物系统第二轮验收测试用例.md) |
| 第三轮测试 | [第三轮用例](tests/在线购物系统第三轮验收测试用例.md) |

## 测试运行

运行tests/run-tests.cmd执行全部接口回归和已有基础补充测试；也可运行tests/run-iteration1-tests.cmd、run-iteration2-tests.cmd、run-iteration3-tests.cmd分轮运行。

每轮实例目录下先分功能测试和接口测试，再按总用例编号及第n个测试实例划分。每总用例每类至少8个实例；Excel中分别展示总方案、功能测试、接口测试、原始请求数据，末行解释测试设计方法。

功能测试为网页人工验收，待执行状态单独保留；接口自动化使用独立内存H2与target测试上传目录，不操作日常data。旧发布包和Docker镜像未重建，运行本次功能请使用当前源码，不把历史JAR作为第三轮包。

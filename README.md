# 单卖家排队购物系统

首轮验收实现：Vue 3 + Element Plus 前端、Spring Boot 后端。买家免注册，填写姓名和电话后按先到先得顺序进入队列；卖家账号固定为 admin，初始密码为 123456。

## 环境要求

- Node.js 16.20 及以上
- Java 17 及以上
- Maven 3.6.3 及以上

## 启动后端

在 backend 目录运行：

    mvn spring-boot:run

后端地址为 http://localhost:8080。H2 数据库文件和上传图片保存在 backend/data，应用重启后仍保留。

## 启动前端

在 frontend 目录运行：

    npm install
    npm run dev

访问 http://localhost:5173。前端开发服务器会把 /api 和 /uploads 请求转发到后端。

## 首轮包含的功能

- 买家查看当前唯一商品，填写姓名和联系电话提交意向。
- 提交成功后展示一次性口令码；买家可凭码查看状态和排位。
- 排队中的买家可撤销意向，交易中的买家不可撤销；撤销后口令码立即失效。
- 卖家可登录后台发布或编辑一件商品，查看当前购买队列。
- 卖家可作废排队意向，或让买家排到队尾；排到队尾保留原口令码。
- 卖家开始交易后，系统只锁定队首买家并暂停接收新意向。
- 卖家登记交易成功或失败；失败时选择作废或重新排队。
- 商品成交或卖家主动下架后，商品从买家端撤下，相关口令码立即失效。
- 商品名称和价格必填；描述和商品图片选填，图片最多 5 张，JPG/PNG 单张上限 5 MB。

## 共用组件与弹窗

公共组件位于 frontend/src/components/common。AppHeader、PageHeader、AppCard、StatusBadge、ProductImage、ProductGallery 和 EmptyState 供多个页面复用。AppDialog 封装 Element Plus Dialog：默认插槽承载正文，title 插槽自定义标题，footer 插槽放置页面所需操作按钮。

## 主要 API

| 方法 | 地址 | 用途 |
| --- | --- | --- |
| GET | /api/public/products/current | 查询当前商品 |
| POST | /api/public/intents | 提交购买意向 |
| GET | /api/public/intents/{口令码} | 查询状态和队列位置 |
| DELETE | /api/public/intents/{口令码} | 撤销尚未进入交易的意向 |
| POST | /api/seller/login | 卖家登录 |
| GET | /api/seller/workbench | 获取商品和排队队列 |
| POST | /api/seller/trades/start | 开始处理队首意向 |
| POST | /api/seller/trades/{意向编号}/result | 登记交易结果 |

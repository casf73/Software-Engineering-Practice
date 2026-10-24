# 在线购物系统前端

第三轮版本，Vue3、Element Plus、Vue Router、Axios。需要Node22及以上。

## 启动

frontend目录执行npm ci、npm run dev，访问http://localhost:5173；/api和/uploads代理至后端8080。构建命令npm run build。

## 页面与数据

StorefrontView负责多商品、搜索、分类、相册和购买意向；CustomerAuthView负责客户注册登录；CustomerAccountView负责本人历史；SellerDashboardView负责商品分类库存和队列，SellerCustomers负责客户查询与历史。customerSession维护客户身份，sellerApi和customerApi分别封装接口。

## 验收

三轮功能测试资料位于tests各轮数据目录的功能测试子目录，每例包含操作步骤、数据和预期。页面人工测试结果单独填写，不直接使用接口测试通过状态。

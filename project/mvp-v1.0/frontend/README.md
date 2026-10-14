# 前端项目

技术栈：Vue 3、Vite、Element Plus、Vue Router、Axios。

## 启动

需要 Node.js 16.20 及以上版本。

1. 在 frontend 目录安装依赖：npm install
2. 启动开发服务器：npm run dev

开发服务器默认地址为 http://localhost:5173，/api 和 /uploads 请求会代理到 http://localhost:8080。

## 构建

运行 npm run build。

## 主要目录

- src/views：商品首页、卖家登录、卖家工作台。
- src/components/common：页面共用组件。AppDialog 通过默认插槽承载弹窗主体，通过 title、footer 插槽自定义标题和操作区。
- src/api：Axios 实例和接口封装。
- src/utils：状态文案与日期、价格格式化。

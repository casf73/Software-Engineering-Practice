# 第二轮集成运行包

本轮展示买家联系信息修改、卖家手动冻结与解冻、卖家密码修改。集成 JAR 内含前端资源。

Java 17 及以上，在本目录执行：

```powershell
java -jar app.jar
```

访问 http://localhost:8080 。首次建库卖家默认 admin / 123456；修改过的密码以所用数据库为准。买家免注册，A.2 客户升级暂未纳入。

默认数据库及上传文件保存在本目录的 data 中。需要复用原业务数据时，用 --spring.datasource.url 和 --app.upload-directory 指向原来的数据库及上传目录，并确保旧服务已经停止占用该库。本次构建没有启动或修改现有运行服务。

重新构建：在项目目录执行 `powershell -ExecutionPolicy Bypass -File scripts/build-iteration2.ps1`；可用 `-InstallDependencies` 安装前端依赖、`-Offline` 使用本地 Maven 缓存。脚本不运行测试。

第二轮说明见 ../第二轮功能说明.md；本包没有同步替换已发布的旧 Docker 镜像。

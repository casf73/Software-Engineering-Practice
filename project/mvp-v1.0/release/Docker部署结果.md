# Docker 部署结果

- 执行时间：2026-10-19 14:59:37 +08:00
- 镜像：online-shopping-system:1.0.0
- 镜像详情：sha256:04f5e292fb1e38b0627e0080d341a7e71c04c7886306eb17fb84b2db1a86ff40 linux/amd64
- 访问地址：http://localhost:8080/
- 卖家登录：http://localhost:8080/seller/login
- 网页检查：首页和登录页均返回 HTTP 200，会话接口可访问。
- 持久化目录：release/docker-data（H2 数据库及上传图片）
- 镜像文件：online-shopping-system-1.0.0-image.tar；SHA256：CB02F510652BFDCE81C262997095041DF17279FD6FC8A3D59AB1E9F2D533FC74

## 容器状态

```text
NAME              IMAGE                                                                     COMMAND                  SERVICE   CREATED         STATUS         PORTS
release-store-1   sha256:613a01588e48185b6ee305550760414fc5a9dba65d48c3a0bc2f4840ebbd5628   "java -jar /app/app.…"   store     2 minutes ago   Up 2 minutes   0.0.0.0:8080->8080/tcp, [::]:8080->8080/tcp
```

停止容器：在 release 目录执行 docker compose down。
重新启动：在 release 目录执行 docker compose up -d。

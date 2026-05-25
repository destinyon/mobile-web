# mobile-web

羽球在线：羽毛球新闻、评论、收藏与内容发布微信小程序，配套 Spring Boot 后端。

## Modules

- `backend/`: REST API、H2 文件数据库、OSS 上传、微信登录、网易羽毛球新闻同步和后台接口。
- `app/`: 原生微信小程序页面、组件和接口服务。
- `docs/`: 接口文档和迭代说明。

## Backend

```powershell
cd backend
mvn test
mvn spring-boot:run
```

默认接口地址：`http://127.0.0.1:8080/api`。

运行时密钥从 `backend/.env` 读取，该文件已被 `.gitignore` 忽略。
后台管理员登录需要配置 `ADMIN_USERNAME`、`ADMIN_PASSWORD`，可选配置 `ADMIN_USER_ID` 绑定已有 `ADMIN` 用户。

## Mini Program

在微信开发者工具中打开 `app/`。小程序 AppID 位于 `app/project.config.json`。

小程序后端地址在 `app/utils/constants.js` 中配置。

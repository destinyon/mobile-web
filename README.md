# mobile-web

羽球在线：羽毛球新闻论坛微信小程序及 Spring Boot 后端。

## Modules

- `backend/`: REST API, H2 file database, OSS upload, WeChat login, admin APIs.
- `app/`: native WeChat Mini Program pages and services.
- `docs/`: API and iteration notes.

## Backend

```powershell
cd backend
mvn test
mvn spring-boot:run
```

Default API base URL: `http://localhost:8080/api`.

Runtime secrets are loaded from `backend/.env`, which is ignored by Git.

## Mini Program

Open `app/` in WeChat Developer Tools. The AppID is configured in `app/project.config.json`.

The Mini Program calls the backend configured in `app/utils/constants.js`.

# 羽球在线接口文档

## 通用响应

```json
{ "success": true, "data": {}, "msg": "ok" }
```

## 小程序接口

### 首页

- `GET /api/banners`
- `GET /api/categories`

### 资讯

- `GET /api/news?keyword=&categoryId=&sort=latest|hot&page=1&pageSize=10`
- `GET /api/news/{id}`
- `POST /api/news`
- `POST /api/news/{id}/favorite`
- `DELETE /api/news/{id}/favorite`
- `POST /api/news/{id}/like`

### 评论

- `POST /api/comments`

### 用户中心

- `GET /api/user/profile`
- `PUT /api/user/profile`
- `GET /api/user/favorites`
- `GET /api/user/comments`
- `GET /api/user/posts`

### 登录与上传

- `POST /api/auth/wx-login`
- `POST /api/upload`

## 后台接口

- `GET /api/admin/news`
- `PUT /api/admin/news/{id}/status?status=PUBLISHED|DRAFT|OFFLINE`

## 权限

- 资讯列表、详情、首页内容公开可读。
- 收藏、点赞、评论、发布、资料查看与修改需要登录。
- 后台接口需要 `ADMIN` 角色。

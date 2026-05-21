# 羽球在线接口文档

## 通用响应

```json
{ "success": true, "data": {}, "msg": "ok" }
```

失败响应仍使用同一结构，`success=false`，`msg` 为可展示错误信息。

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

请求体：

```json
{ "targetType": "NEWS", "targetId": 1, "content": "评论内容" }
```

### 用户中心

- `GET /api/user/profile`
- `PUT /api/user/profile`
- `GET /api/user/favorites`
- `GET /api/user/comments`
- `GET /api/user/posts`

### 登录与上传

- `POST /api/auth/wx-login`
- `POST /api/upload`

`/api/auth/wx-login` 请求体：

```json
{ "code": "wx.login 返回的 code", "nickname": "可选昵称", "avatarUrl": "可选头像" }
```

微信错误码或网络异常不会返回裸 500，会转换为业务错误响应。

## 后台接口

- `GET /api/admin/news`
- `PUT /api/admin/news/{id}/status?status=PUBLISHED|DRAFT|OFFLINE`
- `POST /api/admin/news/sync/juhe`

`POST /api/admin/news/sync/juhe` 会调用聚合新闻头条接口 `https://v.juhe.cn/toutiao/index`，使用 `type=tiyu` 获取体育新闻，再按羽毛球关键词过滤后写入 `news` 表。重复数据按 `source=JUHE` 和 `source_id=uniquekey` 跳过。

## 权限

- 资讯列表、详情、首页内容公开可读。
- 收藏、点赞、评论、发布、资料查看与修改需要登录。
- 后台接口需要 `ADMIN` 角色。

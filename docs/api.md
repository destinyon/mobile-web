# 羽球在线接口文档

## 通用响应

```json
{ "success": true, "data": {}, "msg": "ok" }
```

失败响应仍使用同一结构，`success=false`，`msg` 为可展示错误信息。

## 首页

- `GET /api/banners`
  - 返回最新已发布新闻的前 5 个封面轮播。
  - `linkType=NEWS`，`linkTarget` 为新闻 ID。
- `GET /api/topics`
  - 返回投稿分类：赛事讨论、社区交流、羽球装备。

## 资讯

- `GET /api/news?keyword=&categoryId=&sort=latest|hot&page=1&pageSize=10`
- `GET /api/news/{id}`
- `POST /api/news/{id}/favorite`
- `DELETE /api/news/{id}/favorite`
- `POST /api/news/{id}/like`

点赞接口会切换当前用户点赞状态，并返回：

```json
{ "liked": true, "favorited": false, "likeCount": 1, "favoriteCount": 0 }
```

## 玩家投稿

- `GET /api/topics`
- `GET /api/posts?topicId=&keyword=&sort=latest|hot&page=1&pageSize=10`
- `GET /api/posts/{id}`
- `POST /api/posts`
- `PUT /api/posts/{id}`
- `GET /api/posts/draft`
- `PUT /api/posts/draft`
- `POST /api/posts/draft/publish`
- `POST /api/posts/{id}/favorite`
- `DELETE /api/posts/{id}/favorite`
- `POST /api/posts/{id}/like`

`POST /api/posts` 和 `PUT /api/posts/{id}` 需要登录，请求体：

```json
{
  "topicId": 1,
  "title": "双打接发站位怎么选？",
  "coverUrl": "https://example.com/cover.jpg",
  "content": "帖子正文",
  "images": ["https://example.com/post.jpg"]
}
```

`PUT /api/posts/draft` 会为当前用户保存唯一草稿；再次保存会覆盖同一草稿。发布草稿后状态变为 `PUBLISHED`。

## 评论

- `POST /api/comments`

```json
{ "targetType": "POST", "targetId": 1, "parentId": 10, "content": "回复内容" }
```

`targetType` 支持 `NEWS` 和 `POST`。回复时 `parentId` 必须属于同一个目标对象。

## 用户中心

- `GET /api/user/profile`
- `PUT /api/user/profile`
- `GET /api/user/favorites`
- `GET /api/user/history`
- `GET /api/user/comments`
- `GET /api/user/posts`

`GET /api/user/comments` 返回评论本身以及目标对象摘要，前端可按 `targetType` 跳转新闻或帖子详情。

`GET /api/user/history` 返回当前用户最近浏览的新闻和帖子，按 `viewedAt` 倒序排列；重复浏览同一目标只更新浏览时间，不重复新增记录。

## 登录与上传

- `POST /api/auth/wx-login`
- `POST /api/auth/admin-login`
- `POST /api/upload`
- `DELETE /api/upload?objectKey=uploads/example.jpg`

管理员登录用于后台网站，不提供注册接口。请求体：

```json
{ "username": "admin", "password": "your-admin-password" }
```

成功返回与小程序登录一致的 `LoginResult`。后端从 `ADMIN_USERNAME`、`ADMIN_PASSWORD`、`ADMIN_USER_ID` 读取唯一管理员配置；密码未配置时返回 503，账号或密码错误时返回 401。

上传和删除均需要登录。删除只接受 OSS object key，不接受完整 URL。

## 后台管理

以下接口都需要 `Authorization: Bearer <token>`，且当前用户角色必须为 `ADMIN`。

- `GET /api/admin/summary`
  - 返回用户数、文章数、帖子数、评论数、浏览/点赞/收藏汇总，以及各分类文章和互动统计。
- `GET /api/admin/news?keyword=&categoryId=&page=1&pageSize=20`
  - 返回 `news` 表全量分页数据，包括已发布和已下架文章；`keyword` 匹配标题、摘要、作者，`categoryId` 按分类过滤。
- `GET /api/admin/news/{id}`
  - 返回后台只读文章详情，不增加浏览量，也不受前台发布状态过滤影响。
- `GET /api/admin/news/rankings?limit=10`
  - 返回点赞数加收藏数的文章热度排行，默认 Top 10。
- `PUT /api/admin/news/{id}/status?status=OFFLINE`
  - 更新文章状态，并写入 `admin_logs`。
- `POST /api/admin/news/sync?pages=1`
  - 手动触发网易羽毛球新闻增量同步，`pages` 限制为 1 到 10。
- `GET /api/admin/users?keyword=&page=1&pageSize=20`
  - 返回用户分页列表和发帖、评论、收藏统计。
- `GET /api/admin/users/{id}`
  - 返回用户资料详情和发帖、评论、收藏、点赞、浏览记录统计。

## 权限

- 首页、资讯列表/详情、投稿列表/详情公开可读。
- 收藏、点赞、评论、回复、发布、草稿、编辑、资料查看与修改需要登录。
- 后台接口需要 `ADMIN` 角色。

## App ???????

- `POST /api/auth/email-code`
  - ????`{ "email": "player@example.com" }`
  - ???`{ "email": "player@example.com", "validTime": 300, "interval": 60, "debugCode": null }`
  - ??????????? 400????????? 429?????????? 503????????? 502?
- `POST /api/auth/email-login`
  - ????`{ "email": "player@example.com", "code": "123456" }`
  - ????? `wx-login` ??? `LoginResult`?????????????????
  - ????????????? 401?

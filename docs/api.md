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

回复评论时传 `parentId`。`targetType` 支持 `NEWS` 与 `POST`。

```json
{ "targetType": "POST", "targetId": 1, "parentId": 10, "content": "回复内容" }
```

### 社区

- `GET /api/topics`
- `GET /api/posts?topicId=&keyword=&sort=latest|hot&page=1&pageSize=10`
- `GET /api/posts/{id}`
- `POST /api/posts`
- `POST /api/posts/{id}/favorite`
- `DELETE /api/posts/{id}/favorite`
- `POST /api/posts/{id}/like`

`POST /api/posts` 需要登录，请求体：

```json
{
  "topicId": 2,
  "title": "双打接发站位怎么选？",
  "content": "帖子正文",
  "images": ["https://example.com/post.jpg"]
}
```

帖子状态包含 `PENDING`、`PUBLISHED`、`REJECTED`、`OFFLINE`。当前小程序发帖为课程演示闭环直接发布为 `PUBLISHED`。

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
- `POST /api/admin/news/sync`

`POST /api/admin/news/sync` 会抓取网易体育羽毛球滚动新闻 `https://sports.163.com/special/00051L24/ymq09.html` 前 10 页，读取详情页正文后增量写入 `news` 表。已存在的网易新闻不会重复同步，发布时间不晚于库内最新网易新闻 `created_at` 的数据会跳过；新入库新闻使用原文发布时间写入 `created_at` 和 `updated_at`。同步不会清空新闻、评论、点赞或收藏数据；正文入库前会清洗为安全 HTML，保留正文图片和图注，封面图不重复写入正文开头，标题按原文完整保存。后端启动时会自动同步一次，随后默认每 5 分钟同步一次。

## 权限

- 资讯列表、详情、首页内容公开可读。
- 社区话题、帖子列表和帖子详情公开可读。
- 收藏、点赞、评论、回复、发帖、资料查看与修改需要登录。
- 后台接口需要 `ADMIN` 角色。

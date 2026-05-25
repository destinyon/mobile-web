# Mini Program Completion Design

## Goal

Complete the WeChat Mini Program user-facing flow for 羽球在线 so it can demonstrate real backend data, news browsing, community posts, publishing, comments, replies, likes, favorites, and user-center history.

## Approved Direction

Use the five-entry bottom navigation direction:

- 首页
- 资讯
- center circular `+` publish entry
- 社区
- 我的

The top homepage publish button is removed. The center `+` opens the publish flow. The Mini Program scope is the delivery target; no standalone backend admin website is built in this iteration.

## Product Scope

The implementation focuses on a real Mini Program closed loop:

- Home remains the entry for banners, categories, search, and hot/latest news.
- News list and detail keep existing real APIs and support like, favorite, comments, and comment replies.
- Community becomes a first-class Mini Program tab with post list, post detail, topic filter, sorting, publishing, like, favorite, and replies.
- Publish flow creates community posts, not news articles. Existing user-created news-as-post behavior is not preserved as a compatibility path.
- Mine shows profile plus collections, comments, and posts backed by real API data.

## Backend Scope

Add missing server-side community capabilities:

- Tables: `topics`, `posts`, and reuse `comments`, `favorites`, `likes` with `target_type = 'POST'`.
- Public APIs for post list, topic list, and post detail.
- Authenticated APIs for creating posts, liking posts, favoriting posts, and listing current user's posts.
- Post status supports `PENDING`, `PUBLISHED`, `REJECTED`, and `OFFLINE`; user-created posts use `PUBLISHED` for demo usability unless admin moderation later changes it.
- Comments already support `parent_id`; the Mini Program will expose this as reply UI for `NEWS` and `POST`.

## UI Scope

Use the existing visual language: pale green background, black outline, compact content cards, and WeChat-native spacing. Avoid decorative-only UI. Use custom tab bar image assets or CSS-like text/icon layout only inside Mini Program constraints; keep the center `+` visually dominant and tappable.

## Validation

Run:

- Backend targeted tests for post flow.
- Full backend `mvn test` if targeted tests pass.
- Mini Program compile through WeChat developer MCP.
- WeChat MCP runtime checks, page data, logs, and screenshots for home, community, publish, detail, and mine flows with real backend data.

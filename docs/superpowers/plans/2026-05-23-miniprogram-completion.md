# Mini Program Completion Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the Mini Program-only completion pass for 羽球在线 with a center publish tab, community posts, replies, and real backend integration.

**Architecture:** Extend the existing Spring Boot JDBC pattern with post/topic DTOs, service, controller, and row mappers. Extend the native WeChat Mini Program with a custom tab bar and focused post pages while reusing the existing request/auth/news-card patterns.

**Tech Stack:** Spring Boot 3, JdbcTemplate, H2/MySQL-compatible schema SQL, native WeChat Mini Program WXML/WXSS/JS, WeChat developer MCP.

---

### Task 1: Backend Community Contract

**Files:**
- Modify: `backend/src/test/java/com/server/backend/UserFlowIntegrationTest.java`
- Modify: `backend/src/main/resources/schema.sql`
- Modify: `backend/src/main/resources/data.sql`
- Create: `backend/src/main/java/com/server/backend/post/dto/*.java`
- Create: `backend/src/main/java/com/server/backend/post/service/PostService.java`
- Create: `backend/src/main/java/com/server/backend/post/controller/PostController.java`
- Modify: `backend/src/main/java/com/server/backend/common/Rows.java`
- Modify: `backend/src/main/java/com/server/backend/comment/service/CommentService.java`
- Modify: `backend/src/main/java/com/server/backend/user/service/UserService.java`
- Modify: `backend/src/main/java/com/server/backend/user/controller/UserController.java`

- [ ] Add a failing integration test for topic list, post creation, list, detail, like, favorite, reply, mine posts.
- [ ] Run targeted test and confirm it fails because `/api/topics` or `/api/posts` is missing.
- [ ] Add schema and seed topics/posts.
- [ ] Add post DTOs, row mappers, service, and controller.
- [ ] Extend comments count update to `POST`.
- [ ] Extend user posts to return community posts.
- [ ] Re-run targeted and full backend tests.

### Task 2: Mini Program Navigation

**Files:**
- Modify: `app/app.json`
- Create: `app/custom-tab-bar/index.js`
- Create: `app/custom-tab-bar/index.json`
- Create: `app/custom-tab-bar/index.wxml`
- Create: `app/custom-tab-bar/index.wxss`
- Modify: `app/pages/home/index.wxml`
- Modify: `app/pages/home/index.wxss`
- Modify: `app/pages/home/index.js`
- Modify existing tab pages to mark selected tab on show.

- [ ] Replace the native two-item tab bar with a five-entry custom tab bar.
- [ ] Remove the top homepage publish button.
- [ ] Center the circular `+` publish action in the bottom navigation.
- [ ] Keep all tab targets shallow and predictable.

### Task 3: Community Pages and Publish Flow

**Files:**
- Modify: `app/services/api.js`
- Modify: `app/pages/news/editor/index.*`
- Create: `app/pages/community/index.*`
- Create: `app/pages/post/detail/index.*`
- Modify: `app/app.json`
- Modify: `app/pages/mine/posts/index.*`

- [ ] Add API wrappers for topics and posts.
- [ ] Convert publish page to create community posts.
- [ ] Build community list with topic filters, latest/hot sort, loading, empty, error, and pagination states.
- [ ] Build post detail with content, images, like/favorite, comments, and replies.
- [ ] Make mine posts read from real user post API.

### Task 4: Test and Iterate

**Files:**
- Modify only files needed by observed failures.

- [ ] Run `mvn test` in `backend`.
- [ ] Start backend with real local configuration.
- [ ] Compile Mini Program through WeChat MCP.
- [ ] Use WeChat MCP to navigate home, community, publish, detail, and mine pages.
- [ ] Capture screenshots and inspect logs.
- [ ] Fix layout or runtime bugs found by MCP, then re-run the smallest relevant checks.

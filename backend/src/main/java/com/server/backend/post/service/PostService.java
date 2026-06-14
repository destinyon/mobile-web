package com.server.backend.post.service;

import com.server.backend.auth.dto.AuthUser;
import com.server.backend.comment.service.CommentService;
import com.server.backend.common.ActionState;
import com.server.backend.common.BusinessException;
import com.server.backend.common.PageResult;
import com.server.backend.common.Rows;
import com.server.backend.post.dto.CreatePostRequest;
import com.server.backend.post.dto.PostDetail;
import com.server.backend.post.dto.PostDraftRequest;
import com.server.backend.post.dto.PostSummary;
import com.server.backend.post.dto.TopicItem;
import com.server.backend.user.service.BrowseHistoryService;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class PostService {
    private final JdbcTemplate jdbcTemplate;
    private final CommentService commentService;
    private final BrowseHistoryService browseHistoryService;

    public PostService(JdbcTemplate jdbcTemplate, CommentService commentService, BrowseHistoryService browseHistoryService) {
        this.jdbcTemplate = jdbcTemplate;
        this.commentService = commentService;
        this.browseHistoryService = browseHistoryService;
    }

    public List<TopicItem> topics() {
        return jdbcTemplate.query("""
                SELECT id, name, description
                FROM topics
                WHERE status = 'ACTIVE'
                ORDER BY sort_no ASC, id ASC
                """, Rows.topicItem());
    }

    public PageResult<PostSummary> list(Long topicId, String keyword, String sort, int page, int pageSize, long userId) {
        StringBuilder where = new StringBuilder(" WHERE p.status = 'PUBLISHED'");
        StringBuilder countSql = new StringBuilder("SELECT COUNT(*) FROM posts p");
        StringBuilder querySql = new StringBuilder("""
                SELECT p.*, t.name AS topic_name, u.nickname, u.avatar_url,
                CASE WHEN f.id IS NULL THEN FALSE ELSE TRUE END AS favorited
                FROM posts p
                LEFT JOIN topics t ON t.id = p.topic_id
                JOIN users u ON u.id = p.user_id
                LEFT JOIN favorites f ON f.target_type = 'POST' AND f.target_id = p.id AND f.user_id = ?
                """);
        List<Object> args = new ArrayList<>();
        args.add(userId);
        if (topicId != null) {
            where.append(" AND p.topic_id = ?");
            args.add(topicId);
        }
        if (keyword != null && !keyword.isBlank()) {
            where.append(" AND (LOWER(p.title) LIKE LOWER(?) OR LOWER(p.content) LIKE LOWER(?))");
            String like = "%" + keyword.trim() + "%";
            args.add(like);
            args.add(like);
        }
        String orderBy = "hot".equalsIgnoreCase(sort)
                ? " ORDER BY p.like_count DESC, p.comment_count DESC, p.updated_at DESC"
                : " ORDER BY p.updated_at DESC";
        countSql.append(where);
        querySql.append(where).append(orderBy).append(" LIMIT ? OFFSET ?");
        long total = jdbcTemplate.queryForObject(countSql.toString(), Long.class, args.subList(1, args.size()).toArray());
        args.add(pageSize);
        args.add((long) (page - 1) * pageSize);
        List<PostSummary> items = jdbcTemplate.query(querySql.toString(), Rows.postSummary(userId), args.toArray());
        return new PageResult<>(items, total, page, pageSize);
    }

    public PostDetail detail(long id, long userId) {
        jdbcTemplate.update("UPDATE posts SET view_count = view_count + 1 WHERE id = ? AND status = 'PUBLISHED'", id);
        List<PostDetail> rows = jdbcTemplate.query("""
                SELECT p.*, t.name AS topic_name, u.nickname, u.avatar_url,
                CASE WHEN l.id IS NULL THEN FALSE ELSE TRUE END AS liked,
                CASE WHEN f.id IS NULL THEN FALSE ELSE TRUE END AS favorited
                FROM posts p
                LEFT JOIN topics t ON t.id = p.topic_id
                JOIN users u ON u.id = p.user_id
                LEFT JOIN likes l ON l.target_type = 'POST' AND l.target_id = p.id AND l.user_id = ?
                LEFT JOIN favorites f ON f.target_type = 'POST' AND f.target_id = p.id AND f.user_id = ?
                WHERE p.id = ? AND p.status = 'PUBLISHED'
                """, Rows.postDetail(), userId, userId, id);
        if (rows.isEmpty()) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "帖子不存在或已下架");
        }
        browseHistoryService.record(userId, "POST", id);
        PostDetail detail = rows.get(0);
        detail.comments().addAll(commentService.listForTarget("POST", id));
        return detail;
    }

    public PostDetail create(AuthUser user, CreatePostRequest request) {
        if (!topicExists(request.topicId())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "话题不存在");
        }
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement("""
                INSERT INTO posts(topic_id, user_id, title, cover_url, content, images, status)
                VALUES(?, ?, ?, ?, ?, ?, 'PUBLISHED')
                """, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, request.topicId());
            ps.setLong(2, user.id());
            ps.setString(3, request.title().trim());
            ps.setString(4, clean(request.coverUrl()));
            ps.setString(5, request.content().trim());
            ps.setString(6, joinImages(request.images()));
            return ps;
        }, keyHolder);
        return detail(generatedId(keyHolder), user.id());
    }

    public PostDetail draft(AuthUser user) {
        List<PostDetail> rows = jdbcTemplate.query("""
                SELECT p.*, t.name AS topic_name, u.nickname, u.avatar_url, FALSE AS favorited
                , FALSE AS liked
                FROM posts p
                LEFT JOIN topics t ON t.id = p.topic_id
                JOIN users u ON u.id = p.user_id
                WHERE p.user_id = ? AND p.status = 'DRAFT'
                ORDER BY p.updated_at DESC, p.id DESC
                LIMIT 1
                """, Rows.postDetail(), user.id());
        return rows.isEmpty() ? null : rows.get(0);
    }

    public PostDetail saveDraft(AuthUser user, PostDraftRequest request) {
        Long topicId = request.topicId() == null ? firstTopicId() : request.topicId();
        if (!topicExists(topicId)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "话题不存在");
        }
        PostDetail current = draft(user);
        String title = blankAsDefault(request.title(), "未命名草稿");
        String content = blankAsDefault(request.content(), "");
        if (current == null) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            Long finalTopicId = topicId;
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement("""
                    INSERT INTO posts(topic_id, user_id, title, cover_url, content, images, status)
                    VALUES(?, ?, ?, ?, ?, ?, 'DRAFT')
                    """, Statement.RETURN_GENERATED_KEYS);
                ps.setLong(1, finalTopicId);
                ps.setLong(2, user.id());
                ps.setString(3, title);
                ps.setString(4, clean(request.coverUrl()));
                ps.setString(5, content);
                ps.setString(6, joinImages(request.images()));
                return ps;
            }, keyHolder);
            return rawDetail(generatedId(keyHolder), user.id());
        }
        jdbcTemplate.update("""
                UPDATE posts
                SET topic_id = ?, title = ?, cover_url = ?, content = ?, images = ?, updated_at = CURRENT_TIMESTAMP
                WHERE id = ? AND user_id = ? AND status = 'DRAFT'
                """, topicId, title, clean(request.coverUrl()), content, joinImages(request.images()), current.id(), user.id());
        return rawDetail(current.id(), user.id());
    }

    public PostDetail publishDraft(AuthUser user) {
        PostDetail current = draft(user);
        if (current == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "暂无可发布草稿");
        }
        if (current.title().isBlank() || current.content().isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "请填写标题和正文后发布");
        }
        jdbcTemplate.update("""
                UPDATE posts SET status = 'PUBLISHED', updated_at = CURRENT_TIMESTAMP
                WHERE id = ? AND user_id = ? AND status = 'DRAFT'
                """, current.id(), user.id());
        return detail(current.id(), user.id());
    }

    public PostDetail update(AuthUser user, long postId, CreatePostRequest request) {
        ensureOwner(postId, user.id());
        if (!topicExists(request.topicId())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "话题不存在");
        }
        jdbcTemplate.update("""
                UPDATE posts
                SET topic_id = ?, title = ?, cover_url = ?, content = ?, images = ?, updated_at = CURRENT_TIMESTAMP
                WHERE id = ? AND user_id = ? AND status = 'PUBLISHED'
                """, request.topicId(), request.title().trim(), clean(request.coverUrl()), request.content().trim(),
                joinImages(request.images()), postId, user.id());
        return detail(postId, user.id());
    }

    public ActionState favorite(AuthUser user, long postId) {
        ensurePublished(postId);
        jdbcTemplate.update("INSERT IGNORE INTO favorites(user_id, target_type, target_id) VALUES(?, 'POST', ?)",
                user.id(), postId);
        refreshFavoriteCount(postId);
        return actionState(user.id(), postId);
    }

    public ActionState unfavorite(AuthUser user, long postId) {
        jdbcTemplate.update("DELETE FROM favorites WHERE user_id = ? AND target_type = 'POST' AND target_id = ?", user.id(), postId);
        refreshFavoriteCount(postId);
        return actionState(user.id(), postId);
    }

    public ActionState like(AuthUser user, long postId) {
        ensurePublished(postId);
        Integer exists = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM likes WHERE user_id = ? AND target_type = 'POST' AND target_id = ?
                """, Integer.class, user.id(), postId);
        if (exists != null && exists > 0) {
            jdbcTemplate.update("DELETE FROM likes WHERE user_id = ? AND target_type = 'POST' AND target_id = ?", user.id(), postId);
        } else {
            jdbcTemplate.update("INSERT INTO likes(user_id, target_type, target_id) VALUES(?, 'POST', ?)",
                    user.id(), postId);
        }
        jdbcTemplate.update("UPDATE posts SET like_count = (SELECT COUNT(*) FROM likes WHERE target_type = 'POST' AND target_id = ?) WHERE id = ?",
                postId, postId);
        return actionState(user.id(), postId);
    }

    public List<PostSummary> postsByUser(long userId) {
        return jdbcTemplate.query("""
                SELECT p.*, t.name AS topic_name, u.nickname, u.avatar_url,
                CASE WHEN f.id IS NULL THEN FALSE ELSE TRUE END AS favorited
                FROM posts p
                LEFT JOIN topics t ON t.id = p.topic_id
                JOIN users u ON u.id = p.user_id
                LEFT JOIN favorites f ON f.target_type = 'POST' AND f.target_id = p.id AND f.user_id = ?
                WHERE p.user_id = ?
                ORDER BY CASE WHEN p.status = 'DRAFT' THEN 0 ELSE 1 END, p.updated_at DESC
                """, Rows.postSummary(userId), userId, userId);
    }

    public List<PostSummary> favorites(long userId) {
        return jdbcTemplate.query("""
                SELECT p.*, t.name AS topic_name, u.nickname, u.avatar_url, TRUE AS favorited
                FROM favorites fav
                JOIN posts p ON p.id = fav.target_id AND fav.target_type = 'POST'
                LEFT JOIN topics t ON t.id = p.topic_id
                JOIN users u ON u.id = p.user_id
                WHERE fav.user_id = ? AND p.status = 'PUBLISHED'
                ORDER BY fav.created_at DESC
                """, Rows.postSummary(userId), userId);
    }

    private boolean topicExists(long topicId) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM topics WHERE id = ? AND status = 'ACTIVE'", Integer.class, topicId);
        return count != null && count > 0;
    }

    private void ensurePublished(long postId) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM posts WHERE id = ? AND status = 'PUBLISHED'", Integer.class, postId);
        if (count == null || count == 0) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "帖子不存在或已下架");
        }
    }

    private void ensureOwner(long postId, long userId) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM posts WHERE id = ? AND user_id = ? AND status = 'PUBLISHED'
                """, Integer.class, postId, userId);
        if (count == null || count == 0) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "帖子不存在或无权编辑");
        }
    }

    private void refreshFavoriteCount(long postId) {
        jdbcTemplate.update("UPDATE posts SET favorite_count = (SELECT COUNT(*) FROM favorites WHERE target_type = 'POST' AND target_id = ?) WHERE id = ?",
                postId, postId);
    }

    private PostDetail rawDetail(long id, long userId) {
        return jdbcTemplate.queryForObject("""
                SELECT p.*, t.name AS topic_name, u.nickname, u.avatar_url,
                CASE WHEN l.id IS NULL THEN FALSE ELSE TRUE END AS liked,
                CASE WHEN f.id IS NULL THEN FALSE ELSE TRUE END AS favorited
                FROM posts p
                LEFT JOIN topics t ON t.id = p.topic_id
                JOIN users u ON u.id = p.user_id
                LEFT JOIN likes l ON l.target_type = 'POST' AND l.target_id = p.id AND l.user_id = ?
                LEFT JOIN favorites f ON f.target_type = 'POST' AND f.target_id = p.id AND f.user_id = ?
                WHERE p.id = ?
                """, Rows.postDetail(), userId, userId, id);
    }

    private ActionState actionState(long userId, long postId) {
        return jdbcTemplate.queryForObject("""
                SELECT
                CASE WHEN l.id IS NULL THEN FALSE ELSE TRUE END AS liked,
                CASE WHEN f.id IS NULL THEN FALSE ELSE TRUE END AS favorited,
                p.like_count,
                p.favorite_count
                FROM posts p
                LEFT JOIN likes l ON l.target_type = 'POST' AND l.target_id = p.id AND l.user_id = ?
                LEFT JOIN favorites f ON f.target_type = 'POST' AND f.target_id = p.id AND f.user_id = ?
                WHERE p.id = ?
                """, (rs, rowNum) -> new ActionState(
                rs.getBoolean("liked"),
                rs.getBoolean("favorited"),
                rs.getInt("like_count"),
                rs.getInt("favorite_count")
        ), userId, userId, postId);
    }

    private Long firstTopicId() {
        return jdbcTemplate.queryForObject("""
                SELECT id FROM topics WHERE status = 'ACTIVE' ORDER BY sort_no ASC, id ASC LIMIT 1
                """, Long.class);
    }

    private String joinImages(List<String> images) {
        if (images == null || images.isEmpty()) {
            return "";
        }
        return String.join("\n", images.stream()
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .toList());
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private String blankAsDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private long generatedId(KeyHolder keyHolder) {
        Map<String, Object> keys = keyHolder.getKeys();
        if (keys != null && keys.get("id") instanceof Number id) {
            return id.longValue();
        }
        if (keys != null && keys.get("ID") instanceof Number id) {
            return id.longValue();
        }
        return keyHolder.getKey().longValue();
    }
}

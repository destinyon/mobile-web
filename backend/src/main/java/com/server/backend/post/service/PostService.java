package com.server.backend.post.service;

import com.server.backend.auth.dto.AuthUser;
import com.server.backend.comment.service.CommentService;
import com.server.backend.common.BusinessException;
import com.server.backend.common.PageResult;
import com.server.backend.common.Rows;
import com.server.backend.post.dto.CreatePostRequest;
import com.server.backend.post.dto.PostDetail;
import com.server.backend.post.dto.PostSummary;
import com.server.backend.post.dto.TopicItem;
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

    public PostService(JdbcTemplate jdbcTemplate, CommentService commentService) {
        this.jdbcTemplate = jdbcTemplate;
        this.commentService = commentService;
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
                CASE WHEN f.id IS NULL THEN FALSE ELSE TRUE END AS favorited
                FROM posts p
                LEFT JOIN topics t ON t.id = p.topic_id
                JOIN users u ON u.id = p.user_id
                LEFT JOIN favorites f ON f.target_type = 'POST' AND f.target_id = p.id AND f.user_id = ?
                WHERE p.id = ? AND p.status = 'PUBLISHED'
                """, Rows.postDetail(), userId, id);
        if (rows.isEmpty()) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "帖子不存在或已下架");
        }
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
                INSERT INTO posts(topic_id, user_id, title, content, images, status)
                VALUES(?, ?, ?, ?, ?, 'PUBLISHED')
                """, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, request.topicId());
            ps.setLong(2, user.id());
            ps.setString(3, request.title().trim());
            ps.setString(4, request.content().trim());
            ps.setString(5, joinImages(request.images()));
            return ps;
        }, keyHolder);
        return detail(generatedId(keyHolder), user.id());
    }

    public void favorite(AuthUser user, long postId) {
        ensurePublished(postId);
        jdbcTemplate.update("INSERT IGNORE INTO favorites(user_id, target_type, target_id) VALUES(?, 'POST', ?)",
                user.id(), postId);
        refreshFavoriteCount(postId);
    }

    public void unfavorite(AuthUser user, long postId) {
        jdbcTemplate.update("DELETE FROM favorites WHERE user_id = ? AND target_type = 'POST' AND target_id = ?", user.id(), postId);
        refreshFavoriteCount(postId);
    }

    public void like(AuthUser user, long postId) {
        ensurePublished(postId);
        jdbcTemplate.update("INSERT IGNORE INTO likes(user_id, target_type, target_id) VALUES(?, 'POST', ?)",
                user.id(), postId);
        jdbcTemplate.update("UPDATE posts SET like_count = (SELECT COUNT(*) FROM likes WHERE target_type = 'POST' AND target_id = ?) WHERE id = ?",
                postId, postId);
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
                ORDER BY p.created_at DESC
                """, Rows.postSummary(userId), userId, userId);
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

    private void refreshFavoriteCount(long postId) {
        jdbcTemplate.update("UPDATE posts SET favorite_count = (SELECT COUNT(*) FROM favorites WHERE target_type = 'POST' AND target_id = ?) WHERE id = ?",
                postId, postId);
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

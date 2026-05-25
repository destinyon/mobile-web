package com.server.backend.news.service;

import com.server.backend.auth.dto.AuthUser;
import com.server.backend.comment.service.CommentService;
import com.server.backend.common.BusinessException;
import com.server.backend.common.PageResult;
import com.server.backend.common.Rows;
import com.server.backend.news.dto.CreateNewsRequest;
import com.server.backend.news.dto.NewsDetail;
import com.server.backend.news.dto.NewsSummary;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

@Service
public class NewsService {
    private final JdbcTemplate jdbcTemplate;
    private final CommentService commentService;

    public NewsService(JdbcTemplate jdbcTemplate, CommentService commentService) {
        this.jdbcTemplate = jdbcTemplate;
        this.commentService = commentService;
    }

    public PageResult<NewsSummary> list(String keyword, Long categoryId, String sort, int page, int pageSize, long userId) {
        StringBuilder where = new StringBuilder(" WHERE n.status = 'PUBLISHED'");
        StringBuilder countSql = new StringBuilder("SELECT COUNT(*) FROM news n");
        StringBuilder querySql = new StringBuilder("""
                SELECT n.*, c.name AS category_name,
                CASE WHEN f.id IS NULL THEN FALSE ELSE TRUE END AS favorited
                FROM news n
                LEFT JOIN categories c ON c.id = n.category_id
                LEFT JOIN favorites f ON f.target_type = 'NEWS' AND f.target_id = n.id AND f.user_id = ?
                """);
        List<Object> args = new ArrayList<>();
        args.add(userId);
        if (keyword != null && !keyword.isBlank()) {
            where.append(" AND (LOWER(n.title) LIKE LOWER(?) OR LOWER(n.summary) LIKE LOWER(?))");
            String like = "%" + keyword.trim() + "%";
            args.add(like);
            args.add(like);
        }
        if (categoryId != null) {
            where.append(" AND n.category_id = ?");
            args.add(categoryId);
        }
        String orderBy = "hot".equalsIgnoreCase(sort) ? " ORDER BY n.view_count DESC, n.updated_at DESC" : " ORDER BY n.updated_at DESC";
        countSql.append(where);
        querySql.append(where).append(orderBy).append(" LIMIT ? OFFSET ?");
        long total = jdbcTemplate.queryForObject(countSql.toString(), Long.class, args.subList(1, args.size()).toArray());
        args.add(pageSize);
        args.add((long) (page - 1) * pageSize);
        List<NewsSummary> items = jdbcTemplate.query(querySql.toString(), Rows.newsSummary(userId), args.toArray());
        return new PageResult<>(items, total, page, pageSize);
    }

    public NewsDetail detail(long id, long userId) {
        jdbcTemplate.update("UPDATE news SET view_count = view_count + 1 WHERE id = ? AND status = 'PUBLISHED'", id);
        List<NewsDetail> rows = jdbcTemplate.query("""
                SELECT n.*, c.name AS category_name,
                CASE WHEN f.id IS NULL THEN FALSE ELSE TRUE END AS favorited
                FROM news n
                LEFT JOIN categories c ON c.id = n.category_id
                LEFT JOIN favorites f ON f.target_type = 'NEWS' AND f.target_id = n.id AND f.user_id = ?
                WHERE n.id = ? AND n.status = 'PUBLISHED'
                """, Rows.newsDetail(), userId, id);
        if (rows.isEmpty()) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "资讯不存在或已下架");
        }
        NewsDetail detail = rows.get(0);
        detail.comments().addAll(commentService.listForTarget("NEWS", id));
        return detail;
    }

    public NewsDetail create(AuthUser user, CreateNewsRequest request) {
        String author = jdbcTemplate.queryForObject("SELECT nickname FROM users WHERE id = ?", String.class, user.id());
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement("""
                INSERT INTO news(category_id, user_id, title, cover_url, summary, author, content, media_url, media_type, status)
                VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, 'PUBLISHED')
                """, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, request.categoryId());
            ps.setLong(2, user.id());
            ps.setString(3, request.title().trim());
            ps.setString(4, request.coverUrl().trim());
            ps.setString(5, request.summary());
            ps.setString(6, author);
            ps.setString(7, request.content().trim());
            ps.setString(8, request.mediaUrl());
            ps.setString(9, request.mediaType() == null ? "IMAGE" : request.mediaType());
            return ps;
        }, keyHolder);
        return detail(keyHolder.getKey().longValue(), user.id());
    }

    public void favorite(AuthUser user, long newsId) {
        jdbcTemplate.update("INSERT IGNORE INTO favorites(user_id, target_type, target_id) VALUES(?, 'NEWS', ?)",
                user.id(), newsId);
        refreshFavoriteCount(newsId);
    }

    public void unfavorite(AuthUser user, long newsId) {
        jdbcTemplate.update("DELETE FROM favorites WHERE user_id = ? AND target_type = 'NEWS' AND target_id = ?", user.id(), newsId);
        refreshFavoriteCount(newsId);
    }

    public void like(AuthUser user, long newsId) {
        jdbcTemplate.update("INSERT IGNORE INTO likes(user_id, target_type, target_id) VALUES(?, 'NEWS', ?)",
                user.id(), newsId);
        jdbcTemplate.update("UPDATE news SET like_count = (SELECT COUNT(*) FROM likes WHERE target_type = 'NEWS' AND target_id = ?) WHERE id = ?",
                newsId, newsId);
    }

    public List<NewsSummary> favorites(long userId) {
        return jdbcTemplate.query("""
                SELECT n.*, c.name AS category_name, TRUE AS favorited
                FROM favorites fav
                JOIN news n ON n.id = fav.target_id AND fav.target_type = 'NEWS'
                LEFT JOIN categories c ON c.id = n.category_id
                WHERE fav.user_id = ?
                ORDER BY fav.created_at DESC
                """, Rows.newsSummary(userId), userId);
    }

    public List<NewsSummary> posts(long userId) {
        return jdbcTemplate.query("""
                SELECT n.*, c.name AS category_name,
                CASE WHEN f.id IS NULL THEN FALSE ELSE TRUE END AS favorited
                FROM news n
                LEFT JOIN categories c ON c.id = n.category_id
                LEFT JOIN favorites f ON f.target_type = 'NEWS' AND f.target_id = n.id AND f.user_id = ?
                WHERE n.user_id = ?
                ORDER BY n.created_at DESC
                """, Rows.newsSummary(userId), userId, userId);
    }

    private void refreshFavoriteCount(long newsId) {
        jdbcTemplate.update("UPDATE news SET favorite_count = (SELECT COUNT(*) FROM favorites WHERE target_type = 'NEWS' AND target_id = ?) WHERE id = ?",
                newsId, newsId);
    }
}

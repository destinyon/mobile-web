package com.server.backend.admin.service;

import com.server.backend.admin.dto.AdminCategoryStat;
import com.server.backend.admin.dto.AdminNewsRankingItem;
import com.server.backend.admin.dto.AdminSummary;
import com.server.backend.admin.dto.AdminUserDetail;
import com.server.backend.admin.dto.AdminUserItem;
import com.server.backend.common.BusinessException;
import com.server.backend.common.PageResult;
import com.server.backend.common.Rows;
import com.server.backend.news.dto.NewsDetail;
import com.server.backend.news.dto.NewsSummary;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AdminDashboardService {
    private final JdbcTemplate jdbcTemplate;

    public AdminDashboardService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public AdminSummary summary() {
        List<AdminCategoryStat> categoryStats = jdbcTemplate.query("""
                SELECT c.id AS category_id, c.name AS category_name,
                    COUNT(n.id) AS news_count,
                    COALESCE(SUM(n.view_count), 0) AS total_views,
                    COALESCE(SUM(n.like_count), 0) AS total_likes,
                    COALESCE(SUM(n.favorite_count), 0) AS total_favorites
                FROM categories c
                LEFT JOIN news n ON n.category_id = c.id
                WHERE c.status = 'ACTIVE'
                GROUP BY c.id, c.name, c.sort_no
                ORDER BY c.sort_no ASC, c.id ASC
                """, (rs, rowNum) -> new AdminCategoryStat(
                rs.getLong("category_id"),
                rs.getString("category_name"),
                rs.getLong("news_count"),
                rs.getLong("total_views"),
                rs.getLong("total_likes"),
                rs.getLong("total_favorites")
        ));
        return new AdminSummary(
                count("SELECT COUNT(*) FROM users"),
                count("SELECT COUNT(*) FROM users WHERE status = 'ACTIVE'"),
                count("SELECT COUNT(*) FROM news"),
                count("SELECT COUNT(*) FROM news WHERE status = 'PUBLISHED'"),
                count("SELECT COUNT(*) FROM news WHERE status <> 'PUBLISHED'"),
                count("SELECT COUNT(*) FROM posts"),
                count("SELECT COUNT(*) FROM comments WHERE status = 'PUBLISHED'"),
                count("SELECT COALESCE(SUM(view_count), 0) FROM news"),
                count("SELECT COALESCE(SUM(like_count), 0) FROM news"),
                count("SELECT COALESCE(SUM(favorite_count), 0) FROM news"),
                categoryStats
        );
    }

    public NewsDetail newsDetail(long id) {
        List<NewsDetail> rows = jdbcTemplate.query("""
                SELECT n.*, c.name AS category_name,
                    FALSE AS liked,
                    FALSE AS favorited
                FROM news n
                LEFT JOIN categories c ON c.id = n.category_id
                WHERE n.id = ?
                """, Rows.newsDetail(), id);
        if (rows.isEmpty()) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "资讯不存在");
        }
        return rows.get(0);
    }

    public PageResult<NewsSummary> news(String keyword, Long categoryId, int page, int pageSize) {
        int safePage = Math.max(1, page);
        int safePageSize = Math.max(1, Math.min(pageSize, 100));
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        List<Object> args = new ArrayList<>();
        if (keyword != null && !keyword.isBlank()) {
            where.append(" AND (LOWER(n.title) LIKE LOWER(?) OR LOWER(n.summary) LIKE LOWER(?) OR LOWER(n.author) LIKE LOWER(?))");
            String like = "%" + keyword.trim() + "%";
            args.add(like);
            args.add(like);
            args.add(like);
        }
        if (categoryId != null) {
            where.append(" AND n.category_id = ?");
            args.add(categoryId);
        }

        long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM news n" + where, Long.class, args.toArray());
        List<Object> queryArgs = new ArrayList<>(args);
        queryArgs.add(safePageSize);
        queryArgs.add((long) (safePage - 1) * safePageSize);
        List<NewsSummary> items = jdbcTemplate.query("""
                SELECT n.*, c.name AS category_name, FALSE AS favorited
                FROM news n
                LEFT JOIN categories c ON c.id = n.category_id
                """.stripTrailing() + where + " ORDER BY n.updated_at DESC, n.id DESC LIMIT ? OFFSET ?",
                Rows.newsSummary(0),
                queryArgs.toArray());
        return new PageResult<>(items, total, safePage, safePageSize);
    }

    public List<AdminNewsRankingItem> newsRankings(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 50));
        return jdbcTemplate.query("""
                SELECT n.id, n.title, n.cover_url, c.name AS category_name,
                    n.view_count, n.like_count, n.favorite_count, n.comment_count,
                    (n.like_count + n.favorite_count) AS heat_score
                FROM news n
                LEFT JOIN categories c ON c.id = n.category_id
                WHERE n.status = 'PUBLISHED'
                ORDER BY heat_score DESC, n.view_count DESC, n.updated_at DESC, n.id DESC
                LIMIT ?
                """, (rs, rowNum) -> new AdminNewsRankingItem(
                rs.getLong("id"),
                rs.getString("title"),
                rs.getString("category_name"),
                rs.getString("cover_url"),
                rs.getInt("view_count"),
                rs.getInt("like_count"),
                rs.getInt("favorite_count"),
                rs.getInt("comment_count"),
                rs.getInt("heat_score")
        ), safeLimit);
    }

    public PageResult<AdminUserItem> users(String keyword, int page, int pageSize) {
        int safePage = Math.max(1, page);
        int safePageSize = Math.max(1, Math.min(pageSize, 100));
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        List<Object> args = new ArrayList<>();
        if (keyword != null && !keyword.isBlank()) {
            where.append(" AND (LOWER(u.nickname) LIKE LOWER(?) OR LOWER(u.phone) LIKE LOWER(?) OR LOWER(u.role) LIKE LOWER(?))");
            String like = "%" + keyword.trim() + "%";
            args.add(like);
            args.add(like);
            args.add(like);
        }
        long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users u" + where, Long.class, args.toArray());
        List<Object> queryArgs = new ArrayList<>(args);
        queryArgs.add(safePageSize);
        queryArgs.add((long) (safePage - 1) * safePageSize);
        List<AdminUserItem> items = jdbcTemplate.query("""
                SELECT u.*,
                    (SELECT COUNT(*) FROM posts p WHERE p.user_id = u.id) AS post_count,
                    (SELECT COUNT(*) FROM comments c WHERE c.user_id = u.id AND c.status = 'PUBLISHED') AS comment_count,
                    (SELECT COUNT(*) FROM favorites f WHERE f.user_id = u.id) AS favorite_count
                FROM users u
                """.stripTrailing() + where + " ORDER BY u.updated_at DESC, u.id DESC LIMIT ? OFFSET ?",
                (rs, rowNum) -> new AdminUserItem(
                        rs.getLong("id"),
                        rs.getString("nickname"),
                        rs.getString("avatar_url"),
                        rs.getString("phone"),
                        rs.getString("role"),
                        rs.getString("status"),
                        rs.getInt("post_count"),
                        rs.getInt("comment_count"),
                        rs.getInt("favorite_count"),
                        rs.getTimestamp("created_at").toLocalDateTime(),
                        rs.getTimestamp("updated_at").toLocalDateTime()
                ),
                queryArgs.toArray());
        return new PageResult<>(items, total, safePage, safePageSize);
    }

    public AdminUserDetail userDetail(long id) {
        List<AdminUserDetail> rows = jdbcTemplate.query("""
                SELECT u.*,
                    (SELECT COUNT(*) FROM posts p WHERE p.user_id = u.id) AS post_count,
                    (SELECT COUNT(*) FROM comments c WHERE c.user_id = u.id) AS comment_count,
                    (SELECT COUNT(*) FROM favorites f WHERE f.user_id = u.id) AS favorite_count,
                    (SELECT COUNT(*) FROM likes l WHERE l.user_id = u.id) AS like_count,
                    (SELECT COUNT(*) FROM browse_history b WHERE b.user_id = u.id) AS browse_count
                FROM users u
                WHERE u.id = ?
                """, (rs, rowNum) -> new AdminUserDetail(
                rs.getLong("id"),
                rs.getString("nickname"),
                rs.getString("avatar_url"),
                rs.getString("phone"),
                rs.getObject("age", Integer.class),
                rs.getObject("play_years", Integer.class),
                rs.getString("gender"),
                rs.getString("role"),
                rs.getString("status"),
                rs.getInt("post_count"),
                rs.getInt("comment_count"),
                rs.getInt("favorite_count"),
                rs.getInt("like_count"),
                rs.getInt("browse_count"),
                rs.getTimestamp("created_at").toLocalDateTime(),
                rs.getTimestamp("updated_at").toLocalDateTime()
        ), id);
        if (rows.isEmpty()) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "用户不存在");
        }
        return rows.get(0);
    }

    private long count(String sql) {
        Long value = jdbcTemplate.queryForObject(sql, Long.class);
        return value == null ? 0 : value;
    }
}

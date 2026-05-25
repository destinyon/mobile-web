package com.server.backend.user.service;

import com.server.backend.user.dto.BrowseHistoryItem;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BrowseHistoryService {
    private final JdbcTemplate jdbcTemplate;

    public BrowseHistoryService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void record(long userId, String targetType, long targetId) {
        if (userId <= 0) {
            return;
        }
        jdbcTemplate.update("""
                INSERT INTO browse_history(user_id, target_type, target_id, viewed_at)
                VALUES(?, ?, ?, CURRENT_TIMESTAMP)
                ON DUPLICATE KEY UPDATE viewed_at = CURRENT_TIMESTAMP
                """, userId, targetType, targetId);
    }

    public List<BrowseHistoryItem> list(long userId) {
        return jdbcTemplate.query("""
                SELECT h.id, h.target_type, h.target_id, h.viewed_at,
                COALESCE(n.title, p.title) AS title,
                COALESCE(n.cover_url, p.cover_url) AS cover_url,
                COALESCE(n.author, pu.nickname) AS owner_name
                FROM browse_history h
                LEFT JOIN news n ON h.target_type = 'NEWS' AND n.id = h.target_id AND n.status = 'PUBLISHED'
                LEFT JOIN posts p ON h.target_type = 'POST' AND p.id = h.target_id AND p.status = 'PUBLISHED'
                LEFT JOIN users pu ON p.user_id = pu.id
                WHERE h.user_id = ? AND (n.id IS NOT NULL OR p.id IS NOT NULL)
                ORDER BY h.viewed_at DESC
                LIMIT 100
                """, (rs, rowNum) -> new BrowseHistoryItem(
                rs.getLong("id"),
                rs.getString("target_type"),
                rs.getLong("target_id"),
                rs.getString("title"),
                rs.getString("cover_url"),
                rs.getString("owner_name"),
                rs.getTimestamp("viewed_at").toLocalDateTime()
        ), userId);
    }
}

package com.server.backend.comment.service;

import com.server.backend.auth.dto.AuthUser;
import com.server.backend.comment.dto.CommentNode;
import com.server.backend.comment.dto.CreateCommentRequest;
import com.server.backend.comment.dto.UserCommentItem;
import com.server.backend.common.Rows;
import com.server.backend.common.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CommentService {
    private final JdbcTemplate jdbcTemplate;

    public CommentService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<CommentNode> listForTarget(String targetType, long targetId) {
        List<CommentNode> flat = jdbcTemplate.query("""
                SELECT c.*, u.nickname, u.avatar_url
                FROM comments c
                JOIN users u ON u.id = c.user_id
                WHERE c.target_type = ? AND c.target_id = ? AND c.status = 'PUBLISHED'
                ORDER BY c.created_at ASC
                """, Rows.commentNode(), targetType, targetId);
        return buildTree(flat);
    }

    public CommentNode create(AuthUser user, CreateCommentRequest request) {
        String targetType = request.targetType().toUpperCase();
        validateTarget(targetType, request.targetId(), request.parentId());
        jdbcTemplate.update("""
                INSERT INTO comments(target_type, target_id, user_id, parent_id, content)
                VALUES(?, ?, ?, ?, ?)
                """,
                targetType,
                request.targetId(),
                user.id(),
                request.parentId(),
                request.content().trim());
        jdbcTemplate.update("UPDATE news SET comment_count = comment_count + 1, updated_at = CURRENT_TIMESTAMP WHERE id = ? AND ? = 'NEWS'",
                request.targetId(), targetType);
        jdbcTemplate.update("UPDATE posts SET comment_count = comment_count + 1, updated_at = CURRENT_TIMESTAMP WHERE id = ? AND ? = 'POST'",
                request.targetId(), targetType);
        Long commentId = jdbcTemplate.queryForObject("SELECT MAX(id) FROM comments WHERE user_id = ?", Long.class, user.id());
        return jdbcTemplate.queryForObject("""
                SELECT c.*, u.nickname, u.avatar_url
                FROM comments c JOIN users u ON u.id = c.user_id
                WHERE c.id = ?
                """, Rows.commentNode(), commentId);
    }

    public List<UserCommentItem> listByUser(long userId) {
        return jdbcTemplate.query("""
                SELECT c.id, c.target_type, c.target_id, c.parent_id, c.content, c.created_at,
                parent.content AS parent_content,
                COALESCE(n.title, p.title) AS target_title,
                COALESCE(n.cover_url, p.cover_url) AS target_cover_url,
                COALESCE(n.author, pu.nickname) AS target_owner_name
                FROM comments c
                LEFT JOIN comments parent ON parent.id = c.parent_id
                LEFT JOIN news n ON c.target_type = 'NEWS' AND n.id = c.target_id
                LEFT JOIN posts p ON c.target_type = 'POST' AND p.id = c.target_id
                LEFT JOIN users pu ON p.user_id = pu.id
                WHERE c.user_id = ? AND c.status = 'PUBLISHED'
                ORDER BY c.created_at DESC
                """, (rs, rowNum) -> new UserCommentItem(
                rs.getLong("id"),
                rs.getString("target_type"),
                rs.getLong("target_id"),
                rs.getString("target_title"),
                rs.getString("target_cover_url"),
                rs.getString("target_owner_name"),
                rs.getObject("parent_id", Long.class),
                rs.getString("parent_content"),
                rs.getString("content"),
                rs.getTimestamp("created_at").toLocalDateTime()
        ), userId);
    }

    private List<CommentNode> buildTree(List<CommentNode> flat) {
        Map<Long, CommentNode> byId = new LinkedHashMap<>();
        List<CommentNode> roots = new ArrayList<>();
        flat.forEach(item -> byId.put(item.id(), item));
        for (CommentNode node : flat) {
            if (node.parentId() == null || !byId.containsKey(node.parentId())) {
                roots.add(node);
            } else {
                byId.get(node.parentId()).replies().add(node);
            }
        }
        return roots;
    }

    private void validateTarget(String targetType, long targetId, Long parentId) {
        if (!"NEWS".equals(targetType) && !"POST".equals(targetType)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "评论对象类型不正确");
        }
        String table = "NEWS".equals(targetType) ? "news" : "posts";
        Integer targetCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + table + " WHERE id = ? AND status = 'PUBLISHED'",
                Integer.class,
                targetId);
        if (targetCount == null || targetCount == 0) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "评论对象不存在或已下架");
        }
        if (parentId == null) {
            return;
        }
        Integer parentCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM comments
                WHERE id = ? AND target_type = ? AND target_id = ? AND status = 'PUBLISHED'
                """, Integer.class, parentId, targetType, targetId);
        if (parentCount == null || parentCount == 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "回复的评论不存在");
        }
    }
}

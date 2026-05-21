package com.server.backend.comment;

import com.server.backend.auth.AuthUser;
import com.server.backend.common.Rows;
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
        jdbcTemplate.update("""
                INSERT INTO comments(target_type, target_id, user_id, parent_id, content)
                VALUES(?, ?, ?, ?, ?)
                """, request.targetType().toUpperCase(), request.targetId(), user.id(), request.parentId(), request.content().trim());
        jdbcTemplate.update("UPDATE news SET comment_count = comment_count + 1, updated_at = CURRENT_TIMESTAMP WHERE id = ? AND ? = 'NEWS'",
                request.targetId(), request.targetType().toUpperCase());
        Long id = jdbcTemplate.queryForObject("SELECT MAX(id) FROM comments WHERE user_id = ?", Long.class, user.id());
        return jdbcTemplate.queryForObject("""
                SELECT c.*, u.nickname, u.avatar_url
                FROM comments c JOIN users u ON u.id = c.user_id
                WHERE c.id = ?
                """, Rows.commentNode(), id);
    }

    public List<CommentNode> listByUser(long userId) {
        return jdbcTemplate.query("""
                SELECT c.*, u.nickname, u.avatar_url
                FROM comments c JOIN users u ON u.id = c.user_id
                WHERE c.user_id = ? AND c.status = 'PUBLISHED'
                ORDER BY c.created_at DESC
                """, Rows.commentNode(), userId);
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
}

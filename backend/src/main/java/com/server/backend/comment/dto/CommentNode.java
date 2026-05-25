package com.server.backend.comment.dto;

import java.time.LocalDateTime;
import java.util.List;

public record CommentNode(
        long id,
        String targetType,
        long targetId,
        Long parentId,
        long userId,
        String nickname,
        String avatarUrl,
        String content,
        LocalDateTime createdAt,
        List<CommentNode> replies
) {
}

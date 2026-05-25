package com.server.backend.comment.dto;

import java.time.LocalDateTime;

public record UserCommentItem(
        long id,
        String targetType,
        long targetId,
        String targetTitle,
        String targetCoverUrl,
        String targetOwnerName,
        Long parentId,
        String parentContent,
        String content,
        LocalDateTime createdAt
) {
}

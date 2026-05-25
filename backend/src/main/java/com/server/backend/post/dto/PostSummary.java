package com.server.backend.post.dto;

import java.time.LocalDateTime;
import java.util.List;

public record PostSummary(
        long id,
        long topicId,
        String topicName,
        long userId,
        String nickname,
        String avatarUrl,
        String title,
        String coverUrl,
        String content,
        List<String> images,
        int viewCount,
        int likeCount,
        int favoriteCount,
        int commentCount,
        boolean favorited,
        String status,
        LocalDateTime updatedAt
) {
}

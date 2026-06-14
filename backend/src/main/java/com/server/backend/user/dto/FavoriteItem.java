package com.server.backend.user.dto;

import java.time.LocalDateTime;
import java.util.List;

public record FavoriteItem(
        long id,
        String feedType,
        String categoryName,
        String topicName,
        String title,
        String coverUrl,
        String summary,
        String content,
        List<String> images,
        String author,
        String nickname,
        int viewCount,
        int likeCount,
        int favoriteCount,
        int commentCount,
        boolean favorited,
        LocalDateTime updatedAt
) {
}

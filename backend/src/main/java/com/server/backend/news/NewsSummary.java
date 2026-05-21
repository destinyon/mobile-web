package com.server.backend.news;

import java.time.LocalDateTime;

public record NewsSummary(
        long id,
        long categoryId,
        String categoryName,
        String title,
        String coverUrl,
        String summary,
        String author,
        int viewCount,
        int likeCount,
        int favoriteCount,
        int commentCount,
        boolean favorited,
        LocalDateTime updatedAt
) {
}

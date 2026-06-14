package com.server.backend.admin.dto;

public record AdminNewsRankingItem(
        String targetType,
        long id,
        String title,
        String categoryName,
        String coverUrl,
        int viewCount,
        int likeCount,
        int favoriteCount,
        int commentCount,
        int heatScore
) {
}

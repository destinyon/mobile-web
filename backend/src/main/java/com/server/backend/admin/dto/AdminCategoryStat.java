package com.server.backend.admin.dto;

public record AdminCategoryStat(
        long categoryId,
        String categoryName,
        long newsCount,
        long totalViews,
        long totalLikes,
        long totalFavorites
) {
}

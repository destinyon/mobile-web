package com.server.backend.admin.dto;

import java.util.List;

public record AdminSummary(
        long userCount,
        long activeUserCount,
        long newsCount,
        long publishedNewsCount,
        long offlineNewsCount,
        long postCount,
        long commentCount,
        long totalViews,
        long totalLikes,
        long totalFavorites,
        List<AdminCategoryStat> categoryStats
) {
}

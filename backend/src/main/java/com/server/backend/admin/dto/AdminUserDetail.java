package com.server.backend.admin.dto;

import java.time.LocalDateTime;

public record AdminUserDetail(
        long id,
        String nickname,
        String avatarUrl,
        String phone,
        Integer age,
        Integer playYears,
        String gender,
        String role,
        String status,
        int postCount,
        int commentCount,
        int favoriteCount,
        int likeCount,
        int browseCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

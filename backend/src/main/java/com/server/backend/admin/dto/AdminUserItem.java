package com.server.backend.admin.dto;

import java.time.LocalDateTime;

public record AdminUserItem(
        long id,
        String nickname,
        String avatarUrl,
        String phone,
        String email,
        String role,
        String status,
        int postCount,
        int commentCount,
        int favoriteCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

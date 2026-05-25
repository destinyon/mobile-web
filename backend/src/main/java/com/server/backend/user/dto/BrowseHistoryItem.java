package com.server.backend.user.dto;

import java.time.LocalDateTime;

public record BrowseHistoryItem(
        long id,
        String targetType,
        long targetId,
        String title,
        String coverUrl,
        String ownerName,
        LocalDateTime viewedAt
) {
}

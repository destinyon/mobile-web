package com.server.backend.news.netease;

import java.time.LocalDateTime;

public record NeteaseNewsDetail(
        String sourceId,
        String title,
        String url,
        String author,
        String summary,
        String coverUrl,
        String content,
        LocalDateTime publishedAt
) {
}

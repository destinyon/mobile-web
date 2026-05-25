package com.server.backend.news.netease;

public record NeteaseNewsItem(
        String sourceId,
        String title,
        String url
) {
}

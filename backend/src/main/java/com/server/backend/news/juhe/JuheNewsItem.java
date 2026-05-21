package com.server.backend.news.juhe;

public record JuheNewsItem(
        String uniquekey,
        String title,
        String date,
        String category,
        String authorName,
        String url,
        String thumbnailPicS,
        String thumbnailPicS02,
        String thumbnailPicS03,
        String isContent
) {
}

package com.server.backend.news.dto;

import com.server.backend.comment.dto.CommentNode;

import java.time.LocalDateTime;
import java.util.List;

public record NewsDetail(
        long id,
        long categoryId,
        String categoryName,
        String title,
        String coverUrl,
        String summary,
        String author,
        String content,
        String mediaUrl,
        String mediaType,
        int viewCount,
        int likeCount,
        int favoriteCount,
        int commentCount,
        boolean liked,
        boolean favorited,
        LocalDateTime updatedAt,
        List<CommentNode> comments
) {
}

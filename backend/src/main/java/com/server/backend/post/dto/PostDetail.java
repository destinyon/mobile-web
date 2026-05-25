package com.server.backend.post.dto;

import com.server.backend.comment.dto.CommentNode;

import java.time.LocalDateTime;
import java.util.List;

public record PostDetail(
        long id,
        long topicId,
        String topicName,
        long userId,
        String nickname,
        String avatarUrl,
        String title,
        String content,
        List<String> images,
        int viewCount,
        int likeCount,
        int favoriteCount,
        int commentCount,
        boolean favorited,
        String status,
        LocalDateTime updatedAt,
        List<CommentNode> comments
) {
}

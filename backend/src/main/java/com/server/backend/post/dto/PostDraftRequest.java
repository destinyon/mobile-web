package com.server.backend.post.dto;

import jakarta.validation.constraints.Size;

import java.util.List;

public record PostDraftRequest(
        Long topicId,
        @Size(max = 120) String title,
        @Size(max = 512) String coverUrl,
        @Size(max = 5000) String content,
        @Size(max = 9)
        List<@Size(max = 512) String> images
) {
}

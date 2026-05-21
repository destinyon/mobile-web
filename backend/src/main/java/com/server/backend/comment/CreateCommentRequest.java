package com.server.backend.comment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateCommentRequest(
        @NotBlank String targetType,
        @NotNull Long targetId,
        Long parentId,
        @NotBlank @Size(max = 600) String content
) {
}

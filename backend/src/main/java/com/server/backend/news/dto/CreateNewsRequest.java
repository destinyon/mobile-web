package com.server.backend.news.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateNewsRequest(
        @NotNull Long categoryId,
        @NotBlank @Size(max = 500) String title,
        @NotBlank @Size(max = 512) String coverUrl,
        @Size(max = 500) String summary,
        @NotBlank String content,
        String mediaUrl,
        String mediaType
) {
}

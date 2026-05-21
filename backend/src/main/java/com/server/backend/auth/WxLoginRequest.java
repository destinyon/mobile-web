package com.server.backend.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WxLoginRequest(
        @NotBlank String code,
        @Size(max = 80) String nickname,
        @Size(max = 512) String avatarUrl
) {
}

package com.server.backend.user.dto;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(max = 80) String nickname,
        @Size(max = 512) String avatarUrl,
        @Size(max = 40) String phone,
        Integer age,
        Integer playYears,
        @Size(max = 20) String gender
) {
}

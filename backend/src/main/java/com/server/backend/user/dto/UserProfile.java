package com.server.backend.user.dto;

public record UserProfile(
        long id,
        String nickname,
        String avatarUrl,
        String phone,
        String email,
        Integer age,
        Integer playYears,
        String gender,
        String role
) {
}

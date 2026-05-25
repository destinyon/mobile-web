package com.server.backend.auth.dto;

import com.server.backend.user.dto.UserProfile;

public record LoginResult(String token, UserProfile user) {
}

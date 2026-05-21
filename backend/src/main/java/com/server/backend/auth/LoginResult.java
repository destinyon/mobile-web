package com.server.backend.auth;

import com.server.backend.user.UserProfile;

public record LoginResult(String token, UserProfile user) {
}

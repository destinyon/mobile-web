package com.server.backend.auth.dto;

public record EmailCodeResult(
        String email,
        long validTime,
        long interval,
        String debugCode
) {
}

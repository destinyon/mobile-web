package com.server.backend.auth.service;

import com.server.backend.auth.dto.AuthUser;
import com.server.backend.common.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

@Service
public class AuthService {
    private final String secret;

    public AuthService(@Value("${app.jwt-secret}") String secret) {
        this.secret = secret;
    }

    public String issueToken(long userId, String role) {
        String payload = userId + ":" + role + ":" + Instant.now().plusSeconds(86400 * 7).getEpochSecond();
        String signature = sign(payload);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8)) + "." + signature;
    }

    public AuthUser requireUser(String authorization) {
        AuthUser user = parse(authorization);
        if (user == null) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "请先登录");
        }
        return user;
    }

    public AuthUser parse(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        String token = authorization.substring(7);
        String[] parts = token.split("\\.");
        if (parts.length != 2) {
            return null;
        }
        String payload = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
        if (!sign(payload).equals(parts[1])) {
            return null;
        }
        String[] fields = payload.split(":");
        if (fields.length != 3 || Long.parseLong(fields[2]) < Instant.now().getEpochSecond()) {
            return null;
        }
        return new AuthUser(Long.parseLong(fields[0]), fields[1]);
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("token sign failed", ex);
        }
    }
}

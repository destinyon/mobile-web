package com.server.backend.auth.service;

import com.server.backend.auth.dto.EmailCodeResult;
import com.server.backend.auth.dto.EmailLoginRequest;
import com.server.backend.auth.dto.LoginResult;
import com.server.backend.auth.dto.SendEmailCodeRequest;
import com.server.backend.common.BusinessException;
import com.server.backend.user.dto.UserProfile;
import com.server.backend.user.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
public class EmailAuthService {
    private final JdbcTemplate jdbcTemplate;
    private final EmailCodeSender emailCodeSender;
    private final AuthService authService;
    private final UserService userService;
    private final String secret;
    private final int codeLength;
    private final long validSeconds;
    private final long intervalSeconds;
    private final boolean debug;
    private final SecureRandom random = new SecureRandom();

    public EmailAuthService(
            JdbcTemplate jdbcTemplate,
            EmailCodeSender emailCodeSender,
            AuthService authService,
            UserService userService,
            @Value("${app.jwt-secret}") String secret,
            @Value("${app.email.code-length:${EMAIL_CODE_LENGTH:6}}") int codeLength,
            @Value("${app.email.valid-time-seconds:${EMAIL_VALID_TIME_SECONDS:300}}") long validSeconds,
            @Value("${app.email.interval-seconds:${EMAIL_INTERVAL_SECONDS:60}}") long intervalSeconds,
            @Value("${app.email.debug:${EMAIL_DEBUG:false}}") boolean debug) {
        this.jdbcTemplate = jdbcTemplate;
        this.emailCodeSender = emailCodeSender;
        this.authService = authService;
        this.userService = userService;
        this.secret = secret;
        this.codeLength = codeLength;
        this.validSeconds = validSeconds;
        this.intervalSeconds = intervalSeconds;
        this.debug = debug;
    }

    public EmailCodeResult sendCode(SendEmailCodeRequest request) {
        String email = normalizeEmail(request.email());
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lastSentAt = jdbcTemplate.query("""
                SELECT last_sent_at FROM email_login_codes WHERE email = ? ORDER BY id DESC LIMIT 1
                """, rs -> rs.next() ? rs.getTimestamp("last_sent_at").toLocalDateTime() : null, email);
        if (lastSentAt != null && lastSentAt.plusSeconds(intervalSeconds).isAfter(now)) {
            throw new BusinessException(HttpStatus.TOO_MANY_REQUESTS, "验证码发送太频繁");
        }

        String code = nextCode();
        emailCodeSender.send(email, code, validSeconds);
        jdbcTemplate.update("DELETE FROM email_login_codes WHERE email = ?", email);
        jdbcTemplate.update("""
                INSERT INTO email_login_codes(email, code_hash, expires_at, last_sent_at, verified_at)
                VALUES(?, ?, ?, ?, NULL)
                """, email, hash(email, code), now.plusSeconds(validSeconds), now);
        return new EmailCodeResult(email, validSeconds, intervalSeconds, debug ? code : null);
    }

    public LoginResult login(EmailLoginRequest request) {
        String email = normalizeEmail(request.email());
        CodeRecord record = jdbcTemplate.query("""
                SELECT code_hash FROM email_login_codes
                WHERE email = ? AND expires_at >= CURRENT_TIMESTAMP AND verified_at IS NULL
                ORDER BY id DESC LIMIT 1
                """, rs -> rs.next() ? new CodeRecord(rs.getString("code_hash")) : null, email);
        if (record == null || !record.codeHash().equals(hash(email, request.code()))) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "验证码无效或已过期");
        }

        jdbcTemplate.update("UPDATE email_login_codes SET verified_at = CURRENT_TIMESTAMP WHERE email = ?", email);
        long userId = findOrCreateUser(email);
        UserProfile profile = userService.profile(userId);
        return new LoginResult(authService.issueToken(userId, profile.role()), profile);
    }

    private long findOrCreateUser(String email) {
        Long existing = jdbcTemplate.query("""
                SELECT id FROM users WHERE email = ?
                """, rs -> rs.next() ? rs.getLong("id") : null, email);
        if (existing != null) {
            return existing;
        }

        String prefix = email.substring(0, email.indexOf('@'));
        String nickname = "羽球用户" + (prefix.length() > 12 ? prefix.substring(0, 12) : prefix);
        jdbcTemplate.update("""
                INSERT INTO users(openid, nickname, email, age, play_years, gender)
                VALUES(?, ?, ?, 18, 1, '未设置')
                """, "email:" + email, nickname, email);
        return jdbcTemplate.queryForObject("SELECT id FROM users WHERE email = ?", Long.class, email);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

    private String nextCode() {
        int digits = Math.max(4, Math.min(8, codeLength));
        int bound = (int) Math.pow(10, digits);
        int value = random.nextInt(bound);
        return String.format("%0" + digits + "d", value);
    }

    private String hash(String email, String code) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal((email + ":" + code).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("email code sign failed", ex);
        }
    }

    private record CodeRecord(String codeHash) {
    }
}

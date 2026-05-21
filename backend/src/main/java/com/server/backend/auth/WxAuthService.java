package com.server.backend.auth;

import com.server.backend.common.BusinessException;
import com.server.backend.user.UserProfile;
import com.server.backend.user.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
public class WxAuthService {
    private final JdbcTemplate jdbcTemplate;
    private final AuthService authService;
    private final UserService userService;
    private final RestClient restClient;
    private final String appId;
    private final String appSecret;

    public WxAuthService(
            JdbcTemplate jdbcTemplate,
            AuthService authService,
            UserService userService,
            @Value("${app.wx.app-id}") String appId,
            @Value("${app.wx.app-secret}") String appSecret) {
        this.jdbcTemplate = jdbcTemplate;
        this.authService = authService;
        this.userService = userService;
        this.appId = appId;
        this.appSecret = appSecret;
        this.restClient = RestClient.create();
    }

    public LoginResult login(WxLoginRequest request) {
        if (appId == null || appId.isBlank() || appSecret == null || appSecret.isBlank()) {
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "微信登录配置缺失");
        }
        Map<?, ?> response = restClient.get()
                .uri("https://api.weixin.qq.com/sns/jscode2session?appid={appid}&secret={secret}&js_code={code}&grant_type=authorization_code",
                        appId, appSecret, request.code())
                .retrieve()
                .body(Map.class);
        if (response == null || response.get("openid") == null) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "微信登录失败");
        }
        String openid = String.valueOf(response.get("openid"));
        Long existing = jdbcTemplate.query("""
                SELECT id FROM users WHERE openid = ?
                """, rs -> rs.next() ? rs.getLong("id") : null, openid);
        long userId;
        if (existing == null) {
            jdbcTemplate.update("""
                    INSERT INTO users(openid, nickname, avatar_url, age, play_years, gender)
                    VALUES(?, ?, ?, 18, 1, '未设置')
                    """, openid,
                    request.nickname() == null || request.nickname().isBlank() ? "羽球用户" : request.nickname().trim(),
                    request.avatarUrl());
            userId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE openid = ?", Long.class, openid);
        } else {
            userId = existing;
            if (request.nickname() != null && !request.nickname().isBlank()) {
                jdbcTemplate.update("UPDATE users SET nickname = ?, avatar_url = COALESCE(?, avatar_url), updated_at = CURRENT_TIMESTAMP WHERE id = ?",
                        request.nickname().trim(), request.avatarUrl(), userId);
            }
        }
        UserProfile profile = userService.profile(userId);
        return new LoginResult(authService.issueToken(userId, profile.role()), profile);
    }
}

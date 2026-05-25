package com.server.backend.auth.service;

import com.server.backend.auth.dto.AdminLoginRequest;
import com.server.backend.auth.dto.LoginResult;
import com.server.backend.common.BusinessException;
import com.server.backend.user.dto.UserProfile;
import com.server.backend.user.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class AdminAuthService {
    private final AuthService authService;
    private final UserService userService;
    private final String username;
    private final String password;
    private final long userId;

    public AdminAuthService(
            AuthService authService,
            UserService userService,
            @Value("${app.admin.username:admin}") String username,
            @Value("${app.admin.password:}") String password,
            @Value("${app.admin.user-id:2}") long userId) {
        this.authService = authService;
        this.userService = userService;
        this.username = username;
        this.password = password;
        this.userId = userId;
    }

    public LoginResult login(AdminLoginRequest request) {
        if (password == null || password.isBlank()) {
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "管理员登录未配置");
        }
        if (!username.equals(request.username().trim()) || !password.equals(request.password())) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "管理员账号或密码错误");
        }

        UserProfile profile = userService.profile(userId);
        if (!"ADMIN".equals(profile.role())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "管理员账号未绑定 ADMIN 角色");
        }
        return new LoginResult(authService.issueToken(profile.id(), profile.role()), profile);
    }
}

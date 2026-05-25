package com.server.backend.auth.controller;

import com.server.backend.auth.dto.AdminLoginRequest;
import com.server.backend.auth.dto.LoginResult;
import com.server.backend.auth.dto.WxLoginRequest;
import com.server.backend.auth.service.AdminAuthService;
import com.server.backend.auth.service.WxAuthService;
import com.server.backend.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AdminAuthService adminAuthService;
    private final WxAuthService wxAuthService;

    public AuthController(AdminAuthService adminAuthService, WxAuthService wxAuthService) {
        this.adminAuthService = adminAuthService;
        this.wxAuthService = wxAuthService;
    }

    @PostMapping("/admin-login")
    public ApiResponse<LoginResult> adminLogin(@Valid @RequestBody AdminLoginRequest request) {
        return ApiResponse.ok(adminAuthService.login(request));
    }

    @PostMapping("/wx-login")
    public ApiResponse<LoginResult> wxLogin(@Valid @RequestBody WxLoginRequest request) {
        return ApiResponse.ok(wxAuthService.login(request));
    }
}

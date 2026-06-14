package com.server.backend.auth.controller;

import com.server.backend.auth.dto.AdminLoginRequest;
import com.server.backend.auth.dto.EmailCodeResult;
import com.server.backend.auth.dto.EmailLoginRequest;
import com.server.backend.auth.dto.LoginResult;
import com.server.backend.auth.dto.SendEmailCodeRequest;
import com.server.backend.auth.dto.WxLoginRequest;
import com.server.backend.auth.service.AdminAuthService;
import com.server.backend.auth.service.EmailAuthService;
import com.server.backend.auth.service.WxAuthService;
import com.server.backend.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AdminAuthService adminAuthService;
    private final WxAuthService wxAuthService;
    private final EmailAuthService emailAuthService;

    public AuthController(AdminAuthService adminAuthService, WxAuthService wxAuthService, EmailAuthService emailAuthService) {
        this.adminAuthService = adminAuthService;
        this.wxAuthService = wxAuthService;
        this.emailAuthService = emailAuthService;
    }

    @PostMapping("/admin-login")
    public ApiResponse<LoginResult> adminLogin(@Valid @RequestBody AdminLoginRequest request) {
        return ApiResponse.ok(adminAuthService.login(request));
    }

    @PostMapping("/wx-login")
    public ApiResponse<LoginResult> wxLogin(@Valid @RequestBody WxLoginRequest request) {
        return ApiResponse.ok(wxAuthService.login(request));
    }

    @PostMapping("/email-code")
    public ApiResponse<EmailCodeResult> sendEmailCode(@Valid @RequestBody SendEmailCodeRequest request) {
        return ApiResponse.ok(emailAuthService.sendCode(request));
    }

    @PostMapping("/email-login")
    public ApiResponse<LoginResult> emailLogin(@Valid @RequestBody EmailLoginRequest request) {
        return ApiResponse.ok(emailAuthService.login(request));
    }
}

package com.server.backend.auth;

import com.server.backend.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final WxAuthService wxAuthService;

    public AuthController(WxAuthService wxAuthService) {
        this.wxAuthService = wxAuthService;
    }

    @PostMapping("/wx-login")
    public ApiResponse<LoginResult> wxLogin(@Valid @RequestBody WxLoginRequest request) {
        return ApiResponse.ok(wxAuthService.login(request));
    }
}

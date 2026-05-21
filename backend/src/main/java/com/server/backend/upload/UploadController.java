package com.server.backend.upload;

import com.server.backend.auth.AuthService;
import com.server.backend.common.ApiResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/upload")
public class UploadController {
    private final UploadService uploadService;
    private final AuthService authService;

    public UploadController(UploadService uploadService, AuthService authService) {
        this.uploadService = uploadService;
        this.authService = authService;
    }

    @PostMapping
    public ApiResponse<UploadResult> upload(
            @RequestHeader("Authorization") String authorization,
            @RequestPart("file") MultipartFile file) {
        authService.requireUser(authorization);
        return ApiResponse.ok(uploadService.upload(file));
    }
}

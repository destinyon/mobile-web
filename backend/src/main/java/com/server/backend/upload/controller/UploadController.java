package com.server.backend.upload.controller;

import com.server.backend.auth.service.AuthService;
import com.server.backend.common.ApiResponse;
import com.server.backend.upload.dto.UploadResult;
import com.server.backend.upload.service.UploadService;
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

    @DeleteMapping
    public ApiResponse<Void> delete(
            @RequestHeader("Authorization") String authorization,
            @RequestParam String objectKey) {
        authService.requireUser(authorization);
        uploadService.delete(objectKey);
        return ApiResponse.ok(null);
    }
}

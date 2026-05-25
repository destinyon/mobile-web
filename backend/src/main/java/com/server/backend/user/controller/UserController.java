package com.server.backend.user.controller;

import com.server.backend.auth.service.AuthService;
import com.server.backend.comment.dto.UserCommentItem;
import com.server.backend.common.ApiResponse;
import com.server.backend.news.dto.NewsSummary;
import com.server.backend.post.dto.PostSummary;
import com.server.backend.user.dto.BrowseHistoryItem;
import com.server.backend.user.dto.UpdateProfileRequest;
import com.server.backend.user.dto.UserProfile;
import com.server.backend.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user")
public class UserController {
    private final UserService userService;
    private final AuthService authService;

    public UserController(UserService userService, AuthService authService) {
        this.userService = userService;
        this.authService = authService;
    }

    @GetMapping("/profile")
    public ApiResponse<UserProfile> profile(@RequestHeader("Authorization") String authorization) {
        return ApiResponse.ok(userService.profile(authService.requireUser(authorization).id()));
    }

    @PutMapping("/profile")
    public ApiResponse<UserProfile> update(
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ApiResponse.ok(userService.update(authService.requireUser(authorization).id(), request));
    }

    @GetMapping("/favorites")
    public ApiResponse<List<NewsSummary>> favorites(@RequestHeader("Authorization") String authorization) {
        return ApiResponse.ok(userService.favorites(authService.requireUser(authorization).id()));
    }

    @GetMapping("/comments")
    public ApiResponse<List<UserCommentItem>> comments(@RequestHeader("Authorization") String authorization) {
        return ApiResponse.ok(userService.comments(authService.requireUser(authorization).id()));
    }

    @GetMapping("/posts")
    public ApiResponse<List<PostSummary>> posts(@RequestHeader("Authorization") String authorization) {
        return ApiResponse.ok(userService.posts(authService.requireUser(authorization).id()));
    }

    @GetMapping("/history")
    public ApiResponse<List<BrowseHistoryItem>> history(@RequestHeader("Authorization") String authorization) {
        return ApiResponse.ok(userService.history(authService.requireUser(authorization).id()));
    }
}

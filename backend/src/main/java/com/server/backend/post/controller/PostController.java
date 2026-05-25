package com.server.backend.post.controller;

import com.server.backend.auth.dto.AuthUser;
import com.server.backend.auth.service.AuthService;
import com.server.backend.common.ApiResponse;
import com.server.backend.common.PageResult;
import com.server.backend.post.dto.CreatePostRequest;
import com.server.backend.post.dto.PostDetail;
import com.server.backend.post.dto.PostSummary;
import com.server.backend.post.dto.TopicItem;
import com.server.backend.post.service.PostService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class PostController {
    private final PostService postService;
    private final AuthService authService;

    public PostController(PostService postService, AuthService authService) {
        this.postService = postService;
        this.authService = authService;
    }

    @GetMapping("/api/topics")
    public ApiResponse<List<TopicItem>> topics() {
        return ApiResponse.ok(postService.topics());
    }

    @GetMapping("/api/posts")
    public ApiResponse<PageResult<PostSummary>> list(
            @RequestParam(required = false) Long topicId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "latest") String sort,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int pageSize,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        AuthUser user = authService.parse(authorization);
        return ApiResponse.ok(postService.list(topicId, keyword, sort, page, pageSize, user == null ? 0 : user.id()));
    }

    @GetMapping("/api/posts/{id}")
    public ApiResponse<PostDetail> detail(
            @PathVariable long id,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        AuthUser user = authService.parse(authorization);
        return ApiResponse.ok(postService.detail(id, user == null ? 0 : user.id()));
    }

    @PostMapping("/api/posts")
    public ApiResponse<PostDetail> create(
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody CreatePostRequest request) {
        return ApiResponse.ok(postService.create(authService.requireUser(authorization), request));
    }

    @PostMapping("/api/posts/{id}/favorite")
    public ApiResponse<Void> favorite(@RequestHeader("Authorization") String authorization, @PathVariable long id) {
        postService.favorite(authService.requireUser(authorization), id);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/api/posts/{id}/favorite")
    public ApiResponse<Void> unfavorite(@RequestHeader("Authorization") String authorization, @PathVariable long id) {
        postService.unfavorite(authService.requireUser(authorization), id);
        return ApiResponse.ok(null);
    }

    @PostMapping("/api/posts/{id}/like")
    public ApiResponse<Void> like(@RequestHeader("Authorization") String authorization, @PathVariable long id) {
        postService.like(authService.requireUser(authorization), id);
        return ApiResponse.ok(null);
    }
}

package com.server.backend.news.controller;

import com.server.backend.auth.service.AuthService;
import com.server.backend.auth.dto.AuthUser;
import com.server.backend.common.ApiResponse;
import com.server.backend.common.PageResult;
import com.server.backend.news.dto.CreateNewsRequest;
import com.server.backend.news.dto.NewsDetail;
import com.server.backend.news.dto.NewsSummary;
import com.server.backend.news.service.NewsService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/news")
public class NewsController {
    private final NewsService newsService;
    private final AuthService authService;

    public NewsController(NewsService newsService, AuthService authService) {
        this.newsService = newsService;
        this.authService = authService;
    }

    @GetMapping
    public ApiResponse<PageResult<NewsSummary>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "latest") String sort,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int pageSize,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        AuthUser user = authService.parse(authorization);
        return ApiResponse.ok(newsService.list(keyword, categoryId, sort, page, pageSize, user == null ? 0 : user.id()));
    }

    @GetMapping("/{id}")
    public ApiResponse<NewsDetail> detail(
            @PathVariable long id,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        AuthUser user = authService.parse(authorization);
        return ApiResponse.ok(newsService.detail(id, user == null ? 0 : user.id()));
    }

    @PostMapping
    public ApiResponse<NewsDetail> create(
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody CreateNewsRequest request) {
        return ApiResponse.ok(newsService.create(authService.requireUser(authorization), request));
    }

    @PostMapping("/{id}/favorite")
    public ApiResponse<Void> favorite(@RequestHeader("Authorization") String authorization, @PathVariable long id) {
        newsService.favorite(authService.requireUser(authorization), id);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/{id}/favorite")
    public ApiResponse<Void> unfavorite(@RequestHeader("Authorization") String authorization, @PathVariable long id) {
        newsService.unfavorite(authService.requireUser(authorization), id);
        return ApiResponse.ok(null);
    }

    @PostMapping("/{id}/like")
    public ApiResponse<Void> like(@RequestHeader("Authorization") String authorization, @PathVariable long id) {
        newsService.like(authService.requireUser(authorization), id);
        return ApiResponse.ok(null);
    }
}

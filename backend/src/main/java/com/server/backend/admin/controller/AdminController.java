package com.server.backend.admin.controller;

import com.server.backend.auth.service.AuthService;
import com.server.backend.auth.dto.AuthUser;
import com.server.backend.common.ApiResponse;
import com.server.backend.common.BusinessException;
import com.server.backend.common.PageResult;
import com.server.backend.news.dto.NewsSummary;
import com.server.backend.news.netease.NeteaseNewsSyncResult;
import com.server.backend.news.netease.service.NeteaseNewsSyncService;
import com.server.backend.news.service.NewsService;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AuthService authService;
    private final NewsService newsService;
    private final NeteaseNewsSyncService neteaseNewsSyncService;
    private final JdbcTemplate jdbcTemplate;

    public AdminController(
            AuthService authService,
            NewsService newsService,
            NeteaseNewsSyncService neteaseNewsSyncService,
            JdbcTemplate jdbcTemplate) {
        this.authService = authService;
        this.newsService = newsService;
        this.neteaseNewsSyncService = neteaseNewsSyncService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/news")
    public ApiResponse<PageResult<NewsSummary>> news(
            @RequestHeader("Authorization") String authorization,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String keyword) {
        requireAdmin(authorization);
        return ApiResponse.ok(newsService.list(keyword, null, "latest", page, pageSize, 0));
    }

    @PutMapping("/news/{id}/status")
    public ApiResponse<Void> updateStatus(
            @RequestHeader("Authorization") String authorization,
            @PathVariable long id,
            @RequestParam String status) {
        AuthUser admin = requireAdmin(authorization);
        jdbcTemplate.update("UPDATE news SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?", status.toUpperCase(), id);
        jdbcTemplate.update("INSERT INTO admin_logs(admin_id, action, target_type, target_id) VALUES(?, ?, 'NEWS', ?)",
                admin.id(), "UPDATE_STATUS_" + status.toUpperCase(), id);
        return ApiResponse.ok(null);
    }

    @PostMapping("/news/sync")
    public ApiResponse<NeteaseNewsSyncResult> syncNews(@RequestHeader("Authorization") String authorization) {
        requireAdmin(authorization);
        return ApiResponse.ok(neteaseNewsSyncService.syncLatest());
    }

    private AuthUser requireAdmin(String authorization) {
        AuthUser user = authService.requireUser(authorization);
        if (!"ADMIN".equals(user.role())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "需要管理员权限");
        }
        return user;
    }
}

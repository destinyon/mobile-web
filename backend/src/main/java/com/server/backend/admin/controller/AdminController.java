package com.server.backend.admin.controller;

import com.server.backend.admin.dto.AdminNewsRankingItem;
import com.server.backend.admin.dto.AdminSummary;
import com.server.backend.admin.dto.AdminUserDetail;
import com.server.backend.admin.dto.AdminUserItem;
import com.server.backend.admin.service.AdminDashboardService;
import com.server.backend.auth.service.AuthService;
import com.server.backend.auth.dto.AuthUser;
import com.server.backend.common.ApiResponse;
import com.server.backend.common.BusinessException;
import com.server.backend.common.PageResult;
import com.server.backend.news.dto.NewsDetail;
import com.server.backend.news.dto.NewsSummary;
import com.server.backend.news.netease.NeteaseNewsSyncResult;
import com.server.backend.news.netease.service.NeteaseNewsSyncService;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AuthService authService;
    private final NeteaseNewsSyncService neteaseNewsSyncService;
    private final AdminDashboardService adminDashboardService;
    private final JdbcTemplate jdbcTemplate;

    public AdminController(
            AuthService authService,
            NeteaseNewsSyncService neteaseNewsSyncService,
            AdminDashboardService adminDashboardService,
            JdbcTemplate jdbcTemplate) {
        this.authService = authService;
        this.neteaseNewsSyncService = neteaseNewsSyncService;
        this.adminDashboardService = adminDashboardService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/summary")
    public ApiResponse<AdminSummary> summary(@RequestHeader("Authorization") String authorization) {
        requireAdmin(authorization);
        return ApiResponse.ok(adminDashboardService.summary());
    }

    @GetMapping("/news")
    public ApiResponse<PageResult<NewsSummary>> news(
            @RequestHeader("Authorization") String authorization,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId) {
        requireAdmin(authorization);
        return ApiResponse.ok(adminDashboardService.news(keyword, categoryId, page, pageSize));
    }

    @GetMapping("/news/{id}")
    public ApiResponse<NewsDetail> newsDetail(
            @RequestHeader("Authorization") String authorization,
            @PathVariable long id) {
        requireAdmin(authorization);
        return ApiResponse.ok(adminDashboardService.newsDetail(id));
    }

    @GetMapping("/news/rankings")
    public ApiResponse<java.util.List<AdminNewsRankingItem>> newsRankings(
            @RequestHeader("Authorization") String authorization,
            @RequestParam(defaultValue = "10") int limit) {
        requireAdmin(authorization);
        return ApiResponse.ok(adminDashboardService.newsRankings(limit));
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
    public ApiResponse<NeteaseNewsSyncResult> syncNews(
            @RequestHeader("Authorization") String authorization,
            @RequestParam(defaultValue = "1") int pages) {
        requireAdmin(authorization);
        return ApiResponse.ok(neteaseNewsSyncService.syncLatest(pages));
    }

    @GetMapping("/users")
    public ApiResponse<PageResult<AdminUserItem>> users(
            @RequestHeader("Authorization") String authorization,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        requireAdmin(authorization);
        return ApiResponse.ok(adminDashboardService.users(keyword, page, pageSize));
    }

    @GetMapping("/users/{id}")
    public ApiResponse<AdminUserDetail> userDetail(
            @RequestHeader("Authorization") String authorization,
            @PathVariable long id) {
        requireAdmin(authorization);
        return ApiResponse.ok(adminDashboardService.userDetail(id));
    }

    private AuthUser requireAdmin(String authorization) {
        AuthUser user = authService.requireUser(authorization);
        if (!"ADMIN".equals(user.role())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "需要管理员权限");
        }
        return user;
    }
}

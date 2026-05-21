package com.server.backend.admin;

import com.server.backend.auth.AuthService;
import com.server.backend.auth.AuthUser;
import com.server.backend.common.ApiResponse;
import com.server.backend.common.BusinessException;
import com.server.backend.common.PageResult;
import com.server.backend.news.NewsService;
import com.server.backend.news.NewsSummary;
import com.server.backend.news.juhe.JuheNewsSyncResult;
import com.server.backend.news.juhe.JuheNewsSyncService;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AuthService authService;
    private final NewsService newsService;
    private final JuheNewsSyncService juheNewsSyncService;
    private final JdbcTemplate jdbcTemplate;

    public AdminController(
            AuthService authService,
            NewsService newsService,
            JuheNewsSyncService juheNewsSyncService,
            JdbcTemplate jdbcTemplate) {
        this.authService = authService;
        this.newsService = newsService;
        this.juheNewsSyncService = juheNewsSyncService;
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

    @PostMapping("/news/sync/juhe")
    public ApiResponse<JuheNewsSyncResult> syncJuheNews(@RequestHeader("Authorization") String authorization) {
        requireAdmin(authorization);
        return ApiResponse.ok(juheNewsSyncService.sync());
    }

    private AuthUser requireAdmin(String authorization) {
        AuthUser user = authService.requireUser(authorization);
        if (!"ADMIN".equals(user.role())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "需要管理员权限");
        }
        return user;
    }
}

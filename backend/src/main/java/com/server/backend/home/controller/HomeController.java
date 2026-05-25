package com.server.backend.home.controller;

import com.server.backend.common.ApiResponse;
import com.server.backend.home.dto.BannerItem;
import com.server.backend.home.dto.CategoryItem;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class HomeController {
    private final JdbcTemplate jdbcTemplate;

    public HomeController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/categories")
    public ApiResponse<List<CategoryItem>> categories() {
        return ApiResponse.ok(jdbcTemplate.query("""
                SELECT id, name, sort_no FROM categories WHERE status = 'ACTIVE' ORDER BY sort_no ASC
                """, (rs, rowNum) -> new CategoryItem(rs.getLong("id"), rs.getString("name"), rs.getInt("sort_no"))));
    }

    @GetMapping("/banners")
    public ApiResponse<List<BannerItem>> banners() {
        return ApiResponse.ok(jdbcTemplate.query("""
                SELECT id, title, cover_url AS image_url, 'NEWS' AS link_type, CAST(id AS CHAR) AS link_target
                FROM news
                WHERE status = 'PUBLISHED' AND cover_url IS NOT NULL AND cover_url <> ''
                ORDER BY updated_at DESC, id DESC
                LIMIT 5
                """, (rs, rowNum) -> new BannerItem(
                rs.getLong("id"),
                rs.getString("title"),
                rs.getString("image_url"),
                rs.getString("link_type"),
                rs.getString("link_target")
        )));
    }
}

package com.server.backend;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class NewsApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void seedNews() {
        jdbcTemplate.update("DELETE FROM comments WHERE target_type = 'NEWS'");
        jdbcTemplate.update("DELETE FROM favorites WHERE target_type = 'NEWS'");
        jdbcTemplate.update("DELETE FROM likes WHERE target_type = 'NEWS'");
        jdbcTemplate.update("DELETE FROM news");
        jdbcTemplate.update("""
                INSERT INTO news(id, category_id, user_id, title, cover_url, summary, author, content, source, source_id, source_url, status)
                VALUES(1, 1, NULL, '测试羽毛球新闻', 'https://example.test/cover.jpg', '测试摘要', '网易体育', '<p>测试正文</p>', 'NETEASE', 'test-1', 'https://example.test/news/1', 'PUBLISHED')
                """);
    }

    @Test
    void listsPublishedNewsWithCourseRequiredFields() throws Exception {
        mockMvc.perform(get("/api/news").param("page", "1").param("pageSize", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.data.items[0].title").isNotEmpty())
                .andExpect(jsonPath("$.data.items[0].coverUrl").isNotEmpty())
                .andExpect(jsonPath("$.data.items[0].viewCount").isNumber())
                .andExpect(jsonPath("$.data.items[0].updatedAt").isNotEmpty());
    }

    @Test
    void returnsNewsDetailWithCommentsEnvelope() throws Exception {
        mockMvc.perform(get("/api/news/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").isNotEmpty())
                .andExpect(jsonPath("$.data.author").isNotEmpty())
                .andExpect(jsonPath("$.data.content").isNotEmpty())
                .andExpect(jsonPath("$.data.comments").isArray());
    }
}

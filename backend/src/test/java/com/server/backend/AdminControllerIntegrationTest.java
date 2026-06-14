package com.server.backend;

import com.server.backend.news.netease.NeteaseNewsSyncResult;
import com.server.backend.news.netease.service.NeteaseNewsSyncService;
import com.server.backend.auth.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AdminControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AuthService authService;

    @MockBean
    private NeteaseNewsSyncService neteaseNewsSyncService;

    private String adminToken;

    @BeforeEach
    void seedAdminData() {
        adminToken = "Bearer " + authService.issueToken(2, "ADMIN");
        jdbcTemplate.update("DELETE FROM admin_logs");
        jdbcTemplate.update("DELETE FROM browse_history");
        jdbcTemplate.update("DELETE FROM comments");
        jdbcTemplate.update("DELETE FROM favorites");
        jdbcTemplate.update("DELETE FROM likes");
        jdbcTemplate.update("DELETE FROM posts");
        jdbcTemplate.update("DELETE FROM news");
        jdbcTemplate.update("DELETE FROM topics");
        jdbcTemplate.update("DELETE FROM users");
        jdbcTemplate.update("DELETE FROM categories");

        jdbcTemplate.update("""
                INSERT INTO categories(id, name, sort_no, status)
                VALUES(1, '赛事', 1, 'ACTIVE'), (2, '装备', 2, 'ACTIVE')
                """);
        jdbcTemplate.update("""
                INSERT INTO topics(id, name, description, sort_no, status)
                VALUES(1, '璧涗簨', 'User post topic', 1, 'ACTIVE')
                """);
        jdbcTemplate.update("""
                INSERT INTO users(id, openid, nickname, avatar_url, phone, age, play_years, gender, role, status)
                VALUES
                (1, 'user-openid', '普通用户', '', '17700000000', 20, 3, '女', 'USER', 'ACTIVE'),
                (2, 'admin-openid', '管理员', '', '18800000000', 30, 10, '男', 'ADMIN', 'ACTIVE')
                """);
        jdbcTemplate.update("UPDATE users SET email = ? WHERE id = ?", "user@example.test", 1);
        jdbcTemplate.update("UPDATE users SET email = ? WHERE id = ?", "admin@example.test", 2);
        jdbcTemplate.update("""
                INSERT INTO news(id, category_id, user_id, title, cover_url, summary, author, content,
                    view_count, like_count, favorite_count, comment_count, status)
                VALUES
                (10, 1, 2, '赛事新闻', 'https://example.test/a.jpg', '赛事摘要', '管理员', '<p>赛事正文</p>',
                    30, 5, 2, 1, 'PUBLISHED'),
                (11, 2, 2, '装备新闻', 'https://example.test/b.jpg', '装备摘要', '管理员', '<p>装备正文</p>',
                    7, 3, 8, 0, 'PUBLISHED'),
                (12, 2, 2, '已下架新闻', 'https://example.test/c.jpg', '下架摘要', '管理员', '<p>下架正文</p>',
                    9, 1, 1, 0, 'OFFLINE')
                """);
        jdbcTemplate.update("""
                INSERT INTO posts(id, topic_id, user_id, title, cover_url, content, status)
                VALUES(30, 1, 1, '用户帖子', '', '帖子正文', 'PUBLISHED')
                """);
        jdbcTemplate.update("""
                INSERT INTO comments(id, target_type, target_id, user_id, content, status)
                VALUES(40, 'NEWS', 10, 1, '评论内容', 'PUBLISHED')
                """);
        jdbcTemplate.update("""
                UPDATE posts
                SET title = 'User ranking post',
                    content = 'Post content',
                    view_count = 50,
                    like_count = 20,
                    favorite_count = 1,
                    comment_count = 1
                WHERE id = 30
                """);
        jdbcTemplate.update("""
                INSERT INTO comments(id, target_type, target_id, user_id, content, status)
                VALUES(41, 'POST', 30, 1, 'Post comment', 'PUBLISHED')
                """);
        jdbcTemplate.update("""
                INSERT INTO likes(user_id, target_type, target_id)
                VALUES(1, 'NEWS', 10), (2, 'NEWS', 10), (1, 'NEWS', 11)
                """);
        jdbcTemplate.update("""
                INSERT INTO favorites(user_id, target_type, target_id)
                VALUES(1, 'NEWS', 11), (2, 'NEWS', 11), (1, 'POST', 30)
                """);
    }

    @Test
    void adminNewsListUsesAllNewsRowsAndSupportsCategoryFilter() throws Exception {
        mockMvc.perform(get("/api/admin/news")
                        .header("Authorization", adminToken)
                        .param("page", "1")
                        .param("pageSize", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.total").value(3))
                .andExpect(jsonPath("$.data.items", hasSize(1)));

        mockMvc.perform(get("/api/admin/news")
                        .header("Authorization", adminToken)
                        .param("categoryId", "2")
                        .param("page", "1")
                        .param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.items", hasSize(2)))
                .andExpect(jsonPath("$.data.items[0].id").value(12))
                .andExpect(jsonPath("$.data.items[0].categoryName").value("装备"))
                .andExpect(jsonPath("$.data.items[1].id").value(11));
    }

    @Test
    void adminNewsDetailDoesNotIncreaseViewsAndCanReadOfflineNews() throws Exception {
        mockMvc.perform(get("/api/admin/news/12").header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("已下架新闻"))
                .andExpect(jsonPath("$.data.viewCount").value(9))
                .andExpect(jsonPath("$.data.content").value("<p>下架正文</p>"));

        Integer viewCount = jdbcTemplate.queryForObject("SELECT view_count FROM news WHERE id = 12", Integer.class);
        assertThat(viewCount).isEqualTo(9);
    }

    @Test
    void adminSummaryAndRankingsUseRealRows() throws Exception {
        mockMvc.perform(get("/api/admin/summary").header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userCount").value(2))
                .andExpect(jsonPath("$.data.newsCount").value(3))
                .andExpect(jsonPath("$.data.publishedNewsCount").value(2))
                .andExpect(jsonPath("$.data.offlineNewsCount").value(1))
                .andExpect(jsonPath("$.data.postCount").value(1))
                .andExpect(jsonPath("$.data.commentCount").value(2))
                .andExpect(jsonPath("$.data.totalViews").value(96))
                .andExpect(jsonPath("$.data.totalLikes").value(29))
                .andExpect(jsonPath("$.data.totalFavorites").value(12))
                .andExpect(jsonPath("$.data.categoryStats", hasSize(2)))
                .andExpect(jsonPath("$.data.categoryStats[0].newsCount").value(1))
                .andExpect(jsonPath("$.data.categoryStats[0].postCount").value(1))
                .andExpect(jsonPath("$.data.categoryStats[0].contentCount").value(2))
                .andExpect(jsonPath("$.data.categoryStats[1].categoryName").value("装备"));

        mockMvc.perform(get("/api/admin/news/rankings")
                        .header("Authorization", adminToken)
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(2))))
                .andExpect(jsonPath("$.data[0].targetType").value("POST"))
                .andExpect(jsonPath("$.data[0].id").value(30))
                .andExpect(jsonPath("$.data[0].heatScore").value(21));
    }

    @Test
    void adminPostDetailCanReadPublishedUserPostWithoutIncreasingViews() throws Exception {
        mockMvc.perform(get("/api/admin/posts/30").header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("User ranking post"))
                .andExpect(jsonPath("$.data.topicName").value("璧涗簨"))
                .andExpect(jsonPath("$.data.viewCount").value(50))
                .andExpect(jsonPath("$.data.content").value("Post content"));

        Integer viewCount = jdbcTemplate.queryForObject("SELECT view_count FROM posts WHERE id = 30", Integer.class);
        assertThat(viewCount).isEqualTo(50);
    }

    @Test
    void adminUsersListReturnsOperationalStats() throws Exception {
        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", adminToken)
                        .param("keyword", "普通")
                        .param("page", "1")
                        .param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items", hasSize(1)))
                .andExpect(jsonPath("$.data.items[0].nickname").value("普通用户"))
                .andExpect(jsonPath("$.data.items[0].email").value("user@example.test"))
                .andExpect(jsonPath("$.data.items[0].postCount").value(1))
                .andExpect(jsonPath("$.data.items[0].commentCount").value(2))
                .andExpect(jsonPath("$.data.items[0].favoriteCount").value(2));
    }

    @Test
    void adminUserDetailReturnsRealProfileAndActivityStats() throws Exception {
        mockMvc.perform(get("/api/admin/users/1").header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.nickname").value("普通用户"))
                .andExpect(jsonPath("$.data.email").value("user@example.test"))
                .andExpect(jsonPath("$.data.age").value(20))
                .andExpect(jsonPath("$.data.playYears").value(3))
                .andExpect(jsonPath("$.data.postCount").value(1))
                .andExpect(jsonPath("$.data.commentCount").value(2))
                .andExpect(jsonPath("$.data.favoriteCount").value(2))
                .andExpect(jsonPath("$.data.likeCount").value(2));
    }

    @Test
    void adminNewsSyncAcceptsManualPageLimit() throws Exception {
        when(neteaseNewsSyncService.syncLatest(1)).thenReturn(new NeteaseNewsSyncResult(1, 8, 3, 5));

        mockMvc.perform(post("/api/admin/news/sync")
                        .header("Authorization", adminToken)
                        .param("pages", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.pages").value(1));
    }
}

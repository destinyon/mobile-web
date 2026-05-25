package com.server.backend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.server.backend.auth.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UserFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthService authService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void seedNews() {
        jdbcTemplate.update("DELETE FROM comments WHERE target_type = 'NEWS'");
        jdbcTemplate.update("DELETE FROM favorites WHERE target_type = 'NEWS'");
        jdbcTemplate.update("DELETE FROM likes WHERE target_type = 'NEWS'");
        jdbcTemplate.update("DELETE FROM comments WHERE target_type = 'POST'");
        jdbcTemplate.update("DELETE FROM favorites WHERE target_type = 'POST'");
        jdbcTemplate.update("DELETE FROM likes WHERE target_type = 'POST'");
        jdbcTemplate.update("DELETE FROM browse_history");
        jdbcTemplate.update("DELETE FROM posts");
        jdbcTemplate.update("DELETE FROM news");
        jdbcTemplate.update("""
                INSERT INTO news(id, category_id, user_id, title, cover_url, summary, author, content, source, source_id, source_url, status)
                VALUES(3, 1, NULL, '测试羽毛球报名新闻', 'https://example.test/cover.jpg', '测试摘要', '网易体育', '<p>测试正文</p>', 'NETEASE', 'test-3', 'https://example.test/news/3', 'PUBLISHED')
                """);
    }

    @Test
    void supportsFavoriteCommentAndMinePagesWithRealDatabase() throws Exception {
        String token = "Bearer " + authService.issueToken(1L, "USER");

        mockMvc.perform(post("/api/news/3/favorite").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(post("/api/comments")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"targetType":"NEWS","targetId":3,"content":"报名信息很清楚，期待分级对抗。"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").value("报名信息很清楚，期待分级对抗。"));

        mockMvc.perform(get("/api/user/favorites").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.data[0].title").isNotEmpty());

        mockMvc.perform(get("/api/user/comments").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.data[0].content").isNotEmpty())
                .andExpect(jsonPath("$.data[0].targetTitle").value("测试羽毛球报名新闻"))
                .andExpect(jsonPath("$.data[0].targetType").value("NEWS"));

        mockMvc.perform(delete("/api/news/3/favorite").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void tracksHistoryAndReturnsLikedStateOnDetail() throws Exception {
        String token = "Bearer " + authService.issueToken(1L, "USER");

        mockMvc.perform(post("/api/news/3/like").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.liked").value(true));

        mockMvc.perform(get("/api/news/3").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.liked").value(true));

        mockMvc.perform(get("/api/news/3").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.liked").value(true));

        mockMvc.perform(get("/api/user/history").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].targetType").value("NEWS"))
                .andExpect(jsonPath("$.data[0].targetId").value(3))
                .andExpect(jsonPath("$.data[0].title").value("测试羽毛球报名新闻"));

        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM browse_history
                WHERE user_id = 1 AND target_type = 'NEWS' AND target_id = 3
                """, Integer.class);
        org.assertj.core.api.Assertions.assertThat(count).isEqualTo(1);
    }

    @Test
    void recordsPostBrowseHistoryForMineHistory() throws Exception {
        String token = "Bearer " + authService.issueToken(1L, "USER");
        jdbcTemplate.update("""
                INSERT INTO posts(id, topic_id, user_id, title, cover_url, content, images, status)
                VALUES(4, 2, 1, '浏览记录里的帖子', 'https://example.test/post-cover.jpg', '帖子正文', '', 'PUBLISHED')
                """);

        mockMvc.perform(get("/api/posts/4").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("浏览记录里的帖子"));

        mockMvc.perform(get("/api/user/history").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].targetType").value("POST"))
                .andExpect(jsonPath("$.data[0].targetId").value(4))
                .andExpect(jsonPath("$.data[0].title").value("浏览记录里的帖子"));
    }

    @Test
    void supportsCommunityPostPublishInteractionAndReplies() throws Exception {
        String token = "Bearer " + authService.issueToken(1L, "USER");

        mockMvc.perform(get("/api/topics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.data[0].name").isNotEmpty());

        String postJson = """
                {
                  "topicId": 1,
                  "title": "周末双打怎么轮转更顺畅？",
                  "coverUrl": "https://example.test/post-cover.jpg",
                  "content": "最近固定搭档打双打，想听听大家对前后场轮转的建议。",
                  "images": ["https://example.test/post.jpg"]
                }
                """;

        JsonNode postBody = objectMapper.readTree(mockMvc.perform(post("/api/posts")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(postJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("周末双打怎么轮转更顺畅？"))
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"))
                .andReturn()
                .getResponse()
                .getContentAsByteArray());
        long postId = postBody.path("data").path("id").asLong();

        mockMvc.perform(get("/api/posts").param("sort", "latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.data.items[0].title").isNotEmpty());

        mockMvc.perform(post("/api/posts/" + postId + "/like").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.liked").value(true))
                .andExpect(jsonPath("$.data.likeCount").value(1));

        mockMvc.perform(post("/api/posts/" + postId + "/like").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.liked").value(false))
                .andExpect(jsonPath("$.data.likeCount").value(0));

        mockMvc.perform(post("/api/posts/" + postId + "/like").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.liked").value(true))
                .andExpect(jsonPath("$.data.likeCount").value(1));

        mockMvc.perform(post("/api/posts/" + postId + "/favorite").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        JsonNode rootComment = objectMapper.readTree(mockMvc.perform(post("/api/comments")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"targetType":"POST","targetId":%d,"content":"可以先约定谁接中路球。"}
                                """.formatted(postId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn()
                .getResponse()
                .getContentAsByteArray());
        long commentId = rootComment.path("data").path("id").asLong();

        mockMvc.perform(post("/api/comments")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"targetType":"POST","targetId":%d,"parentId":%d,"content":"这个建议实用。"}
                                """.formatted(postId, commentId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/posts/" + postId).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.favorited").value(true))
                .andExpect(jsonPath("$.data.likeCount").value(1))
                .andExpect(jsonPath("$.data.comments[0].replies", hasSize(1)))
                .andExpect(jsonPath("$.data.comments[0].replies[0].content").value("这个建议实用。"));

        mockMvc.perform(get("/api/user/posts").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.data[0].title").isNotEmpty());
    }

    @Test
    void supportsSingleDraftAndAuthorEditFlow() throws Exception {
        String token = "Bearer " + authService.issueToken(1L, "USER");

        mockMvc.perform(put("/api/posts/draft")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "topicId": 2,
                                  "title": "草稿标题",
                                  "coverUrl": "https://example.test/draft-cover.jpg",
                                  "content": "第一版草稿内容",
                                  "images": ["https://example.test/a.jpg"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.coverUrl").value("https://example.test/draft-cover.jpg"));

        mockMvc.perform(put("/api/posts/draft")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "topicId": 3,
                                  "title": "草稿标题二",
                                  "coverUrl": "https://example.test/draft-cover-2.jpg",
                                  "content": "第二版草稿内容",
                                  "images": []
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.title").value("草稿标题二"));

        mockMvc.perform(get("/api/posts/draft").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("草稿标题二"));

        JsonNode published = objectMapper.readTree(mockMvc.perform(post("/api/posts/draft/publish")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"))
                .andReturn()
                .getResponse()
                .getContentAsByteArray());
        long postId = published.path("data").path("id").asLong();

        mockMvc.perform(put("/api/posts/" + postId)
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "topicId": 1,
                                  "title": "更新后的帖子",
                                  "coverUrl": "https://example.test/updated-cover.jpg",
                                  "content": "更新后的正文",
                                  "images": ["https://example.test/b.jpg"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("更新后的帖子"))
                .andExpect(jsonPath("$.data.coverUrl").value("https://example.test/updated-cover.jpg"));

        mockMvc.perform(get("/api/user/posts").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].status").value("PUBLISHED"))
                .andExpect(jsonPath("$.data[0].title").value("更新后的帖子"));
    }
}

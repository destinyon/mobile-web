package com.server.backend;

import com.server.backend.news.netease.NeteaseNewsClient;
import com.server.backend.news.netease.NeteaseNewsDetail;
import com.server.backend.news.netease.NeteaseNewsItem;
import com.server.backend.news.netease.NeteaseNewsProperties;
import com.server.backend.news.netease.NeteaseNewsSyncResult;
import com.server.backend.news.netease.service.NeteaseNewsSyncService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "app.netease.news.category-id=1",
        "app.netease.news.pages=2",
        "app.netease.news.default-cover-url=https://example.test/default.jpg"
})
class NeteaseNewsSyncServiceTest {
    private static final String NEW_LONG_TITLE = "2026国际青年羽毛球挑战赛在靖江落幕：以完整长标题验证入库时不做截取"
            + "羽球交流合作".repeat(30);

    @Autowired
    private NeteaseNewsSyncService syncService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanNewsData() {
        jdbcTemplate.update("DELETE FROM comments WHERE target_type = 'NEWS'");
        jdbcTemplate.update("DELETE FROM favorites WHERE target_type = 'NEWS'");
        jdbcTemplate.update("DELETE FROM likes WHERE target_type = 'NEWS'");
        jdbcTemplate.update("DELETE FROM news");
        jdbcTemplate.update("DELETE FROM users WHERE id = 200");
    }

    @Test
    void syncOnlyImportsNewerNeteaseNewsAndKeepsExistingData() {
        jdbcTemplate.update("""
                INSERT INTO news(id, category_id, user_id, title, cover_url, summary, author, content,
                                 source, source_id, source_url, status, created_at, updated_at)
                VALUES(100, 1, NULL, '本地保留新闻', 'https://example.test/local.jpg', '本地摘要', '羽球在线',
                       '本地正文', 'LOCAL', 'local-100', 'https://example.test/local/100',
                       'PUBLISHED', '2026-05-20 10:00:00', '2026-05-20 10:00:00')
                """);
        jdbcTemplate.update("""
                INSERT INTO news(id, category_id, user_id, title, cover_url, summary, author, content,
                                 source, source_id, source_url, status, created_at, updated_at)
                VALUES(101, 1, NULL, '已同步网易新闻', 'https://example.test/old.jpg', '旧摘要', '网易体育',
                       '<p>旧正文</p><script>alert(1)</script>', 'NETEASE', 'KTFNN2O7053469KC', 'https://www.163.com/dy/article/KTFNN2O7053469KC.html',
                       'PUBLISHED', '2026-05-21 19:04:30', '2026-05-21 21:10:00')
                """);
        jdbcTemplate.update("INSERT INTO users(id, openid, nickname) VALUES(200, 'comment-user', '球友')");
        jdbcTemplate.update("INSERT INTO comments(target_type, target_id, user_id, content) VALUES('NEWS', 101, 200, '旧评论')");
        jdbcTemplate.update("INSERT INTO likes(user_id, target_type, target_id) VALUES(200, 'NEWS', 101)");
        jdbcTemplate.update("INSERT INTO favorites(user_id, target_type, target_id) VALUES(200, 'NEWS', 101)");

        NeteaseNewsSyncResult result = syncService.syncLatest();

        assertThat(result.pages()).isEqualTo(2);
        assertThat(result.fetched()).isEqualTo(4);
        assertThat(result.inserted()).isEqualTo(1);
        assertThat(result.skipped()).isEqualTo(3);

        Integer oldNews = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM news WHERE source = 'LOCAL'",
                Integer.class);
        assertThat(oldNews).isEqualTo(1);

        Integer neteaseNews = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM news WHERE source = 'NETEASE'",
                Integer.class);
        assertThat(neteaseNews).isEqualTo(2);

        Integer preservedComments = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM comments WHERE target_type = 'NEWS' AND target_id = 101",
                Integer.class);
        assertThat(preservedComments).isEqualTo(1);

        Integer preservedLikes = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM likes WHERE target_type = 'NEWS' AND target_id = 101",
                Integer.class);
        assertThat(preservedLikes).isEqualTo(1);

        Integer preservedFavorites = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM favorites WHERE target_type = 'NEWS' AND target_id = 101",
                Integer.class);
        assertThat(preservedFavorites).isEqualTo(1);

        String existingContent = jdbcTemplate.queryForObject(
                "SELECT content FROM news WHERE source_id = 'KTFNN2O7053469KC'",
                String.class);
        assertThat(existingContent).isEqualTo("<p>旧正文</p>");

        String title = jdbcTemplate.queryForObject(
                "SELECT title FROM news WHERE source_id = 'KTFNEW000000001'",
                String.class);
        assertThat(title).isEqualTo(NEW_LONG_TITLE);

        String content = jdbcTemplate.queryForObject(
                "SELECT content FROM news WHERE source_id = 'KTFNEW000000001'",
                String.class);
        assertThat(content)
                .contains("<p>第一段原文正文。</p>")
                .contains("<p><img src=\"https://example.test/article-2.jpg\"")
                .contains("max-width:100%;width:100%;height:auto;display:block;")
                .contains("<p style=\"margin:0 0 16px;color:#6a7467;font-size:13px;line-height:1.5;text-align:center;\">第二张图说明</p>")
                .contains("<p>第二段包含加粗内容。</p>")
                .doesNotContain("<figure")
                .doesNotContain("<figcaption")
                .doesNotContain("https://example.test/cover.jpg")
                .doesNotContain("<script");

        LocalDateTime publishedAt = jdbcTemplate.queryForObject(
                "SELECT created_at FROM news WHERE source_id = 'KTFNEW000000001'",
                (rs, rowNum) -> rs.getTimestamp(1).toLocalDateTime());
        assertThat(publishedAt).isEqualTo(LocalDateTime.of(2026, 5, 21, 20, 0));
    }

    @Test
    void manualSyncCanLimitFetchedPages() {
        NeteaseNewsSyncResult result = syncService.syncLatest(1);

        assertThat(result.pages()).isEqualTo(1);
        assertThat(result.fetched()).isEqualTo(3);
    }

    @Test
    void syncSkipsDuplicateKeyRaceWithoutFailing() {
        jdbcTemplate.update("""
                INSERT INTO news(category_id, user_id, title, cover_url, summary, author, content,
                                 source, source_id, source_url, status, created_at, updated_at)
                VALUES(1, NULL, '并发已入库新闻', 'https://example.test/cover.jpg', '摘要', '网易体育',
                       '<p>正文</p>', 'NETEASE', 'KTFNEW000000001', 'https://www.163.com/dy/article/KTFNEW000000001.html',
                       'PUBLISHED', '2026-05-21 20:00:00', '2026-05-21 20:00:00')
                """);

        NeteaseNewsSyncResult result = syncService.syncLatest();

        assertThat(result.inserted()).isZero();
        Integer rows = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM news WHERE source = 'NETEASE' AND source_id = 'KTFNEW000000001'",
                Integer.class);
        assertThat(rows).isEqualTo(1);
    }

    @TestConfiguration
    @EnableConfigurationProperties(NeteaseNewsProperties.class)
    static class TestConfig {
        @Bean
        @Primary
        NeteaseNewsClient neteaseNewsClient() {
            return new NeteaseNewsClient() {
                @Override
                public List<NeteaseNewsItem> fetchList(int page) {
                    if (page == 1) {
                        return List.of(
                                new NeteaseNewsItem("KTFNN2O7053469KC", "用羽毛球搭建沟通桥梁", "https://www.163.com/dy/article/KTFNN2O7053469KC.html"),
                                new NeteaseNewsItem("KTFM09770549753N", "陆光祖复仇晋级", "https://www.163.com/dy/article/KTFM09770549753N.html"),
                                new NeteaseNewsItem("KTFNEW000000001", NEW_LONG_TITLE, "https://www.163.com/dy/article/KTFNEW000000001.html")
                        );
                    }
                    return List.of(new NeteaseNewsItem("EMPTY", "空正文新闻", "https://www.163.com/dy/article/EMPTY.html"));
                }

                @Override
                public NeteaseNewsDetail fetchDetail(NeteaseNewsItem item) {
                    if ("EMPTY".equals(item.sourceId())) {
                        return new NeteaseNewsDetail(item.sourceId(), item.title(), item.url(), "网易体育", "", "", "", LocalDateTime.of(2026, 5, 21, 20, 30));
                    }
                    if ("KTFNN2O7053469KC".equals(item.sourceId())) {
                        return new NeteaseNewsDetail(
                                item.sourceId(),
                                item.title(),
                                item.url(),
                                "网易体育",
                                item.title(),
                                "https://example.test/old-cover.jpg",
                                "不应重新入库的旧正文",
                                LocalDateTime.of(2026, 5, 21, 19, 4, 30));
                    }
                    if ("KTFM09770549753N".equals(item.sourceId())) {
                        return new NeteaseNewsDetail(
                                item.sourceId(),
                                item.title(),
                                item.url(),
                                "网易体育",
                                item.title(),
                                "https://example.test/old-cover.jpg",
                                "发布时间不新的旧正文",
                                LocalDateTime.of(2026, 5, 21, 18, 0));
                    }
                    return new NeteaseNewsDetail(
                            item.sourceId(),
                            item.title(),
                            item.url(),
                            "网易体育",
                            item.title(),
                            "https://example.test/cover.jpg",
                            """
                                    <p>第一段原文正文。</p>
                                    <script>alert(1)</script>
                                    <p class="f_center"><img src="https://example.test/cover.jpg" width="640" height="427"></p>
                                    <p class="f_center"><img src="https://example.test/article-2.jpg" width="640" height="427"><br>第二张图说明</p>
                                    <p>第二段包含<strong>加粗</strong>内容。</p>
                                    """,
                            LocalDateTime.of(2026, 5, 21, 20, 0));
                }
            };
        }
    }
}

package com.server.backend;

import com.server.backend.news.juhe.JuheNewsClient;
import com.server.backend.news.juhe.JuheNewsItem;
import com.server.backend.news.juhe.JuheNewsProperties;
import com.server.backend.news.juhe.JuheNewsSyncResult;
import com.server.backend.news.juhe.JuheNewsSyncService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "app.juhe.news.key=test-key",
        "app.juhe.news.category-id=1",
        "app.juhe.news.page-size=10",
        "app.juhe.news.max-pages=1",
        "app.juhe.news.default-cover-url=https://example.test/default.jpg"
})
class JuheNewsSyncServiceTest {

    @Autowired
    private JuheNewsSyncService syncService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void syncInsertsOnlyBadmintonNewsAndSkipsDuplicates() {
        JuheNewsSyncResult first = syncService.sync();
        JuheNewsSyncResult second = syncService.sync();

        assertThat(first.fetched()).isEqualTo(3);
        assertThat(first.matched()).isEqualTo(2);
        assertThat(first.inserted()).isEqualTo(2);
        assertThat(second.inserted()).isZero();

        Integer badmintonCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM news WHERE source = 'JUHE'",
                Integer.class);
        assertThat(badmintonCount).isEqualTo(2);

        Integer footballCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM news WHERE title LIKE '%足球%'",
                Integer.class);
        assertThat(footballCount).isZero();
    }

    @TestConfiguration
    @EnableConfigurationProperties(JuheNewsProperties.class)
    static class TestConfig {
        @Bean
        @Primary
        JuheNewsClient juheNewsClient() {
            return (page, pageSize) -> List.of(
                    new JuheNewsItem("juhe-1", "国羽男双晋级世界羽联巡回赛四强", "2026-05-21 08:00:00", "体育", "聚合体育",
                            "https://example.test/a", "https://example.test/a.jpg", null, null, "1"),
                    new JuheNewsItem("juhe-2", "足球联赛焦点战今晚开球", "2026-05-21 09:00:00", "体育", "聚合体育",
                            "https://example.test/b", "https://example.test/b.jpg", null, null, "1"),
                    new JuheNewsItem("juhe-3", "全英公开赛冠军复盘：女单节奏变化明显", "2026-05-21 10:00:00", "体育", "聚合体育",
                            "https://example.test/c", "", null, null, "1")
            );
        }
    }
}

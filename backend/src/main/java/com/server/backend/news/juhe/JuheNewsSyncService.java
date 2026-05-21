package com.server.backend.news.juhe;

import com.server.backend.common.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class JuheNewsSyncService {
    private final JuheNewsClient client;
    private final JuheNewsProperties properties;
    private final JdbcTemplate jdbcTemplate;

    public JuheNewsSyncService(JuheNewsClient client, JuheNewsProperties properties, JdbcTemplate jdbcTemplate) {
        this.client = client;
        this.properties = properties;
        this.jdbcTemplate = jdbcTemplate;
    }

    public JuheNewsSyncResult sync() {
        if (properties.getKey() == null || properties.getKey().isBlank()) {
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "聚合新闻 AppKey 未配置");
        }

        int fetched = 0;
        int matched = 0;
        int inserted = 0;
        int duplicates = 0;
        int maxPages = Math.max(1, properties.getMaxPages());
        int pageSize = Math.max(1, Math.min(50, properties.getPageSize()));
        for (int page = 1; page <= maxPages; page++) {
            List<JuheNewsItem> items = client.fetch(page, pageSize);
            fetched += items.size();
            for (JuheNewsItem item : items) {
                if (!matchesBadminton(item)) {
                    continue;
                }
                matched++;
                if (exists(item)) {
                    duplicates++;
                    continue;
                }
                insert(item);
                inserted++;
            }
        }
        return new JuheNewsSyncResult(fetched, matched, inserted, duplicates);
    }

    private boolean matchesBadminton(JuheNewsItem item) {
        String haystack = (item.title() + " " + item.category() + " " + item.authorName()).toLowerCase(Locale.ROOT);
        return properties.getKeywords().stream()
                .filter(keyword -> keyword != null && !keyword.isBlank())
                .anyMatch(keyword -> haystack.contains(keyword.toLowerCase(Locale.ROOT)));
    }

    private boolean exists(JuheNewsItem item) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM news WHERE source = 'JUHE' AND source_id = ?",
                Integer.class,
                item.uniquekey());
        return count != null && count > 0;
    }

    private void insert(JuheNewsItem item) {
        String cover = firstNonBlank(item.thumbnailPicS(), item.thumbnailPicS02(), item.thumbnailPicS03(), properties.getDefaultCoverUrl());
        if (cover.isBlank()) {
            cover = "https://mobile-web-design.oss-cn-beijing.aliyuncs.com/uploads/312f8966-5d01-4db8-9618-fd653bcbd731.jpg";
        }
        String author = firstNonBlank(item.authorName(), "聚合新闻");
        String content = """
                <p>%s</p>
                <p>来源：%s，发布时间：%s。</p>
                <p><a href="%s">查看原文</a></p>
                """.formatted(escape(item.title()), escape(author), escape(item.date()), escape(item.url()));
        jdbcTemplate.update("""
                INSERT INTO news(category_id, user_id, title, cover_url, summary, author, content, media_url, media_type,
                                 source, source_id, source_url, status)
                VALUES(?, NULL, ?, ?, ?, ?, ?, ?, 'IMAGE', 'JUHE', ?, ?, 'PUBLISHED')
                """,
                properties.getCategoryId(),
                item.title().trim(),
                cover,
                item.title().trim(),
                author,
                content,
                item.url(),
                item.uniquekey(),
                item.url());
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}

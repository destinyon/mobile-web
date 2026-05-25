package com.server.backend.news.netease.service;

import com.server.backend.news.netease.NeteaseNewsClient;
import com.server.backend.news.netease.NeteaseNewsDetail;
import com.server.backend.news.netease.NeteaseNewsItem;
import com.server.backend.news.netease.NeteaseNewsProperties;
import com.server.backend.news.netease.NeteaseNewsSyncResult;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class NeteaseNewsSyncService {
    private static final String DEFAULT_COVER_URL = "https://mobile-web-design.oss-cn-beijing.aliyuncs.com/uploads/312f8966-5d01-4db8-9618-fd653bcbd731.jpg";

    private final NeteaseNewsClient client;
    private final NeteaseNewsProperties properties;
    private final JdbcTemplate jdbcTemplate;
    private final NeteaseNewsContentSanitizer sanitizer;

    public NeteaseNewsSyncService(
            NeteaseNewsClient client,
            NeteaseNewsProperties properties,
            JdbcTemplate jdbcTemplate,
            NeteaseNewsContentSanitizer sanitizer) {
        this.client = client;
        this.properties = properties;
        this.jdbcTemplate = jdbcTemplate;
        this.sanitizer = sanitizer;
    }

    public NeteaseNewsSyncResult syncLatest() {
        cleanExistingNeteaseContent();
        int fetched = 0;
        int inserted = 0;
        int skipped = 0;
        int pages = Math.max(1, Math.min(10, properties.getPages()));
        Set<String> imported = new HashSet<>();
        LocalDateTime latestImportedAt = latestImportedAt();
        for (int page = 1; page <= pages; page++) {
            List<NeteaseNewsItem> items = client.fetchList(page);
            fetched += items.size();
            for (NeteaseNewsItem item : items) {
                if (!imported.add(item.sourceId())) {
                    skipped++;
                    continue;
                }
                if (exists(item.sourceId())) {
                    skipped++;
                    continue;
                }
                NeteaseNewsDetail detail = client.fetchDetail(item);
                String cover = firstNonBlank(detail.coverUrl(), properties.getDefaultCoverUrl(), DEFAULT_COVER_URL);
                String content = sanitizer.sanitize(detail.content(), cover);
                if (content.isBlank()
                        || !isNewer(detail.publishedAt(), latestImportedAt)) {
                    skipped++;
                    continue;
                }
                if (insert(detail, cover, content)) {
                    inserted++;
                } else {
                    skipped++;
                }
            }
        }
        return new NeteaseNewsSyncResult(pages, fetched, inserted, skipped);
    }

    private boolean insert(NeteaseNewsDetail item, String cover, String content) {
        LocalDateTime publishedAt = item.publishedAt() == null ? LocalDateTime.now() : item.publishedAt();
        int rows = jdbcTemplate.update("""
                INSERT IGNORE INTO news(category_id, user_id, title, cover_url, summary, author, content, media_url, media_type,
                                 source, source_id, source_url, status, created_at, updated_at)
                VALUES(?, NULL, ?, ?, ?, ?, ?, ?, 'IMAGE', 'NETEASE', ?, ?, 'PUBLISHED', ?, ?)
                """,
                properties.getCategoryId(),
                item.title(),
                cover,
                item.summary(),
                item.author(),
                content,
                cover,
                item.sourceId(),
                item.url(),
                Timestamp.valueOf(publishedAt),
                Timestamp.valueOf(publishedAt));
        return rows > 0;
    }

    private LocalDateTime latestImportedAt() {
        return jdbcTemplate.query("""
                SELECT MAX(created_at)
                FROM news
                WHERE source = 'NETEASE'
                """, rs -> rs.next() && rs.getTimestamp(1) != null ? rs.getTimestamp(1).toLocalDateTime() : null);
    }

    private boolean exists(String sourceId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM news WHERE source = 'NETEASE' AND source_id = ?",
                Integer.class,
                sourceId);
        return count != null && count > 0;
    }

    private boolean isNewer(LocalDateTime publishedAt, LocalDateTime latestImportedAt) {
        return publishedAt == null || latestImportedAt == null || publishedAt.isAfter(latestImportedAt);
    }

    private void cleanExistingNeteaseContent() {
        jdbcTemplate.query("""
                SELECT id, content, cover_url
                FROM news
                WHERE source = 'NETEASE' AND content LIKE '%<%'
                """, rs -> {
            long id = rs.getLong("id");
            String cleaned = sanitizer.sanitize(rs.getString("content"), rs.getString("cover_url"));
            if (!cleaned.isBlank()) {
                jdbcTemplate.update("UPDATE news SET content = ? WHERE id = ?", cleaned, id);
            }
        });
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }
}

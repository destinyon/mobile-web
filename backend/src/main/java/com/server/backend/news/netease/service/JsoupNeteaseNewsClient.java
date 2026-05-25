package com.server.backend.news.netease.service;

import com.server.backend.common.BusinessException;
import com.server.backend.news.netease.NeteaseNewsClient;
import com.server.backend.news.netease.NeteaseNewsDetail;
import com.server.backend.news.netease.NeteaseNewsItem;
import com.server.backend.news.netease.NeteaseNewsProperties;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class JsoupNeteaseNewsClient implements NeteaseNewsClient {
    private final NeteaseNewsProperties properties;
    private final NeteaseNewsContentSanitizer sanitizer;

    public JsoupNeteaseNewsClient(NeteaseNewsProperties properties, NeteaseNewsContentSanitizer sanitizer) {
        this.properties = properties;
        this.sanitizer = sanitizer;
    }

    @Override
    public List<NeteaseNewsItem> fetchList(int page) {
        Document document = get(listUrl(page));
        Map<String, NeteaseNewsItem> items = new LinkedHashMap<>();
        for (Element link : document.select("h3 a[href*=/dy/article/]")) {
            String href = link.absUrl("href");
            String title = link.text().trim();
            String sourceId = sourceId(href);
            if (!href.isBlank() && !title.isBlank() && !sourceId.isBlank()) {
                items.putIfAbsent(sourceId, new NeteaseNewsItem(sourceId, title, href));
            }
        }
        return List.copyOf(items.values());
    }

    @Override
    public NeteaseNewsDetail fetchDetail(NeteaseNewsItem item) {
        Document document = get(item.url());
        String title = firstNonBlank(document.selectFirst("h1") == null ? "" : document.selectFirst("h1").text(), item.title());
        String author = author(document);
        LocalDateTime publishedAt = publishedAt(document);
        Element body = document.selectFirst(".post_body");
        if (body == null) {
            return new NeteaseNewsDetail(item.sourceId(), title, item.url(), author, "", "", "", publishedAt);
        }
        body.select("script, style, iframe, .ep-source, .post_statement").remove();
        String coverUrl = "";
        Element image = body.selectFirst("img[src]");
        if (image != null) {
            coverUrl = image.absUrl("src");
        }
        body.select("img[src]").forEach(imageElement -> imageElement.attr("src", imageElement.absUrl("src")));
        String content = sanitizer.sanitize(body.html(), coverUrl);
        String summary = sanitizer.summaryText(content);
        return new NeteaseNewsDetail(item.sourceId(), title, item.url(), author, summary, coverUrl, content, publishedAt);
    }

    private Document get(String url) {
        try {
            return Jsoup.connect(url)
                    .userAgent("Mozilla/5.0")
                    .timeout(10000)
                    .get();
        } catch (IOException ex) {
            throw new BusinessException(HttpStatus.BAD_GATEWAY, "网易新闻页面暂时不可用");
        }
    }

    private String listUrl(int page) {
        String suffix = page <= 1 ? "" : "_" + String.format("%02d", page);
        return String.format(properties.getListUrlPattern(), suffix);
    }

    private String sourceId(String url) {
        int article = url.indexOf("/article/");
        int html = url.indexOf(".html", article);
        if (article < 0 || html < 0) {
            return "";
        }
        return url.substring(article + "/article/".length(), html);
    }

    private String author(Document document) {
        Element source = document.selectFirst(".post_info a");
        if (source != null && !source.text().isBlank()) {
            return source.text().trim();
        }
        String info = document.selectFirst(".post_info") == null ? "" : document.selectFirst(".post_info").text();
        int index = info.indexOf("来源:");
        if (index >= 0) {
            String text = info.substring(index + 3).replace("举报", "").trim();
            return firstNonBlank(text, "网易体育");
        }
        return "网易体育";
    }

    private LocalDateTime publishedAt(Document document) {
        String published = firstNonBlank(
                document.selectFirst("meta[property=article:published_time]") == null ? "" : document.selectFirst("meta[property=article:published_time]").attr("content"),
                document.selectFirst("[data-publishtime]") == null ? "" : document.selectFirst("[data-publishtime]").attr("data-publishtime"));
        if (published.isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(published, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toLocalDateTime();
        } catch (DateTimeParseException ignored) {
            try {
                return LocalDateTime.parse(published, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            } catch (DateTimeParseException ignoredAgain) {
                return null;
            }
        }
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

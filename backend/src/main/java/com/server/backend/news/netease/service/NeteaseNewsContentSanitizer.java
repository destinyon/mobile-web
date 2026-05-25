package com.server.backend.news.netease.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

@Component
public class NeteaseNewsContentSanitizer {
    private static final String IMAGE_STYLE = "max-width:100%;width:100%;height:auto;display:block;";
    private static final String CAPTION_STYLE = "margin:0 0 16px;color:#6a7467;font-size:13px;line-height:1.5;text-align:center;";

    public String sanitize(String content, String coverUrl) {
        if (content == null || content.isBlank()) {
            return "";
        }
        Document document = Jsoup.parseBodyFragment(content);
        document.select("script, style, iframe, object, embed, form, input, button").remove();

        StringBuilder html = new StringBuilder();
        for (Element child : document.body().children()) {
            if (child.selectFirst("img[src]") != null) {
                appendImages(html, child, coverUrl);
                continue;
            }
            String text = child.text().trim();
            if (!text.isBlank()) {
                html.append("<p>").append(escape(text)).append("</p>");
            }
        }
        if (html.isEmpty()) {
            String text = document.body().text().trim();
            return text.isBlank() ? "" : "<p>" + escape(text) + "</p>";
        }
        return html.toString();
    }

    public String summaryText(String content) {
        if (content == null || content.isBlank()) {
            return "";
        }
        String text = Jsoup.parseBodyFragment(content).body().text().trim();
        return text.length() > 180 ? text.substring(0, 180) : text;
    }

    private void appendImages(StringBuilder html, Element container, String coverUrl) {
        for (Element image : container.select("img[src]")) {
            String src = image.attr("src").trim();
            if (src.isBlank() || sameUrl(src, coverUrl)) {
                continue;
            }
            String caption = caption(container, image);
            html.append("<p><img src=\"")
                    .append(escape(src))
                    .append("\" style=\"")
                    .append(IMAGE_STYLE)
                    .append("\" mode=\"widthFix\"/></p>");
            if (!caption.isBlank()) {
                html.append("<p style=\"")
                        .append(CAPTION_STYLE)
                        .append("\">")
                        .append(escape(caption))
                        .append("</p>");
            }
        }
    }

    private String caption(Element container, Element image) {
        Element clone = container.clone();
        clone.select("img").remove();
        return clone.text().trim();
    }

    private boolean sameUrl(String left, String right) {
        return right != null && !right.isBlank() && left.trim().equals(right.trim());
    }

    private String escape(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}

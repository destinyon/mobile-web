package com.server.backend.news.netease;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.netease.news")
public class NeteaseNewsProperties {
    private long categoryId = 1;
    private int pages = 10;
    private String listUrlPattern = "https://sports.163.com/special/00051L24/ymq09%s.html";
    private String defaultCoverUrl = "";
    private boolean startupSync = true;
    private boolean scheduledSync = true;

    public long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(long categoryId) {
        this.categoryId = categoryId;
    }

    public int getPages() {
        return pages;
    }

    public void setPages(int pages) {
        this.pages = pages;
    }

    public String getListUrlPattern() {
        return listUrlPattern;
    }

    public void setListUrlPattern(String listUrlPattern) {
        this.listUrlPattern = listUrlPattern;
    }

    public String getDefaultCoverUrl() {
        return defaultCoverUrl;
    }

    public void setDefaultCoverUrl(String defaultCoverUrl) {
        this.defaultCoverUrl = defaultCoverUrl;
    }

    public boolean isStartupSync() {
        return startupSync;
    }

    public void setStartupSync(boolean startupSync) {
        this.startupSync = startupSync;
    }

    public boolean isScheduledSync() {
        return scheduledSync;
    }

    public void setScheduledSync(boolean scheduledSync) {
        this.scheduledSync = scheduledSync;
    }
}

package com.server.backend.news.netease;

public record NeteaseNewsSyncResult(int pages, int fetched, int inserted, int skipped) {
}

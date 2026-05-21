package com.server.backend.news.juhe;

public record JuheNewsSyncResult(int fetched, int matched, int inserted, int skippedDuplicates) {
}

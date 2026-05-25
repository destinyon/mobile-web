package com.server.backend.news.netease.service;

import com.server.backend.news.netease.NeteaseNewsProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class NeteaseNewsSyncScheduler implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(NeteaseNewsSyncScheduler.class);

    private final NeteaseNewsSyncService syncService;
    private final NeteaseNewsProperties properties;

    public NeteaseNewsSyncScheduler(NeteaseNewsSyncService syncService, NeteaseNewsProperties properties) {
        this.syncService = syncService;
        this.properties = properties;
    }

    @Override
    public void run(org.springframework.boot.ApplicationArguments args) {
        if (properties.isStartupSync()) {
            syncSafely("startup");
        }
    }

    @Scheduled(fixedDelayString = "${app.netease.news.sync-interval-ms:300000}")
    public void syncPeriodically() {
        if (properties.isScheduledSync()) {
            syncSafely("scheduled");
        }
    }

    private void syncSafely(String trigger) {
        try {
            syncService.syncLatest();
        } catch (RuntimeException ex) {
            log.warn("Netease news {} sync failed: {}", trigger, ex.getMessage());
        }
    }
}

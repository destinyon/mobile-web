package com.server.backend;

import com.server.backend.news.netease.NeteaseNewsProperties;
import com.server.backend.news.netease.service.NeteaseNewsSyncScheduler;
import com.server.backend.news.netease.service.NeteaseNewsSyncService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class NeteaseNewsSyncSchedulerTest {
    @Test
    void startupAndScheduledSyncRespectSwitches() {
        NeteaseNewsSyncService syncService = mock(NeteaseNewsSyncService.class);
        NeteaseNewsProperties properties = new NeteaseNewsProperties();
        NeteaseNewsSyncScheduler scheduler = new NeteaseNewsSyncScheduler(syncService, properties);

        scheduler.run(new DefaultApplicationArguments());
        scheduler.syncPeriodically();

        verify(syncService, times(2)).syncLatest();

        properties.setStartupSync(false);
        properties.setScheduledSync(false);
        scheduler.run(new DefaultApplicationArguments());
        scheduler.syncPeriodically();

        verify(syncService, times(2)).syncLatest();
    }
}

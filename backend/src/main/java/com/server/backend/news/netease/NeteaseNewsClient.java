package com.server.backend.news.netease;

import java.util.List;

public interface NeteaseNewsClient {
    List<NeteaseNewsItem> fetchList(int page);

    NeteaseNewsDetail fetchDetail(NeteaseNewsItem item);
}

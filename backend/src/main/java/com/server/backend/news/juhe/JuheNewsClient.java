package com.server.backend.news.juhe;

import java.util.List;

public interface JuheNewsClient {
    List<JuheNewsItem> fetch(int page, int pageSize);
}

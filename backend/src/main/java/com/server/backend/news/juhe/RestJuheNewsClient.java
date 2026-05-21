package com.server.backend.news.juhe;

import com.server.backend.common.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class RestJuheNewsClient implements JuheNewsClient {
    private final RestClient restClient;
    private final JuheNewsProperties properties;

    public RestJuheNewsClient(JuheNewsProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.create();
    }

    @Override
    public List<JuheNewsItem> fetch(int page, int pageSize) {
        Map<?, ?> response;
        try {
            response = restClient.get()
                    .uri(properties.getUrl() + "?key={key}&type={type}&page={page}&page_size={pageSize}&is_filter=1",
                            properties.getKey(), properties.getType(), page, pageSize)
                    .retrieve()
                    .body(Map.class);
        } catch (Exception ex) {
            throw new BusinessException(HttpStatus.BAD_GATEWAY, "聚合新闻服务暂时不可用");
        }
        if (response == null || !"0".equals(String.valueOf(response.get("error_code")))) {
            throw new BusinessException(HttpStatus.BAD_GATEWAY, "聚合新闻同步失败");
        }
        Object result = response.get("result");
        if (!(result instanceof Map<?, ?> resultMap)) {
            return List.of();
        }
        Object data = resultMap.get("data");
        if (!(data instanceof List<?> rows)) {
            return List.of();
        }
        List<JuheNewsItem> items = new ArrayList<>();
        for (Object row : rows) {
            if (row instanceof Map<?, ?> map) {
                items.add(new JuheNewsItem(
                        text(map.get("uniquekey")),
                        text(map.get("title")),
                        text(map.get("date")),
                        text(map.get("category")),
                        text(map.get("author_name")),
                        text(map.get("url")),
                        text(map.get("thumbnail_pic_s")),
                        text(map.get("thumbnail_pic_s02")),
                        text(map.get("thumbnail_pic_s03")),
                        text(map.get("is_content"))
                ));
            }
        }
        return items;
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}

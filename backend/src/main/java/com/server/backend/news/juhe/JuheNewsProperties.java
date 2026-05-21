package com.server.backend.news.juhe;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app.juhe.news")
public class JuheNewsProperties {
    private boolean enabled = false;
    private String key = "";
    private String url = "https://v.juhe.cn/toutiao/index";
    private String type = "tiyu";
    private long categoryId = 1;
    private int pageSize = 30;
    private int maxPages = 2;
    private String defaultCoverUrl = "";
    private List<String> keywords = List.of(
            "羽毛球", "国羽", "羽联", "世界羽联", "BWF", "苏迪曼杯", "汤姆斯杯", "尤伯杯", "全英公开赛", "中国公开赛",
            "印尼公开赛", "马来西亚公开赛", "新加坡公开赛", "日本公开赛", "韩国公开赛", "丹麦公开赛", "法国公开赛", "亚锦赛",
            "世锦赛", "奥运羽毛球", "男单", "女单", "男双", "女双", "混双", "石宇奇", "陈雨菲", "何冰娇", "韩悦",
            "李诗沣", "梁伟铿", "王昶", "郑思维", "黄雅琼", "冯彦哲", "黄东萍", "陈清晨", "贾一凡", "刘圣书",
            "谭宁", "安赛龙", "桃田贤斗", "戴资颖", "山口茜", "安洗莹", "李梓嘉", "骆建佑", "昆拉武特", "因达农", "辛杜"
    );

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(long categoryId) {
        this.categoryId = categoryId;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public int getMaxPages() {
        return maxPages;
    }

    public void setMaxPages(int maxPages) {
        this.maxPages = maxPages;
    }

    public String getDefaultCoverUrl() {
        return defaultCoverUrl;
    }

    public void setDefaultCoverUrl(String defaultCoverUrl) {
        this.defaultCoverUrl = defaultCoverUrl;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
    }
}

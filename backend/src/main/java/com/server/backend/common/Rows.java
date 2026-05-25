package com.server.backend.common;

import com.server.backend.comment.dto.CommentNode;
import com.server.backend.news.dto.NewsDetail;
import com.server.backend.news.dto.NewsSummary;
import com.server.backend.post.dto.PostDetail;
import com.server.backend.post.dto.PostSummary;
import com.server.backend.post.dto.TopicItem;
import org.springframework.jdbc.core.RowMapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class Rows {
    private Rows() {
    }

    public static RowMapper<NewsSummary> newsSummary(long userId) {
        return (rs, rowNum) -> new NewsSummary(
                rs.getLong("id"),
                rs.getLong("category_id"),
                rs.getString("category_name"),
                rs.getString("title"),
                rs.getString("cover_url"),
                rs.getString("summary"),
                rs.getString("author"),
                rs.getInt("view_count"),
                rs.getInt("like_count"),
                rs.getInt("favorite_count"),
                rs.getInt("comment_count"),
                rs.getBoolean("favorited"),
                rs.getTimestamp("updated_at").toLocalDateTime()
        );
    }

    public static RowMapper<NewsDetail> newsDetail() {
        return (rs, rowNum) -> new NewsDetail(
                rs.getLong("id"),
                rs.getLong("category_id"),
                rs.getString("category_name"),
                rs.getString("title"),
                rs.getString("cover_url"),
                rs.getString("summary"),
                rs.getString("author"),
                rs.getString("content"),
                rs.getString("media_url"),
                rs.getString("media_type"),
                rs.getInt("view_count"),
                rs.getInt("like_count"),
                rs.getInt("favorite_count"),
                rs.getInt("comment_count"),
                rs.getBoolean("favorited"),
                rs.getTimestamp("updated_at").toLocalDateTime(),
                new ArrayList<>()
        );
    }

    public static RowMapper<CommentNode> commentNode() {
        return (rs, rowNum) -> new CommentNode(
                rs.getLong("id"),
                rs.getString("target_type"),
                rs.getLong("target_id"),
                rs.getObject("parent_id", Long.class),
                rs.getLong("user_id"),
                rs.getString("nickname"),
                rs.getString("avatar_url"),
                rs.getString("content"),
                rs.getTimestamp("created_at").toLocalDateTime(),
                new ArrayList<>()
        );
    }

    public static RowMapper<TopicItem> topicItem() {
        return (rs, rowNum) -> new TopicItem(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("description")
        );
    }

    public static RowMapper<PostSummary> postSummary(long userId) {
        return (rs, rowNum) -> new PostSummary(
                rs.getLong("id"),
                rs.getLong("topic_id"),
                rs.getString("topic_name"),
                rs.getLong("user_id"),
                rs.getString("nickname"),
                rs.getString("avatar_url"),
                rs.getString("title"),
                rs.getString("content"),
                splitImages(rs.getString("images")),
                rs.getInt("view_count"),
                rs.getInt("like_count"),
                rs.getInt("favorite_count"),
                rs.getInt("comment_count"),
                rs.getBoolean("favorited"),
                rs.getString("status"),
                rs.getTimestamp("updated_at").toLocalDateTime()
        );
    }

    public static RowMapper<PostDetail> postDetail() {
        return (rs, rowNum) -> new PostDetail(
                rs.getLong("id"),
                rs.getLong("topic_id"),
                rs.getString("topic_name"),
                rs.getLong("user_id"),
                rs.getString("nickname"),
                rs.getString("avatar_url"),
                rs.getString("title"),
                rs.getString("content"),
                splitImages(rs.getString("images")),
                rs.getInt("view_count"),
                rs.getInt("like_count"),
                rs.getInt("favorite_count"),
                rs.getInt("comment_count"),
                rs.getBoolean("favorited"),
                rs.getString("status"),
                rs.getTimestamp("updated_at").toLocalDateTime(),
                new ArrayList<>()
        );
    }

    private static List<String> splitImages(String images) {
        if (images == null || images.isBlank() || "[]".equals(images.trim())) {
            return new ArrayList<>();
        }
        return Arrays.stream(images.split("\\n"))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .toList();
    }
}

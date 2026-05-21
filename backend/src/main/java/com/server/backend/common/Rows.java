package com.server.backend.common;

import com.server.backend.comment.CommentNode;
import com.server.backend.news.NewsDetail;
import com.server.backend.news.NewsSummary;
import org.springframework.jdbc.core.RowMapper;

import java.util.ArrayList;

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
}

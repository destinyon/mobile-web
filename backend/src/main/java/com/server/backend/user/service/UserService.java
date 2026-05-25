package com.server.backend.user.service;

import com.server.backend.comment.dto.UserCommentItem;
import com.server.backend.comment.service.CommentService;
import com.server.backend.news.service.NewsService;
import com.server.backend.news.dto.NewsSummary;
import com.server.backend.post.dto.PostSummary;
import com.server.backend.post.service.PostService;
import com.server.backend.user.dto.BrowseHistoryItem;
import com.server.backend.user.dto.UpdateProfileRequest;
import com.server.backend.user.dto.UserProfile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {
    private final JdbcTemplate jdbcTemplate;
    private final NewsService newsService;
    private final CommentService commentService;
    private final PostService postService;
    private final BrowseHistoryService browseHistoryService;

    public UserService(
            JdbcTemplate jdbcTemplate,
            NewsService newsService,
            CommentService commentService,
            PostService postService,
            BrowseHistoryService browseHistoryService) {
        this.jdbcTemplate = jdbcTemplate;
        this.newsService = newsService;
        this.commentService = commentService;
        this.postService = postService;
        this.browseHistoryService = browseHistoryService;
    }

    public UserProfile profile(long userId) {
        return jdbcTemplate.queryForObject("""
                SELECT id, nickname, avatar_url, phone, age, play_years, gender, role
                FROM users WHERE id = ?
                """, (rs, rowNum) -> new UserProfile(
                rs.getLong("id"),
                rs.getString("nickname"),
                rs.getString("avatar_url"),
                rs.getString("phone"),
                rs.getObject("age", Integer.class),
                rs.getObject("play_years", Integer.class),
                rs.getString("gender"),
                rs.getString("role")
        ), userId);
    }

    public UserProfile update(long userId, UpdateProfileRequest request) {
        UserProfile current = profile(userId);
        jdbcTemplate.update("""
                UPDATE users
                SET nickname = ?, avatar_url = ?, phone = ?, age = ?, play_years = ?, gender = ?, updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """,
                pick(request.nickname(), current.nickname()),
                pick(request.avatarUrl(), current.avatarUrl()),
                pick(request.phone(), current.phone()),
                request.age() == null ? current.age() : request.age(),
                request.playYears() == null ? current.playYears() : request.playYears(),
                pick(request.gender(), current.gender()),
                userId);
        return profile(userId);
    }

    public List<NewsSummary> favorites(long userId) {
        return newsService.favorites(userId);
    }

    public List<UserCommentItem> comments(long userId) {
        return commentService.listByUser(userId);
    }

    public List<PostSummary> posts(long userId) {
        return postService.postsByUser(userId);
    }

    public List<BrowseHistoryItem> history(long userId) {
        return browseHistoryService.list(userId);
    }

    private String pick(String next, String current) {
        return next == null || next.isBlank() ? current : next.trim();
    }
}

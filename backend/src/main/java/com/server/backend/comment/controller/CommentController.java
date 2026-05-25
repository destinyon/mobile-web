package com.server.backend.comment.controller;

import com.server.backend.auth.service.AuthService;
import com.server.backend.comment.dto.CommentNode;
import com.server.backend.comment.dto.CreateCommentRequest;
import com.server.backend.comment.service.CommentService;
import com.server.backend.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/comments")
public class CommentController {
    private final CommentService commentService;
    private final AuthService authService;

    public CommentController(CommentService commentService, AuthService authService) {
        this.commentService = commentService;
        this.authService = authService;
    }

    @PostMapping
    public ApiResponse<CommentNode> create(
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody CreateCommentRequest request) {
        return ApiResponse.ok(commentService.create(authService.requireUser(authorization), request));
    }
}

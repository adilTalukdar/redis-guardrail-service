package com.grid.app.controller;

import com.grid.app.dto.*;
import com.grid.app.service.CommentService;
import com.grid.app.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final CommentService commentService;

    @PostMapping
    public ResponseEntity<PostResponse> createPost(@RequestBody CreatePostRequest request) {
        PostResponse response = postService.createPost(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{postId}/comments")
    public ResponseEntity<CommentResponse> addComment(
            @PathVariable Long postId,
            @RequestBody CreateCommentRequest request) {
        CommentResponse response = commentService.addComment(postId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{postId}/like")
    public ResponseEntity<LikePostResponse> likePost(
            @PathVariable Long postId,
            @RequestBody LikePostRequest request) {
        LikePostResponse response = postService.likePost(postId, request);
        return ResponseEntity.ok(response);
    }
}

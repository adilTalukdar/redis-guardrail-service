package com.grid.app.service;

import com.grid.app.dto.CreatePostRequest;
import com.grid.app.dto.LikePostRequest;
import com.grid.app.dto.LikePostResponse;
import com.grid.app.dto.PostResponse;
import com.grid.app.model.Post;
import com.grid.app.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final ViralityService viralityService;

    public PostResponse createPost(CreatePostRequest request) {
        Post post = Post.builder()
                .authorId(request.getAuthorId())
                .authorType(request.getAuthorType())
                .content(request.getContent())
                .build();
        Post saved = postRepository.save(post);
        return PostResponse.builder()
                .id(saved.getId())
                .authorId(saved.getAuthorId())
                .authorType(saved.getAuthorType())
                .content(saved.getContent())
                .createdAt(saved.getCreatedAt())
                .build();
    }

    public LikePostResponse likePost(Long postId, LikePostRequest request) {
        postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found with id: " + postId));
        Long newScore = viralityService.updateViralityScore(postId, "HUMAN_LIKE");
        return LikePostResponse.builder()
                .postId(postId)
                .userId(request.getUserId())
                .viralityScore(newScore)
                .build();
    }
}

package com.grid.app.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentResponse {
    private Long id;
    private Long postId;
    private Long authorId;
    private String authorType;
    private Long botId;
    private String content;
    private int depthLevel;
    private LocalDateTime createdAt;
}

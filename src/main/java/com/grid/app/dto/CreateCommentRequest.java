package com.grid.app.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCommentRequest {
    private Long authorId;
    private String authorType;
    private Long botId;
    private Long userId;
    private String content;
    private int depthLevel;
}

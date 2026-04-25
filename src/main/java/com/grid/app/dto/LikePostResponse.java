package com.grid.app.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LikePostResponse {
    private Long postId;
    private Long userId;
    private Long viralityScore;
}

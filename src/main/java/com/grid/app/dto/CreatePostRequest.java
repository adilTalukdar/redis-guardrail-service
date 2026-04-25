package com.grid.app.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePostRequest {
    private Long authorId;
    private String authorType;
    private String content;
}

package com.grid.app.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostResponse {
    private Long id;
    private Long authorId;
    private String authorType;
    private String content;
    private LocalDateTime createdAt;
}

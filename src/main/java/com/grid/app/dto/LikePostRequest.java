package com.grid.app.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LikePostRequest {
    private Long userId;
}

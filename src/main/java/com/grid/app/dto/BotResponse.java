package com.grid.app.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BotResponse {
    private Long id;
    private String name;
    private String personaDescription;
}

package com.grid.app.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateBotRequest {
    private String name;
    private String personaDescription;
}

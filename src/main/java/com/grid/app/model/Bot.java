package com.grid.app.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "bots")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Bot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "persona_description", columnDefinition = "TEXT")
    private String personaDescription;
}

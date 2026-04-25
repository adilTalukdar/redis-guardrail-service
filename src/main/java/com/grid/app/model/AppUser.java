package com.grid.app.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "app_users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(name = "is_premium", nullable = false)
    private boolean isPremium;
}

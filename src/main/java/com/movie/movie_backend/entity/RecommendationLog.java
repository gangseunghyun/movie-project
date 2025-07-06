package com.movie.movie_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "recommendation_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecommendationLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "movie_id", nullable = false)
    private Long movieId;

    @Column(nullable = false)
    private int score;

    @Column(length = 255)
    private String reasons;

    @Column(name = "recommended_at", nullable = false)
    private LocalDateTime recommendedAt;

    @PrePersist
    public void prePersist() {
        if (recommendedAt == null) {
            recommendedAt = LocalDateTime.now();
        }
    }
} 
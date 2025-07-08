package com.movie.movie_backend.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ReviewResponseDto {
    private Long id;
    private String content;
    private Double rating;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String username;
    private Long userId;
    private Long movieDetailId;
    private int likeCount;
    private boolean likedByMe;
    private int commentCount;
} 
package com.movie.movie_backend.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ReviewResponseDto {
    private Long id;
    private String content;
    private Integer rating;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String username;
    private Long userId;
    private Long movieId;
    private int likeCount;
    private boolean likedByMe;
    private int commentCount;
} 
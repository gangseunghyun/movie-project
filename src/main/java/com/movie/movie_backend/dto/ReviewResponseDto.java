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
    private String userProfileImageUrl;
    private Long movieDetailId;
    private String movieCd;
    private String movieNm;
    private String posterUrl; // 포스터 URL 추가
    private int likeCount;
    private boolean likedByMe;
    private int commentCount;
} 
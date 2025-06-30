package com.movie.movie_backend.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RatingDto {
    private Long id;
    private String movieCd;
    private String movieNm;
    private Double score; // 0.5~5.0점
    private LocalDateTime createdAt;
    private String userEmail;
    private String userNickname;
    
    // 별점 표시용
    public String getStarDisplay() {
        if (score == null) return "☆☆☆☆☆";
        int fullStars = (int)Math.round(score);
        return "★".repeat(fullStars) + "☆".repeat(5 - fullStars);
    }
    
    // 별점 텍스트
    public String getScoreText() {
        if (score == null) return "평점 없음";
        return score + "점";
    }
} 
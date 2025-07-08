package com.movie.movie_backend.dto;

import lombok.*;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ReviewDto {
    private Long id;
    private String content;
    private Integer rating;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long userId;
    private String userNickname;
    private String movieCd;
    private String movieNm;
    private int likeCount;
    private boolean likedByMe;
    private int commentCount;
    @JsonProperty("blockedByCleanbot")
    private boolean blockedByCleanbot;

    public boolean isBlockedByCleanbot() {
        return blockedByCleanbot;
    }

    public static ReviewDto fromEntity(com.movie.movie_backend.entity.Review review) {
        return ReviewDto.builder()
            .id(review.getId())
            .content(review.getContent())
            .rating(review.getRating())
            .status(review.getStatus() != null ? review.getStatus().name() : null)
            .createdAt(review.getCreatedAt())
            .updatedAt(review.getUpdatedAt())
            .userId(review.getUser() != null ? review.getUser().getId() : null)
            .userNickname(review.getUser() != null ? review.getUser().getNickname() : null)
            .movieCd(review.getMovieDetail() != null ? review.getMovieDetail().getMovieCd() : null)
            .movieNm(review.getMovieDetail() != null ? review.getMovieDetail().getMovieNm() : null)
            .blockedByCleanbot(review.isBlockedByCleanbot())
            .likeCount(0)
            .commentCount(0)
            .likedByMe(false)
            .build();
    }
} 
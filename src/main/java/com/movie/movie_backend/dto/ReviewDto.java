package com.movie.movie_backend.dto;

import lombok.*;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ReviewDto {
    private Long id;
    private String content;
    private Double rating;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long userId;
    private String userNickname;
    private String userProfileImageUrl;
    private String movieCd;
    private String movieNm;
    private String posterUrl; // 포스터 URL 추가
    private int likeCount;
    private boolean likedByMe;
    private int commentCount;
    @JsonProperty("blockedByCleanbot")
    private boolean blockedByCleanbot;
    /**
     * movieDetailId: MovieDetail 엔티티의 @Id, @Column(name = "movie_detail_id")와 매핑됨
     * 즉, movie_detail 테이블의 PK
     */
    private Long movieDetailId;

    public boolean isBlockedByCleanbot() {
        return blockedByCleanbot;
    }

    public static ReviewDto fromEntity(com.movie.movie_backend.entity.Review review) {
        // MovieList에서 포스터 URL 가져오기
        String posterUrl = null;
        if (review.getMovieDetail() != null && review.getMovieDetail().getMovieList() != null) {
            posterUrl = review.getMovieDetail().getMovieList().getPosterUrl();
        }
        
        return ReviewDto.builder()
            .id(review.getId())
            .content(review.getContent())
            .rating(review.getRating())
            .status(review.getStatus() != null ? review.getStatus().name() : null)
            .createdAt(review.getCreatedAt())
            .updatedAt(review.getUpdatedAt())
            .userId(review.getUser() != null ? review.getUser().getId() : null)
            .userNickname(review.getUser() != null ? review.getUser().getNickname() : null)
            .userProfileImageUrl(review.getUser() != null ? review.getUser().getProfileImageUrl() : null)
            .movieCd(review.getMovieDetail() != null ? review.getMovieDetail().getMovieCd() : null)
            .movieNm(review.getMovieDetail() != null ? review.getMovieDetail().getMovieNm() : null)
            .posterUrl(posterUrl) // 포스터 URL 설정
            .movieDetailId(review.getMovieDetail() != null ? review.getMovieDetail().getId() : null)
            .blockedByCleanbot(review.isBlockedByCleanbot())
            .likeCount(0)
            .commentCount(0)
            .likedByMe(false)
            .build();
    }
} 
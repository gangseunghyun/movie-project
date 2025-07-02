package com.movie.movie_backend.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ReviewRequestDto {
    private String content;
    private Integer rating;
    private Long movieId;
    private Long parentId; // 대댓글(부모 리뷰)용, 일반 리뷰면 null
    private Long userId;
} 
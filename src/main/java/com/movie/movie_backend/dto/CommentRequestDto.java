package com.movie.movie_backend.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CommentRequestDto {
    private String content;
    private Long reviewId;
    private Long parentId; // 대댓글(부모 댓글)용, 일반 댓글이면 null
} 
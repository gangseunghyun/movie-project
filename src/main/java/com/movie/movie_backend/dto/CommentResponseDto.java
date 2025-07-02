package com.movie.movie_backend.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CommentResponseDto {
    private Long id;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String username;
    private Long userId;
    private Long reviewId;
    private Long parentId;
    private int likeCount;
    private boolean likedByMe;
    private List<CommentResponseDto> replies; // 대댓글
} 
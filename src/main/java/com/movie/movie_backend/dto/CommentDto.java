package com.movie.movie_backend.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CommentDto {
    private Long id;
    private String content;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long userId;
    private String userNickname;
    private Long reviewId;
    private Long parentId;
    private boolean isReply;
    private Long likeCount;
    private boolean likedByMe;
    private java.util.List<CommentDto> replies;

    public static CommentDto fromEntity(com.movie.movie_backend.entity.Comment comment) {
        return CommentDto.builder()
            .id(comment.getId())
            .content(comment.getContent())
            .status(comment.getStatus() != null ? comment.getStatus().name() : null)
            .createdAt(comment.getCreatedAt())
            .updatedAt(comment.getUpdatedAt())
            .userId(comment.getUser() != null ? comment.getUser().getId() : null)
            .userNickname(comment.getUser() != null ? comment.getUser().getNickname() : null)
            .reviewId(comment.getReview() != null ? comment.getReview().getId() : null)
            .parentId(comment.getParent() != null ? comment.getParent().getId() : null)
            .isReply(comment.isReply())
            .likeCount(0L) // 기본값
            .likedByMe(false) // 기본값
            .build();
    }

    public static CommentDto fromEntityWithLikeInfo(com.movie.movie_backend.entity.Comment comment, 
                                                   Long likeCount, 
                                                   boolean likedByMe) {
        return CommentDto.builder()
            .id(comment.getId())
            .content(comment.getContent())
            .status(comment.getStatus() != null ? comment.getStatus().name() : null)
            .createdAt(comment.getCreatedAt())
            .updatedAt(comment.getUpdatedAt())
            .userId(comment.getUser() != null ? comment.getUser().getId() : null)
            .userNickname(comment.getUser() != null ? comment.getUser().getNickname() : null)
            .reviewId(comment.getReview() != null ? comment.getReview().getId() : null)
            .parentId(comment.getParent() != null ? comment.getParent().getId() : null)
            .isReply(comment.isReply())
            .likeCount(likeCount)
            .likedByMe(likedByMe)
            .build();
    }
} 
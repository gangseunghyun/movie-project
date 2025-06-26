package com.movie.movie_backend.entity;

import lombok.*;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Table(name = "reviews")
public class Review {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 리뷰 고유 ID

    @Column(columnDefinition = "TEXT")
    private String content; // 리뷰 내용 (null 가능 - 평점만 달 수도 있음)
    
    private Integer rating; // 평점 (1~5, null 가능 - 댓글만 달 수도 있음)
    
    private LocalDateTime createdAt; // 작성 시각
    private LocalDateTime updatedAt; // 수정 시각

    @Enumerated(EnumType.STRING)
    private ReviewStatus status; // 리뷰 상태 (활성, 삭제됨, 신고됨)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user; // 리뷰 작성자

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_detail_id")
    private MovieDetail movieDetail; // 리뷰가 달린 영화 상세정보

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Review parent; // 부모 리뷰(대댓글 구조)

    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReviewLike> likes; // 리뷰에 달린 좋아요 목록

    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> comments; // 리뷰에 달린 댓글 목록

    // 리뷰 타입 확인 메서드
    public boolean hasContent() {
        return content != null && !content.trim().isEmpty();
    }

    public boolean hasRating() {
        return rating != null && rating >= 1 && rating <= 5;
    }

    public boolean isContentOnly() {
        return hasContent() && !hasRating();
    }

    public boolean isRatingOnly() {
        return hasRating() && !hasContent();
    }

    public boolean isFullReview() {
        return hasContent() && hasRating();
    }

    // 평점 표시 메서드
    public String getRatingDisplay() {
        if (!hasRating()) return null;
        return "★".repeat(rating) + "☆".repeat(5 - rating);
    }

    // 리뷰 상태 enum
    public enum ReviewStatus {
        ACTIVE("활성"),
        DELETED("삭제됨"),
        REPORTED("신고됨");

        private final String displayName;

        ReviewStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
} 

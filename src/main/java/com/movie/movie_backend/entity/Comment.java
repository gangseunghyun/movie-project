package com.movie.movie_backend.entity;

import lombok.*;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Table(name = "comment")
public class Comment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 댓글 고유 ID

    @Column(columnDefinition = "TEXT")
    private String content; // 댓글 내용
    
    private LocalDateTime createdAt; // 작성 시각
    private LocalDateTime updatedAt; // 수정 시각

    @Enumerated(EnumType.STRING)
    private CommentStatus status; // 댓글 상태 (활성, 삭제됨, 신고됨)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user; // 댓글 작성자

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id")
    private Review review; // 댓글이 달린 리뷰

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Comment parent; // 부모 댓글(대댓글 구조)

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> replies; // 대댓글 목록

    @OneToMany(mappedBy = "comment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CommentLike> likes; // 댓글에 달린 좋아요 목록

    @Column(nullable = false)
    private boolean isBlockedByCleanbot = false;

    // 댓글 타입 확인 메서드
    public boolean isReply() {
        return parent != null;
    }

    public boolean isTopLevel() {
        return parent == null;
    }

    // 댓글 상태 enum
    public enum CommentStatus {
        ACTIVE("활성"),
        DELETED("삭제됨"),
        REPORTED("신고됨");

        private final String displayName;

        CommentStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
} 

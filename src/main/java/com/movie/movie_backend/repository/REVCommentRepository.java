package com.movie.movie_backend.repository;

import com.movie.movie_backend.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface REVCommentRepository extends JpaRepository<Comment, Long> {

    // 리뷰별 댓글 조회 (최상위 댓글만, 최신순)
    List<Comment> findByReviewIdAndParentIsNullOrderByCreatedAtDesc(Long reviewId);

    // 리뷰별 모든 댓글 조회 (최신순)
    List<Comment> findByReviewIdOrderByCreatedAtDesc(Long reviewId);

    // 특정 댓글의 대댓글 조회 (최신순)
    List<Comment> findByParentIdOrderByCreatedAtAsc(Long parentId);

    // 사용자별 댓글 조회 (최신순)
    List<Comment> findByUserIdOrderByCreatedAtDesc(Long userId);

    // 사용자가 특정 리뷰에 작성한 댓글 조회
    List<Comment> findByUserIdAndReviewId(Long userId, Long reviewId);

    // 댓글 개수 조회
    @Query("SELECT COUNT(c) FROM Comment c WHERE c.review.id = :reviewId AND c.status = 'ACTIVE'")
    Long getCommentCountByReviewId(@Param("reviewId") Long reviewId);

    // 대댓글 개수 조회
    @Query("SELECT COUNT(c) FROM Comment c WHERE c.parent.id = :parentId AND c.status = 'ACTIVE'")
    Long getReplyCountByParentId(@Param("parentId") Long parentId);

    // 활성 상태의 댓글만 조회
    List<Comment> findByReviewIdAndStatusOrderByCreatedAtDesc(Long reviewId, Comment.CommentStatus status);

    int countByReviewIdAndParentIsNull(Long reviewId);
    List<Comment> findByReviewIdAndParentIsNullOrderByCreatedAtAsc(Long reviewId);
} 

package com.movie.repository;

import com.movie.entity.CommentLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface REVCommentLikeRepository extends JpaRepository<CommentLike, Long> {

    // 댓글별 좋아요 개수 조회
    @Query("SELECT COUNT(cl) FROM CommentLike cl WHERE cl.comment.id = :commentId")
    Long getLikeCountByCommentId(@Param("commentId") Long commentId);

    // 사용자가 특정 댓글에 좋아요를 눌렀는지 확인
    Optional<CommentLike> findByUserIdAndCommentId(Long userId, Long commentId);

    // 사용자가 누른 댓글 좋아요 목록 조회
    List<CommentLike> findByUserIdOrderByCreatedAtDesc(Long userId);

    // 댓글별 좋아요 목록 조회
    List<CommentLike> findByCommentIdOrderByCreatedAtDesc(Long commentId);

    // 댓글 삭제 시 관련 좋아요도 삭제
    void deleteByCommentId(Long commentId);

    // 사용자가 특정 댓글에 좋아요를 눌렀는지 확인 (boolean 반환)
    @Query("SELECT CASE WHEN COUNT(cl) > 0 THEN true ELSE false END FROM CommentLike cl WHERE cl.user.id = :userId AND cl.comment.id = :commentId")
    boolean existsByUserIdAndCommentId(@Param("userId") Long userId, @Param("commentId") Long commentId);
} 
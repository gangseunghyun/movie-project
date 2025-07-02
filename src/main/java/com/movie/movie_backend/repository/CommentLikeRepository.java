package com.movie.movie_backend.repository;

import com.movie.movie_backend.entity.CommentLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {
    int countByCommentId(Long commentId);
    boolean existsByCommentIdAndUserId(Long commentId, Long userId);
    void deleteByCommentIdAndUserId(Long commentId, Long userId);
    
    // 사용자가 좋아요한 코멘트 목록 조회 (내가 작성한 코멘트 제외)
    @Query("SELECT cl FROM CommentLike cl " +
           "JOIN FETCH cl.comment c " +
           "JOIN FETCH c.user cu " +
           "JOIN FETCH c.review r " +
           "JOIN FETCH r.movieDetail md " +
           "WHERE cl.user.id = :userId " +
           "AND c.user.id != :userId " +
           "ORDER BY cl.createdAt DESC")
    List<CommentLike> findLikedCommentsByUserIdExcludingOwn(@Param("userId") Long userId);
    
    // 사용자가 좋아요한 모든 코멘트 목록 조회 (디버깅용)
    @Query("SELECT cl FROM CommentLike cl " +
           "JOIN FETCH cl.comment c " +
           "JOIN FETCH c.user cu " +
           "JOIN FETCH c.review r " +
           "JOIN FETCH r.movieDetail md " +
           "WHERE cl.user.id = :userId " +
           "ORDER BY cl.createdAt DESC")
    List<CommentLike> findAllLikedCommentsByUserId(@Param("userId") Long userId);
} 
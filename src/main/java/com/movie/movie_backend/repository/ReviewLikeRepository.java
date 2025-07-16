package com.movie.movie_backend.repository;

import com.movie.movie_backend.entity.ReviewLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReviewLikeRepository extends JpaRepository<ReviewLike, Long> {
    int countByReviewId(Long reviewId);
    boolean existsByReviewIdAndUserId(Long reviewId, Long userId);
    void deleteByReviewIdAndUserId(Long reviewId, Long userId);

    // 사용자가 좋아요한 리뷰 목록 조회 (내가 쓴 리뷰 제외)
    @Query("SELECT rl FROM ReviewLike rl " +
           "JOIN FETCH rl.review r " +
           "JOIN FETCH r.movieDetail md " +
           "JOIN FETCH r.user ru " +
           "WHERE rl.user.id = :userId " +
           "AND r.user.id != :userId " +
           "ORDER BY rl.createdAt DESC")
    List<ReviewLike> findLikedReviewsByUserIdExcludingOwn(@Param("userId") Long userId);

    // 특정 리뷰를 좋아요한 모든 ReviewLike 엔티티 조회
    List<ReviewLike> findByReviewId(Long reviewId);
} 
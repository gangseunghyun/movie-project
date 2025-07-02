package com.movie.movie_backend.repository;

import com.movie.movie_backend.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface REVReviewRepository extends JpaRepository<Review, Long> {

    // 영화별 리뷰 조회 (최신순, 삭제되지 않은 것만)
    List<Review> findByMovieDetailMovieCdAndStatusOrderByCreatedAtDesc(String movieCd, Review.ReviewStatus status);

    // 영화별 평점이 있는 리뷰만 조회 (최신순, 삭제되지 않은 것만)
    List<Review> findByMovieDetailMovieCdAndRatingIsNotNullAndStatusOrderByCreatedAtDesc(String movieCd, Review.ReviewStatus status);

    // 영화별 댓글만 있는 리뷰 조회 (평점 없음, 최신순, 삭제되지 않은 것만)
    List<Review> findByMovieDetailMovieCdAndRatingIsNullAndStatusOrderByCreatedAtDesc(String movieCd, Review.ReviewStatus status);

    // 영화별 평균 별점 (rating이 있는 리뷰만, 삭제되지 않은 것만)
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.movieDetail.movieCd = :movieCd AND r.rating IS NOT NULL AND r.status = :status")
    Double getAverageRatingByMovieCd(@Param("movieCd") String movieCd, @Param("status") Review.ReviewStatus status);

    // 영화별 별점 분포 (rating이 있는 리뷰만, 삭제되지 않은 것만)
    @Query("SELECT r.rating, COUNT(r) FROM Review r WHERE r.movieDetail.movieCd = :movieCd AND r.rating IS NOT NULL AND r.status = :status GROUP BY r.rating ORDER BY r.rating")
    List<Object[]> getRatingDistributionByMovieCd(@Param("movieCd") String movieCd, @Param("status") Review.ReviewStatus status);

    // 영화별 평점 개수 조회 (삭제되지 않은 것만)
    @Query("SELECT COUNT(r) FROM Review r WHERE r.movieDetail.movieCd = :movieCd AND r.rating IS NOT NULL AND r.status = :status")
    Long getRatingCountByMovieCd(@Param("movieCd") String movieCd, @Param("status") Review.ReviewStatus status);

    // 사용자별 리뷰 조회 (최신순, 삭제되지 않은 것만)
    List<Review> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, Review.ReviewStatus status);

    // 사용자가 특정 영화에 작성한 리뷰 조회 (삭제되지 않은 것만)
    Review findByUserIdAndMovieDetailMovieCdAndStatus(Long userId, String movieCd, Review.ReviewStatus status);

    // 영화별 리뷰 조회 (최신순, movieId로, 삭제되지 않은 것만)
    List<Review> findByMovieDetailIdAndStatusOrderByCreatedAtDesc(Long movieId, Review.ReviewStatus status);
} 

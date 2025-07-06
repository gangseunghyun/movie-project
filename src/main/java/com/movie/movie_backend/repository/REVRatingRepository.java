package com.movie.movie_backend.repository;

import com.movie.movie_backend.entity.Rating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface REVRatingRepository extends JpaRepository<Rating, Long> {
    // 평점 관련 쿼리 메소드 추가 가능
    
    // 특정 영화의 평점 조회
    List<Rating> findByMovieDetailMovieCd(String movieCd);
    
    // 특정 영화의 평점 개수 조회
    long countByMovieDetailMovieCd(String movieCd);
    
    // 특정 영화의 평균 평점 조회 (평점이 있는 경우만)
    // @Query("SELECT AVG(r.score) FROM Rating r WHERE r.movieDetail.movieCd = :movieCd")
    // Double getAverageRatingByMovieCd(@Param("movieCd") String movieCd);

    // 유저가 평점 남긴 영화의 MovieDetail ID 리스트 조회
    @Query("SELECT r.movieDetail.id FROM Rating r WHERE r.user.id = :userId")
    List<Long> findMovieIdsByUserId(@Param("userId") Long userId);
} 

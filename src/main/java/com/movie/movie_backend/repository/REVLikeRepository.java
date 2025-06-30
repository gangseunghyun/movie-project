package com.movie.movie_backend.repository;

import com.movie.movie_backend.entity.Like;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface REVLikeRepository extends JpaRepository<Like, Long> {
    // 좋아요 관련 쿼리 메소드 추가 가능
    boolean existsByMovieDetailAndUser(com.movie.movie_backend.entity.MovieDetail movieDetail, com.movie.movie_backend.entity.User user);
    int countByMovieDetail(com.movie.movie_backend.entity.MovieDetail movieDetail);
} 

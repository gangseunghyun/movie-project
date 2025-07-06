package com.movie.movie_backend.repository;

import com.movie.movie_backend.entity.Like;
import com.movie.movie_backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface REVLikeRepository extends JpaRepository<Like, Long> {
    // 좋아요 관련 쿼리 메소드 추가 가능
    boolean existsByMovieDetailAndUser(com.movie.movie_backend.entity.MovieDetail movieDetail, com.movie.movie_backend.entity.User user);
    int countByMovieDetail(com.movie.movie_backend.entity.MovieDetail movieDetail);
    
    // 사용자가 찜한 영화 목록 조회 (찜한 날짜 최신순)
    List<Like> findByUserOrderByCreatedAtDesc(User user);
    
    // 사용자 ID로 찜한 영화 목록 조회 (찜한 날짜 최신순)
    @Query("SELECT l FROM Like l WHERE l.user.id = :userId ORDER BY l.createdAt DESC")
    List<Like> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);
    
    // 사용자가 찜한 영화 개수 조회
    int countByUser(User user);
    
    // 유저가 찜한 영화의 MovieDetail ID 리스트 조회
    @Query("SELECT l.movieDetail.id FROM Like l WHERE l.user.id = :userId")
    List<Long> findMovieIdsByUserId(@Param("userId") Long userId);
} 

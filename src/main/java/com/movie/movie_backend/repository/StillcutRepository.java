package com.movie.movie_backend.repository;

import com.movie.movie_backend.entity.Stillcut;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface StillcutRepository extends JpaRepository<Stillcut, Long> {
    // 기본 CRUD 메서드만 사용
    long countByMovieDetailId(Long movieDetailId);
    List<Stillcut> findByMovieDetailId(Long movieDetailId);
    
    // 정렬된 스틸컷 조회
    List<Stillcut> findByMovieDetailIdOrderByOrderInMovieAsc(Long movieDetailId);
} 
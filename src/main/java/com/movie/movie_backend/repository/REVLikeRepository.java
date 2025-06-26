package com.movie.repository;

import com.movie.entity.Like;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface REVLikeRepository extends JpaRepository<Like, Long> {
    // 좋아요 관련 쿼리 메소드 추가 가능
} 
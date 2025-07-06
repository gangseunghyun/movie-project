package com.movie.movie_backend.repository;

import com.movie.movie_backend.entity.RecommendationLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecommendationLogRepository extends JpaRepository<RecommendationLog, Long> {
} 
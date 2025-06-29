package com.movie.movie_backend.repository;

import com.movie.movie_backend.entity.SearchLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SearchLogRepository extends JpaRepository<SearchLog, Long> {
} 
package com.movie.movie_backend.repository;

import com.movie.movie_backend.entity.PopularKeyword;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface PopularKeywordRepository extends JpaRepository<PopularKeyword, Long> {
    List<PopularKeyword> findTop10ByAggregatedAtOrderBySearchCountDesc(LocalDateTime aggregatedAt);
    List<PopularKeyword> findTop10ByOrderBySearchCountDesc();
} 
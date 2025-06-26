package com.movie.movie_backend.repository;

import com.movie.movie_backend.entity.SearchHistory;
import com.movie.movie_backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SearchHistoryRepository extends JpaRepository<SearchHistory, Long> {
    // 사용자별 최근 검색어 내림차순 조회 (예: 최근 10개)
    List<SearchHistory> findTop10ByUserOrderBySearchedAtDesc(User user);
} 
package com.movie.movie_backend.service;

import com.movie.movie_backend.entity.SearchHistory;
import com.movie.movie_backend.entity.User;
import com.movie.movie_backend.repository.SearchHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SearchHistoryService {

    private final SearchHistoryRepository searchHistoryRepository;

    // 검색어 저장
    public SearchHistory saveSearchHistory(User user, String keyword) {
        SearchHistory history = SearchHistory.builder()
                .user(user)
                .keyword(keyword)
                .searchedAt(LocalDateTime.now())
                .build();
        return searchHistoryRepository.save(history);
    }

    // 최근 검색어 조회 (최신 10개)
    public List<SearchHistory> getRecentSearches(User user) {
        return searchHistoryRepository.findTop10ByUserOrderBySearchedAtDesc(user);
    }
} 
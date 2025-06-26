package com.movie.movie_backend.controller;

import com.movie.movie_backend.entity.SearchHistory;
import com.movie.movie_backend.entity.User;
import com.movie.movie_backend.service.SearchHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/search-history")
@RequiredArgsConstructor
public class SearchHistoryController {

    private final SearchHistoryService searchHistoryService;

    // 검색어 저장
    @PostMapping
    public void saveSearch(@RequestParam String keyword, @AuthenticationPrincipal User user) {
        searchHistoryService.saveSearchHistory(user, keyword);
    }

    // 최근 검색어 조회
    @GetMapping
    public List<SearchHistory> getRecentSearches(@AuthenticationPrincipal User user) {
        return searchHistoryService.getRecentSearches(user);
    }
} 
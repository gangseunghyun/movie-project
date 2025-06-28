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
    public void saveSearch(@RequestParam String keyword, @AuthenticationPrincipal Object principal) {
        System.out.println("==== SearchHistoryController.saveSearch 메서드 호출됨 ====");
        System.out.println("==== keyword: " + keyword);
        System.out.println("==== principal class: " + (principal != null ? principal.getClass() : "null"));
        System.out.println("==== principal: " + principal);
        if (principal == null) {
            throw new RuntimeException("로그인한 사용자만 검색어를 저장할 수 있습니다.");
        }
        // 이후 principal에서 User 엔티티를 추출하는 로직을 설계
    }

    // 최근 검색어 조회
    @GetMapping
    public List<SearchHistory> getRecentSearches(@AuthenticationPrincipal Object principal) {
        System.out.println("==== principal class: " + (principal != null ? principal.getClass() : "null"));
        System.out.println("==== principal: " + principal);
        if (principal == null) {
            throw new RuntimeException("로그인한 사용자만 최근 검색어를 조회할 수 있습니다.");
        }
        // principal에서 email 등으로 User 엔티티를 조회해서 반환하는 로직 추가 예정
        return null; // 임시로 null 반환
    }
} 
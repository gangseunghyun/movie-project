package com.movie.movie_backend.controller;

import com.movie.movie_backend.entity.SearchHistory;
import com.movie.movie_backend.entity.User;
import com.movie.movie_backend.service.SearchHistoryService;
import com.movie.movie_backend.repository.USRUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.web.bind.annotation.*;
import com.movie.movie_backend.dto.SearchHistoryDto;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/search-history")
@RequiredArgsConstructor
public class SearchHistoryController {

    private final SearchHistoryService searchHistoryService;
    private final USRUserRepository userRepository;

    // 검색어 저장
    @PostMapping
    public void saveSearch(@RequestParam String keyword, @RequestParam(defaultValue = "0") int searchResultCount, @AuthenticationPrincipal Object principal) {
        log.info("검색어 저장 요청: keyword={}, searchResultCount={}", keyword, searchResultCount);
        System.out.println("==== SearchHistoryController.saveSearch 메서드 호출됨 ====");
        System.out.println("==== keyword: " + keyword);
        System.out.println("==== searchResultCount: " + searchResultCount);
        System.out.println("==== principal class: " + (principal != null ? principal.getClass() : "null"));
        System.out.println("==== principal: " + principal);

        String email = null;
        if (principal instanceof DefaultOAuth2User) {
            DefaultOAuth2User oAuth2User = (DefaultOAuth2User) principal;
            System.out.println("==== attributes: " + oAuth2User.getAttributes());
            email = (String) oAuth2User.getAttribute("email");
        } else if (principal instanceof User) {
            User userPrincipal = (User) principal;
            email = userPrincipal.getEmail();
            System.out.println("==== User principal email: " + email);
        } else {
            System.out.println("==== principal은 DefaultOAuth2User도 User도 아님: " + (principal != null ? principal.getClass() : "null"));
        }
        System.out.println("==== email: " + email);

        if (email == null) throw new RuntimeException("이메일 정보를 찾을 수 없습니다.");
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("사용자 정보가 없습니다."));
        System.out.println("==== user: " + user);

        searchHistoryService.saveSearchHistory(user, keyword, searchResultCount);
        log.info("검색어 저장 완료: user={}, keyword={}, searchResultCount={}", user.getEmail(), keyword, searchResultCount);
    }

    // 최근 검색어 조회
    @GetMapping
    public List<SearchHistoryDto> getRecentSearches(@AuthenticationPrincipal Object principal) {
        log.info("최근 검색어 조회 요청");
        System.out.println("==== principal class: " + (principal != null ? principal.getClass() : "null"));
        System.out.println("==== principal: " + principal);

        String email = null;
        if (principal instanceof DefaultOAuth2User) {
            DefaultOAuth2User oAuth2User = (DefaultOAuth2User) principal;
            System.out.println("==== attributes: " + oAuth2User.getAttributes());
            email = (String) oAuth2User.getAttribute("email");
        } else if (principal instanceof User) {
            User userPrincipal = (User) principal;
            email = userPrincipal.getEmail();
            System.out.println("==== User principal email: " + email);
        } else {
            System.out.println("==== principal은 DefaultOAuth2User도 User도 아님: " + (principal != null ? principal.getClass() : "null"));
        }
        System.out.println("==== email: " + email);

        if (email == null) throw new RuntimeException("이메일 정보를 찾을 수 없습니다.");
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("사용자 정보가 없습니다."));
        System.out.println("==== user: " + user);

        List<SearchHistoryDto> recentSearches = searchHistoryService.getRecentSearches(user).stream()
                .map(h -> new SearchHistoryDto(h.getId(), h.getKeyword(), h.getSearchedAt()))
                .toList();
        
        log.info("최근 검색어 조회 결과: user={}, count={}", user.getEmail(), recentSearches.size());
        return recentSearches;
    }

    @DeleteMapping
    public void deleteSearchHistory(@RequestParam String keyword, @AuthenticationPrincipal Object principal) {
        log.info("검색어 삭제 요청: keyword={}", keyword);
        System.out.println("==== SearchHistoryController.deleteSearchHistory 메서드 호출됨 ====");
        System.out.println("==== keyword: " + keyword);
        System.out.println("==== principal class: " + (principal != null ? principal.getClass() : "null"));
        System.out.println("==== principal: " + principal);
        
        String email = null;
        if (principal instanceof DefaultOAuth2User) {
            DefaultOAuth2User oAuth2User = (DefaultOAuth2User) principal;
            email = (String) oAuth2User.getAttribute("email");
            System.out.println("==== OAuth2User email: " + email);
        } else if (principal instanceof User) {
            User userPrincipal = (User) principal;
            email = userPrincipal.getEmail();
            System.out.println("==== User principal email: " + email);
        } else {
            System.out.println("==== principal은 DefaultOAuth2User도 User도 아님: " + (principal != null ? principal.getClass() : "null"));
        }
        System.out.println("==== email: " + email);
        
        if (email == null) throw new RuntimeException("이메일 정보를 찾을 수 없습니다.");
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("사용자 정보가 없습니다."));
        System.out.println("==== user: " + user);
        
        searchHistoryService.deleteByUserAndKeyword(user, keyword);
        System.out.println("==== deleteSearchHistory 완료 ====");
        log.info("검색어 삭제 완료: user={}, keyword={}", user.getEmail(), keyword);
    }
} 
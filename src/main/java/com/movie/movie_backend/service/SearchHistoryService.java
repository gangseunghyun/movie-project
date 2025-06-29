package com.movie.movie_backend.service;

import com.movie.movie_backend.entity.SearchHistory;
import com.movie.movie_backend.entity.SearchLog;
import com.movie.movie_backend.entity.User;
import com.movie.movie_backend.repository.SearchHistoryRepository;
import com.movie.movie_backend.repository.SearchLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class SearchHistoryService {

    private final SearchHistoryRepository searchHistoryRepository;
    private final SearchLogRepository searchLogRepository;

    // 검색어 저장 (중복 비허용, 10개 제한) + search_log에도 저장
    public SearchHistory saveSearchHistory(User user, String keyword) {
        System.out.println("==== saveSearchHistory 호출됨 ====");
        System.out.println("user: " + user);
        System.out.println("keyword: " + keyword);
        
        // 1. search_log에 저장 (인기검색어 집계용) - 로그인/비로그인 모두 저장
        try {
            SearchLog searchLog = new SearchLog();
            searchLog.setKeyword(keyword);
            searchLog.setSearchedAt(LocalDateTime.now());
            searchLog.setUserId(user != null ? user.getId() : null);
            searchLogRepository.save(searchLog);
            System.out.println("==== search_log 저장 완료 ====");
        } catch (Exception e) {
            System.out.println("==== search_log 저장 실패 ====");
            e.printStackTrace();
            // search_log 저장 실패해도 최근검색어는 계속 진행
        }
        
        // 2. 최근검색어 저장 (로그인한 사용자만)
        if (user != null) {
            // 2-1. 기존에 같은 검색어가 있으면 삭제
            List<SearchHistory> duplicates = searchHistoryRepository.findByUserAndKeyword(user, keyword);
            if (!duplicates.isEmpty()) {
                searchHistoryRepository.deleteAll(duplicates);
            }
            // 2-2. 새로 저장
            SearchHistory history = SearchHistory.builder()
                    .user(user)
                    .keyword(keyword)
                    .searchedAt(LocalDateTime.now())
                    .build();
            try {
                searchHistoryRepository.save(history);
                System.out.println("==== searchHistoryRepository.save 이후 ====");
            } catch (Exception e) {
                System.out.println("==== searchHistoryRepository.save 예외 발생 ====");
                e.printStackTrace();
            }
            // 2-3. 10개 초과 시 오래된 것 삭제
            List<SearchHistory> all = searchHistoryRepository.findTop10ByUserOrderBySearchedAtDesc(user);
            List<SearchHistory> allByUser = searchHistoryRepository.findAllByUserOrderBySearchedAtDesc(user);
            if (allByUser.size() > 10) {
                List<SearchHistory> toDelete = allByUser.subList(10, allByUser.size());
                searchHistoryRepository.deleteAll(toDelete);
            }
            return history;
        } else {
            System.out.println("==== 비로그인 사용자이므로 최근검색어 저장하지 않음 ====");
            return null;
        }
    }

    // 최근 검색어 조회 (최신 10개)
    @Transactional(readOnly = true)
    public List<SearchHistory> getRecentSearches(User user) {
        return searchHistoryRepository.findTop10ByUserOrderBySearchedAtDesc(user);
    }

    public void deleteByUserAndKeyword(User user, String keyword) {
        System.out.println("==== deleteByUserAndKeyword 호출됨 ====");
        System.out.println("user: " + user);
        System.out.println("keyword: " + keyword);
        
        // 삭제 전 검색어 존재 여부 확인
        List<SearchHistory> existing = searchHistoryRepository.findByUserAndKeyword(user, keyword);
        System.out.println("삭제 전 검색어 개수: " + existing.size());
        
        // 직접 쿼리로 삭제 (인증 우회)
        searchHistoryRepository.deleteByUserIdAndKeyword(user.getId(), keyword);
        
        // 삭제 후 검색어 존재 여부 확인
        List<SearchHistory> afterDelete = searchHistoryRepository.findByUserAndKeyword(user, keyword);
        System.out.println("삭제 후 검색어 개수: " + afterDelete.size());
        System.out.println("==== deleteByUserAndKeyword 완료 ====");
    }
} 
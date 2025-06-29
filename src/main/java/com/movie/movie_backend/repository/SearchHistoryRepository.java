package com.movie.movie_backend.repository;

import com.movie.movie_backend.entity.SearchHistory;
import com.movie.movie_backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface SearchHistoryRepository extends JpaRepository<SearchHistory, Long> {
    // 사용자별 최근 검색어 내림차순 조회 (예: 최근 10개)
    List<SearchHistory> findTop10ByUserOrderBySearchedAtDesc(User user);

    // 사용자별 특정 키워드 검색어 조회 (중복 체크용)
    List<SearchHistory> findByUserAndKeyword(User user, String keyword);

    // 사용자별 전체 검색어 내림차순 조회 (10개 초과 삭제용)
    List<SearchHistory> findAllByUserOrderBySearchedAtDesc(User user);

    // 사용자+키워드로 검색어 삭제
    @Modifying
    @Query("delete from SearchHistory sh where sh.user = :user and sh.keyword = :keyword")
    void deleteByUserAndKeyword(@Param("user") User user, @Param("keyword") String keyword);
    
    // 직접 쿼리로 삭제 (인증 우회용)
    @Modifying
    @Query(value = "DELETE FROM search_history WHERE user_id = :userId AND keyword = :keyword", nativeQuery = true)
    void deleteByUserIdAndKeyword(@Param("userId") Long userId, @Param("keyword") String keyword);
} 
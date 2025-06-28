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
    
    // 인기 검색어 조회 (검색 결과가 있는 검색어만)
    @Query(value = """
        SELECT sh.keyword, COUNT(*) as search_count
        FROM search_history sh
        WHERE sh.searched_at >= DATE_SUB(NOW(), INTERVAL 7 DAY)
          AND EXISTS (
            SELECT 1
            FROM movie_detail md
            LEFT JOIN director d ON md.director_id = d.id
            LEFT JOIN cast c ON c.movie_detail_id = md.movieCd
            LEFT JOIN actor a ON c.actor_id = a.id
            WHERE
              md.movieNm LIKE CONCAT('%', sh.keyword, '%')
              OR md.movieNmEn LIKE CONCAT('%', sh.keyword, '%')
              OR md.genreNm LIKE CONCAT('%', sh.keyword, '%')
              OR md.description LIKE CONCAT('%', sh.keyword, '%')
              OR d.name LIKE CONCAT('%', sh.keyword, '%')
              OR a.name LIKE CONCAT('%', sh.keyword, '%')
          )
        GROUP BY sh.keyword
        ORDER BY search_count DESC
        LIMIT 10
        """, nativeQuery = true)
    List<Object[]> findPopularKeywords();
} 
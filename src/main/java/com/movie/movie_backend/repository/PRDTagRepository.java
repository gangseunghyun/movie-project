package com.movie.movie_backend.repository;

import com.movie.movie_backend.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PRDTagRepository extends JpaRepository<Tag, Long> {
    
    // 태그명으로 태그 조회
    Optional<Tag> findByName(String name);
    
    // 태그명으로 존재 여부 확인
    boolean existsByName(String name);
    
    // 태그명으로 검색 (부분 일치)
    List<Tag> findByNameContainingIgnoreCase(String name);
    
    // 특정 영화에 달린 태그들 조회
    @Query("SELECT t FROM Tag t JOIN t.movieDetails md WHERE md.movieCd = :movieCd")
    List<Tag> findTagsByMovieCd(@Param("movieCd") String movieCd);
    
    // 가장 많이 사용된 태그들 조회 (인기 태그)
    @Query("SELECT t, COUNT(md) as usageCount FROM Tag t JOIN t.movieDetails md GROUP BY t ORDER BY usageCount DESC")
    List<Object[]> findPopularTags();
    
    // 실제 사용되는 장르 태그만 조회 (동적)
    @Query("SELECT DISTINCT t FROM Tag t JOIN t.movieDetails md WHERE md IS NOT NULL ORDER BY t.name")
    List<Tag> findGenreTags();
} 

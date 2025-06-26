package com.movie.movie_backend.repository;

import com.movie.movie_backend.entity.BoxOffice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface BoxOfficeRepository extends JpaRepository<BoxOffice, Long> {
    
    // 특정 날짜의 박스오피스 TOP-10 조회
    List<BoxOffice> findByTargetDateAndRankTypeOrderByRankAsc(LocalDate targetDate, String rankType);
    
    // 최신 박스오피스 TOP-10 조회 (JOIN FETCH로 movieDetail 함께 가져오기)
    @Query("SELECT b FROM BoxOffice b LEFT JOIN FETCH b.movieDetail md WHERE b.targetDate = (SELECT MAX(b2.targetDate) FROM BoxOffice b2 WHERE b2.rankType = :rankType) AND b.rankType = :rankType ORDER BY b.rank ASC")
    List<BoxOffice> findLatestBoxOfficeTop10(@Param("rankType") String rankType);
    
    // 특정 날짜의 박스오피스 TOP-10 조회 (JOIN FETCH로 movieDetail 함께 가져오기)
    @Query("SELECT b FROM BoxOffice b LEFT JOIN FETCH b.movieDetail md WHERE b.targetDate = :targetDate AND b.rankType = :rankType ORDER BY b.rank ASC")
    List<BoxOffice> findByTargetDateAndRankTypeOrderByRankAscWithMovieDetail(@Param("targetDate") LocalDate targetDate, @Param("rankType") String rankType);
    
    // 일일 박스오피스 TOP-10
    List<BoxOffice> findTop10ByRankTypeOrderByRankAsc(String rankType);
    
    // 특정 영화의 박스오피스 기록 조회
    List<BoxOffice> findByMovieCdOrderByTargetDateDesc(String movieCd);
    
    // 특정 날짜 범위의 박스오피스 조회
    List<BoxOffice> findByTargetDateBetweenAndRankTypeOrderByTargetDateDescRankAsc(LocalDate startDate, LocalDate endDate, String rankType);
    
    // 특정 날짜와 타입으로 기존 데이터 삭제
    void deleteByTargetDateAndRankType(LocalDate targetDate, String rankType);
} 

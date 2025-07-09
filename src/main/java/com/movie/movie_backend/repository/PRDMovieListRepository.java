package com.movie.movie_backend.repository;

import com.movie.movie_backend.entity.MovieList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PRDMovieListRepository extends JpaRepository<MovieList, String> {
    
    /**
     * 영화 코드로 영화 찾기
     */
    Optional<MovieList> findByMovieCd(String movieCd);
    
    /**
     * 영화 코드로 존재 여부 확인
     */
    boolean existsByMovieCd(String movieCd);
    
    /**
     * 영화명으로 검색 (부분 일치)
     */
    List<MovieList> findByMovieNmContainingIgnoreCase(String movieNm);
    
    /**
     * 영화명으로 검색 (단어별 검색)
     */
    @Query("SELECT m FROM MovieList m WHERE LOWER(m.movieNm) LIKE LOWER(CONCAT('%', :word, '%'))")
    List<MovieList> findByMovieNmContainingWord(@Param("word") String word);
    
    /**
     * 영화명으로 검색 (띄어쓰기 무시)
     */
    @Query("SELECT m FROM MovieList m WHERE REPLACE(LOWER(m.movieNm), ' ', '') LIKE LOWER(CONCAT('%', REPLACE(:keyword, ' ', ''), '%'))")
    List<MovieList> findByMovieNmIgnoreSpace(@Param("keyword") String keyword);
    
    /**
     * 영화명으로 검색 (유사도 기반)
     */
    @Query("SELECT m FROM MovieList m WHERE " +
           "LOWER(m.movieNm) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(m.movieNmEn) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "REPLACE(LOWER(m.movieNm), ' ', '') LIKE LOWER(CONCAT('%', REPLACE(:keyword, ' ', ''), '%'))")
    List<MovieList> findByMovieNmSimilar(@Param("keyword") String keyword);
    
    /**
     * 영화 상태별 조회
     */
    List<MovieList> findByStatus(com.movie.movie_backend.constant.MovieStatus status);
    
    /**
     * 영화 상태별 조회 (개봉일 오름차순)
     */
    List<MovieList> findByStatusOrderByOpenDtAsc(String status);
    
    /**
     * 영화 상태별 조회 (개봉일 내림차순)
     */
    List<MovieList> findByStatusOrderByOpenDtDesc(String status);
    
    /**
     * 장르별 조회
     */
    List<MovieList> findByGenreNmContaining(String genreNm);
    
    /**
     * 국가별 조회
     */
    List<MovieList> findByNationNmContaining(String nationNm);
    
    /**
     * 개봉일 기준으로 최신 영화 조회 (최신순)
     */
    @Query("SELECT m FROM MovieList m WHERE m.openDt IS NOT NULL ORDER BY m.openDt DESC")
    List<MovieList> findLatestMovies();
    
    /**
     * 개봉일 기준으로 특정 기간 영화 조회
     */
    @Query("SELECT m FROM MovieList m WHERE m.openDt BETWEEN :startDate AND :endDate ORDER BY m.openDt DESC")
    List<MovieList> findByOpenDtBetween(@Param("startDate") String startDate, @Param("endDate") String endDate);
    
    /**
     * 영화 코드 목록으로 조회
     */
    List<MovieList> findByMovieCdIn(List<String> movieCds);
    
    /**
     * kmdbId가 있는 영화 목록 조회
     */
    List<MovieList> findByKmdbIdIsNotNull();
} 

package com.movie.movie_backend.repository;

import com.movie.movie_backend.entity.MovieDetail;
import com.movie.movie_backend.entity.Tag;
import com.movie.movie_backend.constant.MovieStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PRDMovieRepository extends JpaRepository<MovieDetail, Long> {
    Optional<MovieDetail> findByMovieCd(String movieCd);
    boolean existsByMovieCd(String movieCd);
    Optional<MovieDetail> findByMovieNmContaining(String movieNm);
    List<MovieDetail> findByStatus(MovieStatus status);
    List<MovieDetail> findByMovieNmContainingIgnoreCase(String movieNm);
    
    // 장르별 조회
    List<MovieDetail> findByGenreNmContaining(String genreNm);
    
    // 개봉일순 정렬 (최신순 - 내림차순)
    List<MovieDetail> findAllByOrderByOpenDtDesc();
    
    // 이름순 정렬 (오름차순)
    List<MovieDetail> findAllByOrderByMovieNmAsc();
    
    // 이름순 정렬 (내림차순)
    List<MovieDetail> findAllByOrderByMovieNmDesc();
    
    // 장르 중복 확인을 위한 쿼리들
    @Query("SELECT genreNm, COUNT(*) as count FROM MovieDetail WHERE genreNm IS NOT NULL GROUP BY genreNm HAVING COUNT(*) > 1 ORDER BY count DESC")
    List<Object[]> findDuplicateGenres();
    
    @Query("SELECT m FROM MovieDetail m WHERE m.genreNm = :genreNm")
    List<MovieDetail> findByExactGenreNm(@Param("genreNm") String genreNm);
    
    // 띄어쓰기 무시 통합 검색 (제목, 감독, 배우, 장르)
    @Query(value = "SELECT DISTINCT m.* FROM movie_detail m " +
            "LEFT JOIN director d ON m.director_id = d.id " +
            "LEFT JOIN casts c ON c.movie_detail_id = m.movie_detail_id " +
            "LEFT JOIN actor a ON c.actor_id = a.id " +
            "WHERE REPLACE(m.movie_nm, ' ', '') LIKE CONCAT('%', :keyword, '%') " +
            "OR REPLACE(m.genre_nm, ' ', '') LIKE CONCAT('%', :keyword, '%') " +
            "OR REPLACE(d.name, ' ', '') LIKE CONCAT('%', :keyword, '%') " +
            "OR REPLACE(a.name, ' ', '') LIKE CONCAT('%', :keyword, '%')",
            nativeQuery = true)
    List<MovieDetail> searchIgnoreSpace(@Param("keyword") String keyword);
    
    // 상품 관련 쿼리 메소드 추가 가능
    
    // 관객수 기준 상위 20개 영화 조회
    List<MovieDetail> findTop20ByOrderByTotalAudienceDesc();
    
    // 태그를 가진 영화들 조회
    @Query("SELECT DISTINCT m FROM MovieDetail m JOIN m.tags t WHERE t IN :tags ORDER BY m.totalAudience DESC")
    List<MovieDetail> findMoviesByTags(@Param("tags") List<Tag> tags);

    // 추천 영화 리스트를 fetch join으로 tags까지 한 번에 가져오기
    @Query("SELECT DISTINCT m FROM MovieDetail m LEFT JOIN FETCH m.tags WHERE m IN :movies")
    List<MovieDetail> fetchMoviesWithTags(@Param("movies") List<MovieDetail> movies);
} 

package com.movie.movie_backend.controller;

import com.movie.movie_backend.dto.MovieDetailDto;
import com.movie.movie_backend.entity.MovieDetail;
import com.movie.movie_backend.mapper.MovieDetailMapper;
import com.movie.movie_backend.service.TmdbRatingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Slf4j
@RestController
@RequestMapping("/api/ratings")
@RequiredArgsConstructor
public class RatingController {

    private final TmdbRatingService tmdbRatingService;
    private final MovieDetailMapper movieDetailMapper;

    /**
     * 평균 별점이 높은 영화 TOP-10 조회
     */
    @GetMapping("/top-rated")
    public ResponseEntity<Map<String, Object>> getTopRatedMovies(@RequestParam(defaultValue = "10") int limit) {
        try {
            log.info("평균 별점이 높은 영화 TOP-{} 조회", limit);
            
            List<MovieDetail> topRatedMovies = tmdbRatingService.getTopRatedMovies(limit);
            List<MovieDetailDto> movieDtos = topRatedMovies.stream()
                    .map(movieDetailMapper::toDto)
                    .toList();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", movieDtos);
            response.put("count", movieDtos.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("평균 별점이 높은 영화 조회 실패", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "평균 별점이 높은 영화 조회에 실패했습니다: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 특정 영화의 평균 별점 조회
     */
    @GetMapping("/movie/{movieCd}/average")
    public ResponseEntity<Map<String, Object>> getMovieAverageRating(@PathVariable String movieCd) {
        try {
            log.info("영화 평균 별점 조회: movie={}", movieCd);
            
            Double averageRating = tmdbRatingService.getAverageRating(movieCd);
            Integer ratingCount = tmdbRatingService.getRatingCount(movieCd);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("movieCd", movieCd);
            response.put("averageRating", averageRating);
            response.put("ratingCount", ratingCount);
            response.put("message", averageRating != null ? "평균 별점을 조회했습니다." : "별점이 없습니다.");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("영화 평균 별점 조회 실패", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "평균 별점 조회에 실패했습니다: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * TMDB 평점 가져오기 실행
     */
    @PostMapping("/fetch-tmdb")
    public ResponseEntity<Map<String, Object>> fetchTmdbRatings() {
        try {
            log.info("TMDB 평점 가져오기 시작");
            
            // 비동기로 실행 (실제로는 @Async 사용 권장)
            tmdbRatingService.fetchAndSaveTmdbRatings();
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "TMDB 평점 가져오기가 시작되었습니다."
            ));
        } catch (Exception e) {
            log.error("TMDB 평점 가져오기 실패", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "TMDB 평점 가져오기에 실패했습니다: " + e.getMessage()
            ));
        }
    }
} 

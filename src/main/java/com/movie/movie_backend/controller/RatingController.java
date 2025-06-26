package com.movie.controller;

import com.movie.dto.MovieDetailDto;
import com.movie.entity.MovieDetail;
import com.movie.mapper.MovieDetailMapper;
import com.movie.service.TmdbRatingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", movieDtos,
                "count", movieDtos.size()
            ));
        } catch (Exception e) {
            log.error("평균 별점이 높은 영화 조회 실패", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "평균 별점이 높은 영화 조회에 실패했습니다: " + e.getMessage()
            ));
        }
    }

    /**
     * 특정 영화의 평균 별점 조회
     */
    @GetMapping("/{movieCd}/average")
    public ResponseEntity<Map<String, Object>> getAverageRating(@PathVariable String movieCd) {
        try {
            log.info("영화 {}의 평균 별점 조회", movieCd);
            
            Double averageRating = tmdbRatingService.getAverageRating(movieCd);
            
            if (averageRating != null) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "movieCd", movieCd,
                    "averageRating", averageRating
                ));
            } else {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "movieCd", movieCd,
                    "averageRating", null,
                    "message", "평점 정보가 없습니다."
                ));
            }
        } catch (Exception e) {
            log.error("영화 {}의 평균 별점 조회 실패", movieCd, e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "평균 별점 조회에 실패했습니다: " + e.getMessage()
            ));
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
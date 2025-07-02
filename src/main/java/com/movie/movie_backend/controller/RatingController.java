package com.movie.movie_backend.controller;

import com.movie.movie_backend.dto.MovieDetailDto;
import com.movie.movie_backend.entity.MovieDetail;
import com.movie.movie_backend.entity.User;
import com.movie.movie_backend.mapper.MovieDetailMapper;
import com.movie.movie_backend.repository.REVLikeRepository;
import com.movie.movie_backend.service.TmdbRatingService;
import com.movie.movie_backend.dto.RatingDto;
import com.movie.movie_backend.dto.RatingRequestDto;
import com.movie.movie_backend.service.REVRatingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
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
    private final REVLikeRepository likeRepository;
    private final REVRatingService ratingService;

    // 현재 로그인한 사용자 반환 (없으면 null)
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            return null;
        }
        if (authentication.getPrincipal() instanceof User) {
            return (User) authentication.getPrincipal();
        }
        return null;
    }

    // 별점 등록/수정
    @PostMapping
    public ResponseEntity<Map<String, Object>> saveRating(
            @Valid @RequestBody RatingRequestDto requestDto,
            @AuthenticationPrincipal Object principal) {
        try {
            String userEmail = extractUserEmail(principal);
            if (userEmail == null) {
                return fail("로그인이 필요합니다.");
            }
            RatingDto rating = ratingService.saveRating(
                userEmail,
                requestDto.getMovieCd(),
                requestDto.getScore()
            );
            return ok(rating, "별점이 저장되었습니다.");
        } catch (Exception e) {
            log.error("별점 저장 실패", e);
            return fail("별점 저장에 실패했습니다: " + e.getMessage());
        }
    }

    // 별점 삭제
    @DeleteMapping("/{movieCd}")
    public ResponseEntity<Map<String, Object>> deleteRating(
            @PathVariable String movieCd,
            @AuthenticationPrincipal Object principal) {
        try {
            String userEmail = extractUserEmail(principal);
            if (userEmail == null) {
                return fail("로그인이 필요합니다.");
            }
            ratingService.deleteRating(userEmail, movieCd);
            return ok(null, "별점이 삭제되었습니다.");
        } catch (Exception e) {
            log.error("별점 삭제 실패", e);
            return fail("별점 삭제에 실패했습니다: " + e.getMessage());
        }
    }

    // 내가 남긴 별점 조회
    @GetMapping("/{movieCd}")
    public ResponseEntity<Map<String, Object>> getUserRating(
            @PathVariable String movieCd,
            @AuthenticationPrincipal Object principal) {
        try {
            String userEmail = extractUserEmail(principal);
            if (userEmail == null) {
                return ok(null, "로그인이 필요합니다.");
            }
            RatingDto rating = ratingService.getUserRating(userEmail, movieCd);
            return ok(rating, rating != null ? "별점을 찾았습니다." : "별점이 없습니다.");
        } catch (Exception e) {
            log.error("사용자 별점 조회 실패", e);
            return ok(null, "별점 조회에 실패했습니다: " + e.getMessage());
        }
    }

    // 영화 평균 별점/참여자수
    @GetMapping("/movie/{movieCd}/average")
    public ResponseEntity<Map<String, Object>> getMovieAverageRating(@PathVariable String movieCd) {
        try {
            Double averageRating = ratingService.getAverageRating(movieCd);
            long ratingCount = ratingService.getRatingCountByMovieDetail(movieCd);
            Map<String, Object> data = new HashMap<>();
            data.put("averageRating", averageRating);
            data.put("ratingCount", ratingCount);
            return ok(data, averageRating != null ? "평균 별점을 조회했습니다." : "별점이 없습니다.");
        } catch (Exception e) {
            log.error("영화 평균 별점 조회 실패", e);
            Map<String, Object> data = new HashMap<>();
            data.put("averageRating", null);
            data.put("ratingCount", 0L);
            return ok(data, "평균 별점 조회에 실패했습니다: " + e.getMessage());
        }
    }

    // 영화 별점 분포
    @GetMapping("/movie/{movieCd}/distribution")
    public ResponseEntity<Map<String, Object>> getRatingDistribution(@PathVariable String movieCd) {
        try {
            Map<Double, Long> distribution = ratingService.getRatingDistribution(movieCd);
            Map<String, Object> data = new HashMap<>();
            data.put("distribution", distribution);
            return ok(data, "별점 분포를 조회했습니다.");
        } catch (Exception e) {
            log.error("별점 분포 조회 실패", e);
            Map<String, Object> data = new HashMap<>();
            data.put("distribution", null);
            return ok(data, "별점 분포 조회에 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * 평균 별점이 높은 영화 TOP-10 조회
     */
    @GetMapping("/top-rated")
    public ResponseEntity<Map<String, Object>> getTopRatedMovies(@RequestParam(defaultValue = "10") int limit) {
        try {
            log.info("평균 별점이 높은 영화 TOP-{} 조회", limit);
            
            List<MovieDetail> topRatedMovies = tmdbRatingService.getTopRatedMovies(limit);
            User currentUser = getCurrentUser();
            List<MovieDetailDto> movieDtos = topRatedMovies.stream()
                    .map(md -> movieDetailMapper.toDto(
                        md,
                        likeRepository.countByMovieDetail(md),
                        currentUser != null && likeRepository.existsByMovieDetailAndUser(md, currentUser)
                    ))
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

    // 유틸: 응답 포맷 통일
    private ResponseEntity<Map<String, Object>> ok(Object data, String message) {
        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("data", data);
        res.put("message", message);
        return ResponseEntity.ok(res);
    }
    private ResponseEntity<Map<String, Object>> fail(String message) {
        Map<String, Object> res = new HashMap<>();
        res.put("success", false);
        res.put("data", null);
        res.put("message", message);
        return ResponseEntity.ok(res);
    }
    private String extractUserEmail(Object principal) {
        if (principal instanceof DefaultOAuth2User) {
            DefaultOAuth2User oAuth2User = (DefaultOAuth2User) principal;
            return (String) oAuth2User.getAttribute("email");
        }
        if (principal instanceof com.movie.movie_backend.entity.User) {
            return ((com.movie.movie_backend.entity.User) principal).getEmail();
        }
        return null;
    }
} 

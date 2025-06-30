package com.movie.movie_backend.controller;

import com.movie.movie_backend.dto.RatingDto;
import com.movie.movie_backend.dto.RatingRequestDto;
import com.movie.movie_backend.service.REVRatingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Slf4j
@RestController
@RequestMapping("/api/user-ratings")
@RequiredArgsConstructor
public class UserRatingController {

    private final REVRatingService ratingService;

    /**
     * 사용자가 영화에 별점 등록/수정
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> saveRating(
            @Valid @RequestBody RatingRequestDto requestDto,
            @AuthenticationPrincipal Object principal) {
        
        try {
            log.info("별점 저장 요청 시작: requestDto={}, principal={}", requestDto, principal);
            
            String userEmail = extractUserEmail(principal);
            log.info("추출된 사용자 이메일: {}", userEmail);
            
            if (userEmail == null) {
                log.warn("사용자 이메일을 추출할 수 없습니다. principal={}", principal);
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "로그인이 필요합니다.");
                return ResponseEntity.badRequest().body(response);
            }

            log.info("별점 저장 요청: user={}, movie={}, score={}", 
                    userEmail, requestDto.getMovieCd(), requestDto.getScore());

            RatingDto rating = ratingService.saveRating(
                userEmail, 
                requestDto.getMovieCd(), 
                requestDto.getScore()
            );

            log.info("별점 저장 성공: {}", rating);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", rating);
            response.put("message", "별점이 저장되었습니다.");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("별점 저장 실패", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "별점 저장에 실패했습니다: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 사용자의 별점 삭제
     */
    @DeleteMapping("/{movieCd}")
    public ResponseEntity<Map<String, Object>> deleteRating(
            @PathVariable String movieCd,
            @AuthenticationPrincipal Object principal) {
        
        try {
            String userEmail = extractUserEmail(principal);
            if (userEmail == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "로그인이 필요합니다.");
                return ResponseEntity.badRequest().body(response);
            }

            log.info("별점 삭제 요청: user={}, movie={}", userEmail, movieCd);

            ratingService.deleteRating(userEmail, movieCd);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "별점이 삭제되었습니다.");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("별점 삭제 실패", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "별점 삭제에 실패했습니다: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 사용자가 특정 영화에 준 별점 조회
     */
    @GetMapping("/{movieCd}")
    public ResponseEntity<Map<String, Object>> getUserRating(
            @PathVariable String movieCd,
            @AuthenticationPrincipal Object principal) {
        try {
            String userEmail = extractUserEmail(principal);
            if (userEmail == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "로그인이 필요합니다.");
                return ResponseEntity.badRequest().body(response);
            }

            log.info("사용자 별점 조회: user={}, movie={}", userEmail, movieCd);

            RatingDto rating = ratingService.getUserRating(userEmail, movieCd);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", rating);
            result.put("message", rating != null ? "별점을 찾았습니다." : "별점이 없습니다.");
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("사용자 별점 조회 실패", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "별점 조회에 실패했습니다: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 사용자의 모든 별점 조회
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getUserRatings(
            @AuthenticationPrincipal Object principal) {
        
        try {
            String userEmail = extractUserEmail(principal);
            if (userEmail == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "로그인이 필요합니다.");
                return ResponseEntity.badRequest().body(response);
            }

            log.info("사용자 모든 별점 조회: user={}", userEmail);

            List<RatingDto> ratings = ratingService.getUserRatings(userEmail);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", ratings);
            response.put("count", ratings.size());
            response.put("message", "별점 목록을 조회했습니다.");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("사용자 별점 목록 조회 실패", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "별점 목록 조회에 실패했습니다: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 영화의 평균 별점 조회
     */
    @GetMapping("/movie/{movieCd}/average")
    public ResponseEntity<Map<String, Object>> getMovieAverageRating(@PathVariable String movieCd) {
        try {
            log.info("영화 평균 별점 조회: movie={}", movieCd);

            Double averageRating = ratingService.getAverageRating(movieCd);
            long ratingCount = ratingService.getRatingCountByMovieDetail(movieCd);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("movieCd", movieCd);
            result.put("averageRating", averageRating);
            result.put("ratingCount", ratingCount);
            result.put("message", averageRating != null ? "평균 별점을 조회했습니다." : "별점이 없습니다.");

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("영화 평균 별점 조회 실패", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "평균 별점 조회에 실패했습니다: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 영화의 별점 분포 조회 (0.5~5.0, 0.5 단위, Rating 테이블 기준)
     */
    @GetMapping("/movie/{movieCd}/distribution")
    public ResponseEntity<Map<String, Object>> getRatingDistribution(@PathVariable String movieCd) {
        try {
            Map<Double, Long> distribution = ratingService.getRatingDistribution(movieCd);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("movieCd", movieCd);
            result.put("distribution", distribution);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("별점 분포 조회 실패", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "별점 분포 조회에 실패했습니다: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 사용자 이메일 추출
     */
    private String extractUserEmail(Object principal) {
        if (principal instanceof DefaultOAuth2User) {
            DefaultOAuth2User oAuth2User = (DefaultOAuth2User) principal;
            return (String) oAuth2User.getAttribute("email");
        }
        // 일반 로그인(세션 기반) 사용자 처리
        if (principal instanceof com.movie.movie_backend.entity.User) {
            return ((com.movie.movie_backend.entity.User) principal).getEmail();
        }
        return null;
    }
} 
package com.movie.movie_backend.controller;

import com.movie.movie_backend.entity.Review;
import com.movie.movie_backend.service.REVReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final REVReviewService reviewService;

    /**
     * 리뷰 작성 (댓글만, 평점만, 둘 다 가능)
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createReview(@RequestBody Map<String, Object> request) {
        try {
            String movieCd = (String) request.get("movieCd");
            Long userId = Long.valueOf(request.get("userId").toString());
            String content = (String) request.get("content");
            Integer rating = request.get("rating") != null ? 
                Integer.valueOf(request.get("rating").toString()) : null;

            Review review = reviewService.createReview(movieCd, userId, content, rating);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "리뷰가 작성되었습니다.",
                "reviewId", review.getId(),
                "reviewType", getReviewType(review)
            ));
        } catch (Exception e) {
            log.error("리뷰 작성 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "리뷰 작성에 실패했습니다: " + e.getMessage()
            ));
        }
    }

    /**
     * 리뷰 수정
     */
    @PutMapping("/{reviewId}")
    public ResponseEntity<Map<String, Object>> updateReview(
            @PathVariable Long reviewId,
            @RequestBody Map<String, Object> request) {
        try {
            Long userId = Long.valueOf(request.get("userId").toString());
            String content = (String) request.get("content");
            Integer rating = request.get("rating") != null ? 
                Integer.valueOf(request.get("rating").toString()) : null;

            Review review = reviewService.updateReview(reviewId, userId, content, rating);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "리뷰가 수정되었습니다.",
                "reviewId", review.getId(),
                "reviewType", getReviewType(review)
            ));
        } catch (Exception e) {
            log.error("리뷰 수정 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "리뷰 수정에 실패했습니다: " + e.getMessage()
            ));
        }
    }

    /**
     * 리뷰 삭제
     */
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Map<String, Object>> deleteReview(
            @PathVariable Long reviewId,
            @RequestBody Map<String, Object> request) {
        try {
            Long userId = Long.valueOf(request.get("userId").toString());
            
            reviewService.deleteReview(reviewId, userId);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "리뷰가 삭제되었습니다."
            ));
        } catch (Exception e) {
            log.error("리뷰 삭제 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "리뷰 삭제에 실패했습니다: " + e.getMessage()
            ));
        }
    }

    /**
     * 영화의 모든 리뷰 조회
     */
    @GetMapping("/movie/{movieCd}")
    public ResponseEntity<Map<String, Object>> getReviewsByMovie(@PathVariable String movieCd) {
        try {
            List<Review> reviews = reviewService.getReviewsByMovieCd(movieCd);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", reviews,
                "count", reviews.size()
            ));
        } catch (Exception e) {
            log.error("리뷰 조회 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "리뷰 조회에 실패했습니다: " + e.getMessage()
            ));
        }
    }

    /**
     * 영화의 댓글만 있는 리뷰 조회 (평점 없음)
     */
    @GetMapping("/movie/{movieCd}/content-only")
    public ResponseEntity<Map<String, Object>> getContentOnlyReviews(@PathVariable String movieCd) {
        try {
            List<Review> reviews = reviewService.getContentOnlyReviewsByMovieCd(movieCd);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", reviews,
                "count", reviews.size()
            ));
        } catch (Exception e) {
            log.error("댓글만 조회 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "댓글만 조회에 실패했습니다: " + e.getMessage()
            ));
        }
    }

    /**
     * 영화의 평점 통계 조회 (왓챠피디아 스타일)
     */
    @GetMapping("/movie/{movieCd}/rating-stats")
    public ResponseEntity<Map<String, Object>> getRatingStats(@PathVariable String movieCd) {
        try {
            Double averageRating = reviewService.getAverageRating(movieCd);
            Long ratingCount = reviewService.getRatingCount(movieCd);
            Long contentReviewCount = reviewService.getContentReviewCount(movieCd);
            Map<Integer, Long> ratingDistribution = reviewService.getRatingDistribution(movieCd);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "movieCd", movieCd,
                "averageRating", averageRating,
                "ratingCount", ratingCount,
                "contentReviewCount", contentReviewCount,
                "ratingDistribution", ratingDistribution
            ));
        } catch (Exception e) {
            log.error("평점 통계 조회 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "평점 통계 조회에 실패했습니다: " + e.getMessage()
            ));
        }
    }

    /**
     * 사용자가 특정 영화에 작성한 리뷰 조회
     */
    @GetMapping("/movie/{movieCd}/user/{userId}")
    public ResponseEntity<Map<String, Object>> getUserReviewForMovie(
            @PathVariable String movieCd,
            @PathVariable Long userId) {
        try {
            Optional<Review> review = reviewService.getReviewByUserAndMovie(userId, movieCd);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", review.orElse(null),
                "exists", review.isPresent(),
                "reviewType", review.map(this::getReviewType).orElse(null)
            ));
        } catch (Exception e) {
            log.error("사용자 리뷰 조회 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "사용자 리뷰 조회에 실패했습니다: " + e.getMessage()
            ));
        }
    }

    /**
     * 사용자의 모든 리뷰 조회
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<Map<String, Object>> getUserReviews(@PathVariable Long userId) {
        try {
            List<Review> reviews = reviewService.getReviewsByUserId(userId);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", reviews,
                "count", reviews.size()
            ));
        } catch (Exception e) {
            log.error("사용자 리뷰 조회 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "사용자 리뷰 조회에 실패했습니다: " + e.getMessage()
            ));
        }
    }

    /**
     * 리뷰 타입 확인 메서드
     */
    private String getReviewType(Review review) {
        if (review.hasContent() && review.hasRating()) {
            return "FULL_REVIEW";
        } else if (review.hasContent()) {
            return "CONTENT_ONLY";
        } else if (review.hasRating()) {
            return "RATING_ONLY";
        } else {
            return "EMPTY";
        }
    }
} 

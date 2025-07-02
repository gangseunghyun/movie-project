package com.movie.movie_backend.controller;

import com.movie.movie_backend.dto.ReviewDto;
import com.movie.movie_backend.entity.Review;
import com.movie.movie_backend.entity.User;
import com.movie.movie_backend.service.REVReviewService;
import com.movie.movie_backend.repository.USRUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.movie.movie_backend.dto.ReviewRequestDto;
import com.movie.movie_backend.dto.ReviewResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final REVReviewService reviewService;
    private final USRUserRepository userRepository;

    /**
     * 리뷰 작성
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createReview(@RequestBody Map<String, Object> request, @AuthenticationPrincipal Object principal) {
        try {
            // 사용자 정보 추출
            String userEmail = null;
            if (principal instanceof DefaultOAuth2User) {
                DefaultOAuth2User oauth2User = (DefaultOAuth2User) principal;
                userEmail = oauth2User.getAttribute("email");
            }
            
            if (userEmail == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "로그인이 필요합니다.");
                return ResponseEntity.badRequest().body(response);
            }
            
            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
            
            // 요청 데이터 파싱
            String movieCd = null;
            if (request.get("movieCd") != null) {
                movieCd = (String) request.get("movieCd");
            } else if (request.get("movieId") != null) {
                // movieId가 movieCd인 경우
                movieCd = request.get("movieId").toString();
            } else {
                throw new RuntimeException("movieCd 또는 movieId가 필요합니다.");
            }
            
            String content = (String) request.get("content");
            Integer rating = null;
            if (request.get("rating") != null) {
                try {
                    rating = Integer.valueOf(request.get("rating").toString());
                } catch (NumberFormatException e) {
                    log.warn("평점 형식이 올바르지 않습니다: {}", request.get("rating"));
                }
            }

            Review review = reviewService.createReview(movieCd, user.getId(), content, rating);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "리뷰가 작성되었습니다.");
            response.put("reviewId", review.getId());
            response.put("reviewType", getReviewType(review));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("리뷰 작성 실패: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "리뷰 작성에 실패했습니다: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
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
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "리뷰가 수정되었습니다.");
            response.put("reviewId", review.getId());
            response.put("reviewType", getReviewType(review));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("리뷰 수정 실패: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "리뷰 수정에 실패했습니다: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
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
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "리뷰가 삭제되었습니다.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("리뷰 삭제 실패: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "리뷰 삭제에 실패했습니다: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 영화의 모든 리뷰 조회
     */
    @GetMapping("/movie/{movieCd}")
    public ResponseEntity<Map<String, Object>> getReviewsByMovie(@PathVariable String movieCd) {
        try {
            List<ReviewDto> reviews = reviewService.getReviewsByMovieCd(movieCd);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", reviews);
            response.put("count", reviews.size());
            response.put("message", "리뷰 목록을 조회했습니다.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("리뷰 목록 조회 실패: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "리뷰 목록 조회에 실패했습니다: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 영화의 댓글만 있는 리뷰 조회 (평점 없음)
     */
    @GetMapping("/movie/{movieCd}/content-only")
    public ResponseEntity<Map<String, Object>> getContentOnlyReviews(@PathVariable String movieCd) {
        try {
            List<ReviewDto> reviews = reviewService.getContentOnlyReviewsByMovieCd(movieCd);
            
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
     * 사용자가 특정 영화에 작성한 리뷰 조회
     */
    @GetMapping("/movie/{movieCd}/user/{userId}")
    public ResponseEntity<Map<String, Object>> getUserReviewForMovie(
            @PathVariable String movieCd,
            @PathVariable Long userId) {
        try {
            Optional<ReviewDto> review = reviewService.getReviewByUserAndMovie(userId, movieCd);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", review.orElse(null),
                "exists", review.isPresent(),
                "reviewType", getReviewTypeFromDto(review.orElse(null))
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
            List<ReviewDto> reviews = reviewService.getReviewsByUserId(userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", reviews);
            response.put("count", reviews.size());
            response.put("message", "사용자 리뷰 목록을 조회했습니다.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("사용자 리뷰 목록 조회 실패: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "사용자 리뷰 목록 조회에 실패했습니다: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 리뷰 타입 확인 메서드 (Review 엔티티용)
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

    /**
     * 리뷰 타입 확인 메서드 (ReviewDto용)
     */
    private String getReviewTypeFromDto(ReviewDto review) {
        if (review.getContent() != null && !review.getContent().isEmpty() && review.getRating() != null) {
            return "FULL_REVIEW";
        } else if (review.getContent() != null && !review.getContent().isEmpty()) {
            return "CONTENT_ONLY";
        } else if (review.getRating() != null) {
            return "RATING_ONLY";
        } else {
            return "EMPTY";
        }
    }

    /**
     * [DTO 기반] 리뷰 등록
     */
    @PostMapping("/dto")
    public ResponseEntity<ReviewResponseDto> createReviewDto(@RequestBody ReviewRequestDto requestDto) {
        ReviewResponseDto responseDto = reviewService.createReviewDto(requestDto);
        return ResponseEntity.ok(responseDto);
    }

    /**
     * [DTO 기반] 리뷰 수정
     */
    @PutMapping("/dto/{reviewId}")
    public ResponseEntity<ReviewResponseDto> updateReviewDto(@PathVariable Long reviewId, @RequestBody ReviewRequestDto requestDto) {
        ReviewResponseDto responseDto = reviewService.updateReviewDto(reviewId, requestDto);
        return ResponseEntity.ok(responseDto);
    }

    /**
     * [DTO 기반] 리뷰 삭제
     */
    @DeleteMapping("/dto/{reviewId}")
    public ResponseEntity<Void> deleteReviewDto(@PathVariable Long reviewId, @RequestParam Long userId) {
        reviewService.deleteReviewDto(reviewId, userId);
        return ResponseEntity.ok().build();
    }

    /**
     * [DTO 기반] 영화별 리뷰 목록(정렬/페이징)
     */
    @GetMapping("/dto/list")
    public ResponseEntity<Page<ReviewResponseDto>> getReviewsByMovieDto(
            @RequestParam Long movieId,
            @RequestParam(defaultValue = "date") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<ReviewResponseDto> pageResult = reviewService.getReviewsByMovieDto(movieId, sort, page, size);
        return ResponseEntity.ok(pageResult);
    }

    /**
     * [DTO 기반] 리뷰 상세(댓글 포함)
     */
    @GetMapping("/dto/{reviewId}")
    public ResponseEntity<ReviewResponseDto> getReviewDetailDto(@PathVariable Long reviewId, @RequestParam Long userId) {
        ReviewResponseDto responseDto = reviewService.getReviewDetailDto(reviewId, userId);
        return ResponseEntity.ok(responseDto);
    }

    /**
     * [DTO 기반] 리뷰 좋아요/취소
     */
    @PostMapping("/dto/{reviewId}/like")
    public ResponseEntity<Void> likeReview(@PathVariable Long reviewId, @RequestParam Long userId) {
        reviewService.likeReview(reviewId, userId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/dto/{reviewId}/like")
    public ResponseEntity<Void> unlikeReview(@PathVariable Long reviewId, @RequestParam Long userId) {
        reviewService.unlikeReview(reviewId, userId);
        return ResponseEntity.ok().build();
    }
} 

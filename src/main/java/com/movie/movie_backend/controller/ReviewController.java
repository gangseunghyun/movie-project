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
import com.movie.movie_backend.entity.ReviewLike;
import com.movie.movie_backend.repository.ReviewLikeRepository;
import com.movie.movie_backend.service.USRUserService;

@Slf4j
@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final REVReviewService reviewService;
    private final USRUserRepository userRepository;
    private final ReviewLikeRepository reviewLikeRepository;
    private final USRUserService userService;

    /**
     * 리뷰 작성
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createReview(@RequestBody Map<String, Object> request, @AuthenticationPrincipal Object principal) {
        try {
            // 사용자 정보 추출 - getCurrentUserId 메서드 사용 (소셜 + 일반 로그인 모두 지원)
            Long currentUserId = getCurrentUserId(principal);
            log.info("리뷰 작성 - 현재 사용자 ID: {}", currentUserId);
            
            if (currentUserId == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "로그인이 필요합니다.");
                return ResponseEntity.badRequest().body(response);
            }
            
            User user = userRepository.findById(currentUserId)
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
            Double rating = null;
            if (request.get("rating") != null) {
                try {
                    rating = Double.valueOf(request.get("rating").toString());
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
            @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal Object principal) {
        try {
            // 사용자 정보 추출 - getCurrentUserId 메서드 사용 (소셜 + 일반 로그인 모두 지원)
            Long currentUserId = getCurrentUserId(principal);
            log.info("리뷰 수정 - 현재 사용자 ID: {}", currentUserId);
            
            if (currentUserId == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "로그인이 필요합니다.");
                return ResponseEntity.badRequest().body(response);
            }
            
            String content = (String) request.get("content");
            Double rating = request.get("rating") != null ? 
                Double.valueOf(request.get("rating").toString()) : null;

            Review review = reviewService.updateReview(reviewId, currentUserId, content, rating);
            
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
            @AuthenticationPrincipal Object principal) {
        try {
            // 사용자 정보 추출 - getCurrentUserId 메서드 사용 (소셜 + 일반 로그인 모두 지원)
            Long currentUserId = getCurrentUserId(principal);
            log.info("리뷰 삭제 - 현재 사용자 ID: {}", currentUserId);
            
            if (currentUserId == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "로그인이 필요합니다.");
                return ResponseEntity.badRequest().body(response);
            }
            
            reviewService.deleteReview(reviewId, currentUserId);
            
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
     * 영화의 모든 리뷰 조회 (페이지네이션 + 정렬)
     */
    @GetMapping("/movie/{movieCd}")
    public ResponseEntity<Map<String, Object>> getReviewsByMovie(
            @PathVariable String movieCd,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "latest") String sort,
            @AuthenticationPrincipal Object principal) {
        try {
            Long currentUserId = null;
            if (principal != null) {
                currentUserId = getCurrentUserId(principal);
            }
            log.info("리뷰 목록 조회 - 현재 사용자 ID: {}", currentUserId);

            // 전체 리뷰 가져오기
            List<ReviewDto> reviews = reviewService.getReviewsByMovieCdWithLikeInfo(movieCd, currentUserId);

            // 정렬
            switch (sort) {
                case "like":
                    reviews.sort((a, b) -> Integer.compare(b.getLikeCount(), a.getLikeCount()));
                    break;
                case "ratingDesc":
                    reviews.sort((a, b) -> Double.compare(
                        b.getRating() != null ? b.getRating() : 0.0,
                        a.getRating() != null ? a.getRating() : 0.0));
                    break;
                case "ratingAsc":
                    reviews.sort((a, b) -> Double.compare(
                        a.getRating() != null ? a.getRating() : 0.0,
                        b.getRating() != null ? b.getRating() : 0.0));
                    break;
                case "latest":
                default:
                    reviews.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
                    break;
            }

            // 페이지네이션
            int total = reviews.size();
            int start = page * size;
            int end = Math.min(start + size, total);
            List<ReviewDto> pagedList = (start < end) ? reviews.subList(start, end) : List.of();

            for (ReviewDto review : pagedList) {
                log.info("리뷰 ID: {}, isBlockedByCleanbot: {}, content: {}", 
                    review.getId(), review.isBlockedByCleanbot(), review.getContent());
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", pagedList);
            response.put("count", pagedList.size());
            response.put("page", page);
            response.put("size", size);
            response.put("total", total);
            response.put("totalPages", (int) Math.ceil((double) total / size));
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
     * 현재 로그인한 사용자가 특정 영화에 리뷰를 작성했는지 확인
     */
    @GetMapping("/movie/{movieCd}/check-user-review")
    public ResponseEntity<Map<String, Object>> checkUserReviewForMovie(
            @PathVariable String movieCd,
            @AuthenticationPrincipal Object principal) {
        try {
            // 사용자 정보 추출
            Long currentUserId = getCurrentUserId(principal);
            log.info("리뷰 작성 여부 확인 - 현재 사용자 ID: {}, 영화 코드: {}", currentUserId, movieCd);
            
            if (currentUserId == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "로그인이 필요합니다.");
                response.put("hasReview", false);
                return ResponseEntity.ok(response);
            }
            
            Optional<ReviewDto> review = reviewService.getReviewByUserAndMovie(currentUserId, movieCd);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("hasReview", review.isPresent());
            if (review.isPresent()) {
                response.put("message", "이미 해당 영화에 리뷰를 작성했습니다.");
                response.put("reviewId", review.get().getId());
            } else {
                response.put("message", "해당 영화에 리뷰를 작성하지 않았습니다.");
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("리뷰 작성 여부 확인 실패: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "리뷰 작성 여부 확인에 실패했습니다: " + e.getMessage());
            response.put("hasReview", false);
            return ResponseEntity.badRequest().body(response);
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
    public ResponseEntity<Void> likeReview(@PathVariable Long reviewId, @AuthenticationPrincipal Object principal) {
        log.info("=== 리뷰 좋아요 API 호출 ===");
        log.info("리뷰ID: {}", reviewId);
        log.info("Principal 타입: {}", principal != null ? principal.getClass().getSimpleName() : "null");
        
        Long currentUserId = getCurrentUserId(principal);
        log.info("현재 사용자 ID: {}", currentUserId);
        
        if (currentUserId == null) {
            log.error("사용자 인증 실패 - 401 반환");
            return ResponseEntity.status(401).build();
        }
        
        reviewService.likeReview(reviewId, currentUserId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/dto/{reviewId}/like")
    public ResponseEntity<Void> unlikeReview(@PathVariable Long reviewId, @AuthenticationPrincipal Object principal) {
        Long currentUserId = getCurrentUserId(principal);
        if (currentUserId == null) {
            return ResponseEntity.status(401).build();
        }
        reviewService.unlikeReview(reviewId, currentUserId);
        return ResponseEntity.ok().build();
    }

    /**
     * 특정 리뷰를 좋아요한 유저 목록 조회
     */
    @GetMapping("/{reviewId}/liked-users")
    public ResponseEntity<Map<String, Object>> getLikedUsersForReview(@PathVariable Long reviewId, @AuthenticationPrincipal Object principal) {
        Long currentUserId = getCurrentUserId(principal);
        List<ReviewLike> likes = reviewService.getReviewLikesByReviewId(reviewId);
        List<Map<String, Object>> users = likes.stream().map(like -> {
            var user = like.getUser();
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("id", user.getId());
            userInfo.put("nickname", user.getNickname());
            userInfo.put("profileImageUrl", user.getProfileImageUrl());
            // 현재 사용자가 이 유저를 팔로우하고 있는지 확인 (로그인한 경우에만)
            if (currentUserId != null && !currentUserId.equals(user.getId())) {
                boolean isFollowing = userService.getFollowing(currentUserId).contains(user);
                userInfo.put("isFollowing", isFollowing);
            } else {
                userInfo.put("isFollowing", false);
            }
            return userInfo;
        }).toList();
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", users);
        response.put("count", users.size());
        return ResponseEntity.ok(response);
    }

    /**
     * 현재 인증된 사용자의 ID를 가져오는 헬퍼 메서드
     */
    private Long getCurrentUserId(Object principal) {
        log.info("getCurrentUserId 호출 - Principal: {}", principal);
        
        if (principal == null) {
            log.warn("Principal이 null입니다");
            return null;
        }
        
        log.info("Principal 클래스: {}", principal.getClass().getName());
        
        // anonymousUser 처리 (비로그인 상태)
        if (principal instanceof String && "anonymousUser".equals(principal)) {
            log.info("익명 사용자 - 로그인하지 않음");
            return null;
        }
        
        // AnonymousAuthenticationToken 처리 (비로그인 상태)
        if (principal instanceof org.springframework.security.authentication.AnonymousAuthenticationToken) {
            log.info("AnonymousAuthenticationToken - 로그인하지 않음");
            return null;
        }
        
        if (principal instanceof DefaultOAuth2User) {
            // OAuth2 로그인 (소셜 로그인)
            DefaultOAuth2User oauth2User = (DefaultOAuth2User) principal;
            String userEmail = oauth2User.getAttribute("email");
            log.info("OAuth2User 이메일: {}", userEmail);
            
            if (userEmail != null) {
                User user = userRepository.findByEmail(userEmail).orElse(null);
                log.info("DB에서 찾은 사용자: {}", user != null ? user.getId() : "null");
                return user != null ? user.getId() : null;
            }
        } else if (principal instanceof User) {
            // 일반 로그인 (세션 기반)
            User user = (User) principal;
            log.info("일반 로그인 사용자 ID: {}", user.getId());
            return user.getId();
        } else {
            log.warn("지원하지 않는 Principal 타입: {}", principal.getClass().getName());
        }
        
        return null;
    }
} 

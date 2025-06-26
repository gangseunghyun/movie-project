package com.movie.movie_backend.service;

import com.movie.movie_backend.entity.Review;
import com.movie.movie_backend.entity.User;
import com.movie.movie_backend.entity.MovieDetail;
import com.movie.movie_backend.repository.REVReviewRepository;
import com.movie.movie_backend.repository.USRUserRepository;
import com.movie.movie_backend.repository.PRDMovieRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class REVReviewService {

    private final REVReviewRepository reviewRepository;
    private final USRUserRepository userRepository;
    private final PRDMovieRepository movieRepository;

    /**
     * 리뷰 작성 (댓글만, 평점만, 둘 다 가능)
     */
    @Transactional
    public Review createReview(String movieCd, Long userId, String content, Integer rating) {
        log.info("리뷰 작성: 영화={}, 사용자={}, 평점={}", movieCd, userId, rating);

        // 사용자와 영화 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + userId));
        
        MovieDetail movie = movieRepository.findByMovieCd(movieCd)
                .orElseThrow(() -> new RuntimeException("영화를 찾을 수 없습니다: " + movieCd));

        // 이미 작성한 리뷰가 있는지 확인
        Review existingReview = reviewRepository.findByUserIdAndMovieDetailMovieCd(userId, movieCd);
        if (existingReview != null) {
            throw new RuntimeException("이미 이 영화에 리뷰를 작성했습니다.");
        }

        // 리뷰 생성
        Review review = new Review();
        review.setContent(content);
        review.setRating(rating);
        review.setUser(user);
        review.setMovieDetail(movie);
        review.setCreatedAt(LocalDateTime.now());
        review.setUpdatedAt(LocalDateTime.now());
        review.setStatus(Review.ReviewStatus.ACTIVE);

        Review savedReview = reviewRepository.save(review);
        log.info("리뷰 작성 완료: ID={}, 타입={}", savedReview.getId(), getReviewType(savedReview));

        return savedReview;
    }

    /**
     * 리뷰 수정
     */
    @Transactional
    public Review updateReview(Long reviewId, Long userId, String content, Integer rating) {
        log.info("리뷰 수정: 리뷰ID={}, 사용자={}, 평점={}", reviewId, userId, rating);

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("리뷰를 찾을 수 없습니다: " + reviewId));

        // 작성자 확인
        if (!review.getUser().getId().equals(userId)) {
            throw new RuntimeException("리뷰를 수정할 권한이 없습니다.");
        }

        // 리뷰 수정
        review.setContent(content);
        review.setRating(rating);
        review.setUpdatedAt(LocalDateTime.now());

        Review updatedReview = reviewRepository.save(review);
        log.info("리뷰 수정 완료: ID={}, 타입={}", updatedReview.getId(), getReviewType(updatedReview));

        return updatedReview;
    }

    /**
     * 리뷰 삭제 (소프트 삭제)
     */
    @Transactional
    public void deleteReview(Long reviewId, Long userId) {
        log.info("리뷰 삭제: 리뷰ID={}, 사용자={}", reviewId, userId);

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("리뷰를 찾을 수 없습니다: " + reviewId));

        // 작성자 확인
        if (!review.getUser().getId().equals(userId)) {
            throw new RuntimeException("리뷰를 삭제할 권한이 없습니다.");
        }

        // 소프트 삭제
        review.setStatus(Review.ReviewStatus.DELETED);
        reviewRepository.save(review);
        log.info("리뷰 삭제 완료: ID={}", reviewId);
    }

    /**
     * 영화의 모든 리뷰 조회
     */
    public List<Review> getReviewsByMovieCd(String movieCd) {
        return reviewRepository.findByMovieDetailMovieCdOrderByCreatedAtDesc(movieCd);
    }

    /**
     * 영화의 평점이 있는 리뷰만 조회
     */
    public List<Review> getRatedReviewsByMovieCd(String movieCd) {
        return reviewRepository.findByMovieDetailMovieCdAndRatingIsNotNullOrderByCreatedAtDesc(movieCd);
    }

    /**
     * 영화의 댓글만 있는 리뷰 조회 (평점 없음)
     */
    public List<Review> getContentOnlyReviewsByMovieCd(String movieCd) {
        return reviewRepository.findByMovieDetailMovieCdAndRatingIsNullOrderByCreatedAtDesc(movieCd);
    }

    /**
     * 영화의 평균 평점 조회
     */
    public Double getAverageRating(String movieCd) {
        Double average = reviewRepository.getAverageRatingByMovieCd(movieCd);
        return average != null ? Math.round(average * 10.0) / 10.0 : null; // 소수점 첫째자리까지
    }

    /**
     * 영화의 평점 개수 조회
     */
    public Long getRatingCount(String movieCd) {
        return reviewRepository.getRatingCountByMovieCd(movieCd);
    }

    /**
     * 영화의 댓글 리뷰 개수 조회
     */
    public Long getContentReviewCount(String movieCd) {
        return (long) reviewRepository.findByMovieDetailMovieCdAndRatingIsNullOrderByCreatedAtDesc(movieCd).size();
    }

    /**
     * 영화의 평점 분포 조회 (왓챠피디아 스타일)
     */
    public Map<Integer, Long> getRatingDistribution(String movieCd) {
        List<Object[]> distribution = reviewRepository.getRatingDistributionByMovieCd(movieCd);
        Map<Integer, Long> result = new java.util.HashMap<>();
        
        // 1~5점까지 초기화
        for (int i = 1; i <= 5; i++) {
            result.put(i, 0L);
        }
        
        // 실제 데이터로 채우기
        for (Object[] row : distribution) {
            Integer rating = (Integer) row[0];
            Long count = (Long) row[1];
            result.put(rating, count);
        }
        
        return result;
    }

    /**
     * 사용자의 모든 리뷰 조회
     */
    public List<Review> getReviewsByUserId(Long userId) {
        return reviewRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * 사용자가 특정 영화에 작성한 리뷰 조회
     */
    public Optional<Review> getReviewByUserAndMovie(Long userId, String movieCd) {
        return Optional.ofNullable(reviewRepository.findByUserIdAndMovieDetailMovieCd(userId, movieCd));
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

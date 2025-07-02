package com.movie.movie_backend.service;

import com.movie.movie_backend.dto.ReviewDto;
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
import com.movie.movie_backend.dto.ReviewRequestDto;
import com.movie.movie_backend.dto.ReviewResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import com.movie.movie_backend.entity.ReviewLike;
import com.movie.movie_backend.entity.Comment;
import com.movie.movie_backend.repository.ReviewLikeRepository;
import com.movie.movie_backend.repository.REVCommentRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class REVReviewService {

    private final REVReviewRepository reviewRepository;
    private final USRUserRepository userRepository;
    private final PRDMovieRepository movieRepository;
    private final ReviewLikeRepository reviewLikeRepository;
    private final REVCommentRepository commentRepository;

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
    public List<ReviewDto> getReviewsByMovieCd(String movieCd) {
        List<Review> reviews = reviewRepository.findByMovieDetailMovieCdOrderByCreatedAtDesc(movieCd);
        return reviews.stream()
                .map(ReviewDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 영화의 모든 리뷰 조회 (좋아요 정보 포함)
     */
    public List<ReviewDto> getReviewsByMovieCdWithLikeInfo(String movieCd, Long currentUserId) {
        List<Review> reviews = reviewRepository.findByMovieDetailMovieCdOrderByCreatedAtDesc(movieCd);
        return reviews.stream()
                .map(review -> {
                    ReviewDto dto = ReviewDto.fromEntity(review);
                    // 좋아요 개수 설정
                    int likeCount = reviewLikeRepository.countByReviewId(review.getId());
                    dto.setLikeCount(likeCount);
                    
                    // 현재 사용자가 좋아요를 눌렀는지 확인
                    if (currentUserId != null) {
                        boolean likedByMe = reviewLikeRepository.existsByReviewIdAndUserId(review.getId(), currentUserId);
                        dto.setLikedByMe(likedByMe);
                    } else {
                        dto.setLikedByMe(false);
                    }
                    
                    return dto;
                })
                .collect(Collectors.toList());
    }

    /**
     * 영화의 평점이 있는 리뷰만 조회
     */
    public List<ReviewDto> getRatedReviewsByMovieCd(String movieCd) {
        List<Review> reviews = reviewRepository.findByMovieDetailMovieCdAndRatingIsNotNullOrderByCreatedAtDesc(movieCd);
        return reviews.stream()
                .map(ReviewDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 영화의 댓글만 있는 리뷰 조회 (평점 없음)
     */
    public List<ReviewDto> getContentOnlyReviewsByMovieCd(String movieCd) {
        List<Review> reviews = reviewRepository.findByMovieDetailMovieCdAndRatingIsNullOrderByCreatedAtDesc(movieCd);
        return reviews.stream()
                .map(ReviewDto::fromEntity)
                .collect(Collectors.toList());
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
    public List<ReviewDto> getReviewsByUserId(Long userId) {
        List<Review> reviews = reviewRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return reviews.stream()
                .map(ReviewDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 사용자가 특정 영화에 작성한 리뷰 조회
     */
    public Optional<ReviewDto> getReviewByUserAndMovie(Long userId, String movieCd) {
        Review review = reviewRepository.findByUserIdAndMovieDetailMovieCd(userId, movieCd);
        return Optional.ofNullable(review != null ? ReviewDto.fromEntity(review) : null);
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

    // [DTO 기반] 리뷰 등록
    @Transactional
    public ReviewResponseDto createReviewDto(ReviewRequestDto dto) {
        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + dto.getUserId()));
        MovieDetail movie = movieRepository.findById(dto.getMovieId())
                .orElseThrow(() -> new RuntimeException("영화를 찾을 수 없습니다: " + dto.getMovieId()));
        Review review = Review.builder()
                .content(dto.getContent())
                .rating(dto.getRating())
                .user(user)
                .movieDetail(movie)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .status(Review.ReviewStatus.ACTIVE)
                .build();
        Review saved = reviewRepository.save(review);
        return toResponseDto(saved, false);
    }

    // [DTO 기반] 리뷰 수정
    @Transactional
    public ReviewResponseDto updateReviewDto(Long reviewId, ReviewRequestDto dto) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("리뷰를 찾을 수 없습니다: " + reviewId));
        if (!review.getUser().getId().equals(dto.getUserId())) {
            throw new RuntimeException("리뷰를 수정할 권한이 없습니다.");
        }
        review.setContent(dto.getContent());
        review.setRating(dto.getRating());
        review.setUpdatedAt(LocalDateTime.now());
        Review updated = reviewRepository.save(review);
        return toResponseDto(updated, false);
    }

    // [DTO 기반] 리뷰 삭제
    @Transactional
    public void deleteReviewDto(Long reviewId, Long userId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("리뷰를 찾을 수 없습니다: " + reviewId));
        if (!review.getUser().getId().equals(userId)) {
            throw new RuntimeException("리뷰를 삭제할 권한이 없습니다.");
        }
        review.setStatus(Review.ReviewStatus.DELETED);
        reviewRepository.save(review);
    }

    // [DTO 기반] 영화별 리뷰 목록(정렬/페이징)
    public Page<ReviewResponseDto> getReviewsByMovieDto(Long movieId, String sort, int page, int size) {
        List<Review> reviews = reviewRepository.findByMovieDetailIdOrderByCreatedAtDesc(movieId); // 정렬/페이징은 임시
        int start = page * size;
        int end = Math.min(start + size, reviews.size());
        List<ReviewResponseDto> dtoList = reviews.subList(start, end).stream()
                .map(r -> toResponseDto(r, false)).toList();
        return new PageImpl<>(dtoList, PageRequest.of(page, size), reviews.size());
    }

    // [DTO 기반] 리뷰 상세(댓글 포함)
    public ReviewResponseDto getReviewDetailDto(Long reviewId, Long userId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("리뷰를 찾을 수 없습니다: " + reviewId));
        boolean likedByMe = reviewLikeRepository.existsByReviewIdAndUserId(reviewId, userId);
        return toResponseDto(review, likedByMe);
    }

    // [DTO 기반] 리뷰 좋아요/취소
    @Transactional
    public void likeReview(Long reviewId, Long userId) {
        System.out.println("=== 리뷰 좋아요 시도 ===");
        System.out.println("리뷰ID: " + reviewId);
        System.out.println("사용자ID: " + userId);
        
        boolean alreadyLiked = reviewLikeRepository.existsByReviewIdAndUserId(reviewId, userId);
        System.out.println("이미 좋아요를 눌렀는지 확인: " + alreadyLiked);
        
        if (!alreadyLiked) {
            System.out.println("좋아요 추가 시작...");
            
            ReviewLike reviewLike = ReviewLike.builder()
                .review( reviewRepository.findById(reviewId).orElseThrow(() -> new RuntimeException("리뷰 없음")) )
                .user( userRepository.findById(userId).orElseThrow(() -> new RuntimeException("유저 없음")) )
                .createdAt(LocalDateTime.now())
                .build();
            
            ReviewLike savedLike = reviewLikeRepository.save(reviewLike);
            System.out.println("리뷰 좋아요 추가 완료: 좋아요ID=" + savedLike.getId());
        } else {
            System.out.println("이미 좋아요를 눌렀으므로 추가하지 않음");
        }
        
        System.out.println("=== 리뷰 좋아요 시도 완료 ===");
    }
    @Transactional
    public void unlikeReview(Long reviewId, Long userId) {
        System.out.println("=== 리뷰 좋아요 취소 시도 ===");
        System.out.println("리뷰ID: " + reviewId);
        System.out.println("사용자ID: " + userId);
        
        boolean exists = reviewLikeRepository.existsByReviewIdAndUserId(reviewId, userId);
        System.out.println("좋아요가 존재하는지 확인: " + exists);
        
        if (exists) {
            reviewLikeRepository.deleteByReviewIdAndUserId(reviewId, userId);
            System.out.println("리뷰 좋아요 취소 완료");
        } else {
            System.out.println("좋아요가 존재하지 않으므로 취소하지 않음");
        }
        
        System.out.println("=== 리뷰 좋아요 취소 시도 완료 ===");
    }

    // Review -> ReviewResponseDto 변환
    private ReviewResponseDto toResponseDto(Review review, boolean likedByMe) {
        int likeCount = reviewLikeRepository.countByReviewId(review.getId());
        int commentCount = commentRepository.countByReviewIdAndParentIsNull(review.getId());
        return ReviewResponseDto.builder()
                .id(review.getId())
                .content(review.getContent())
                .rating(review.getRating())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .username(review.getUser().getNickname())
                .userId(review.getUser().getId())
                .movieId(review.getMovieDetail().getId())
                .likeCount(likeCount)
                .likedByMe(likedByMe)
                .commentCount(commentCount)
                .build();
    }
} 

package com.movie.movie_backend.service;

import com.movie.movie_backend.entity.Rating;
import com.movie.movie_backend.entity.User;
import com.movie.movie_backend.entity.MovieDetail;
import com.movie.movie_backend.repository.REVRatingRepository;
import com.movie.movie_backend.repository.PRDMovieRepository;
import com.movie.movie_backend.repository.USRUserRepository;
import com.movie.movie_backend.dto.RatingDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class REVRatingService {
    private final REVRatingRepository ratingRepository;
    private final PRDMovieRepository movieRepository;
    private final USRUserRepository userRepository;
    private final PersonalizedRecommendationService recommendationService;

    /**
     * 사용자가 영화에 별점 등록/수정
     */
    @CacheEvict(value = {"averageRatings", "topRatedMovies"}, allEntries = true)
    public RatingDto saveRating(String userEmail, String movieCd, Double score) {
        log.info("별점 저장 요청: user={}, movie={}, score={}", userEmail, movieCd, score);
        
        // 사용자 조회
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        
        // 영화 조회
        MovieDetail movie = movieRepository.findByMovieCd(movieCd)
                .orElseThrow(() -> new RuntimeException("영화를 찾을 수 없습니다: " + movieCd));
        
        // 기존 별점 조회 - 효율적인 쿼리 사용
        Optional<Rating> existingRating = ratingRepository.findByUserAndMovieDetail(user, movie);
        
        Rating rating;
        if (existingRating.isPresent()) {
            // 기존 별점 수정
            rating = existingRating.get();
            rating.setScore(score);
            rating.setCreatedAt(LocalDateTime.now());
            log.info("기존 별점 수정: {}", rating.getId());
        } else {
            // 새 별점 등록
            rating = new Rating();
            rating.setUser(user);
            rating.setMovieDetail(movie);
            rating.setScore(score);
            rating.setCreatedAt(LocalDateTime.now());
            log.info("새 별점 등록");
        }
        
        Rating savedRating = ratingRepository.save(rating);
        
        // MovieDetail의 평점 캐시 업데이트
        updateMovieRatingCache(movieCd);
        
        // 추천 캐시 무효화
        recommendationService.evictUserRecommendations(user.getId());
        
        return convertToDto(savedRating);
    }
    
    /**
     * 사용자의 별점 삭제
     */
    @CacheEvict(value = {"averageRatings", "topRatedMovies"}, allEntries = true)
    public void deleteRating(String userEmail, String movieCd) {
        log.info("별점 삭제 요청: user={}, movie={}", userEmail, movieCd);
        
        // 사용자 조회
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        
        // 영화 조회
        MovieDetail movie = movieRepository.findByMovieCd(movieCd)
                .orElseThrow(() -> new RuntimeException("영화를 찾을 수 없습니다: " + movieCd));
        
        // 기존 별점 조회 - 효율적인 쿼리 사용
        Optional<Rating> existingRating = ratingRepository.findByUserAndMovieDetail(user, movie);
        
        if (existingRating.isPresent()) {
            ratingRepository.delete(existingRating.get());
            log.info("별점 삭제 완료: {}", existingRating.get().getId());
            
            // MovieDetail의 평점 캐시 업데이트
            updateMovieRatingCache(movieCd);
            
            // 추천 캐시 무효화
            recommendationService.evictUserRecommendations(user.getId());
        } else {
            log.warn("삭제할 별점이 없습니다: user={}, movie={}", userEmail, movieCd);
        }
    }
    
    /**
     * 사용자가 특정 영화에 준 별점 조회
     */
    @Transactional(readOnly = true)
    public RatingDto getUserRating(String userEmail, String movieCd) {
        log.info("사용자 별점 조회: user={}, movie={}", userEmail, movieCd);
        
        // 사용자 조회
        User user = userRepository.findByEmail(userEmail)
                .orElse(null);
        
        if (user == null) {
            return null;
        }
        
        // 영화 조회
        MovieDetail movie = movieRepository.findByMovieCd(movieCd)
                .orElse(null);
        
        if (movie == null) {
            return null;
        }
        
        // 별점 조회 - 효율적인 쿼리 사용
        Optional<Rating> rating = ratingRepository.findByUserAndMovieDetail(user, movie);
        
        return rating.map(this::convertToDto).orElse(null);
    }
    
    /**
     * 사용자의 모든 별점 조회
     */
    @Transactional(readOnly = true)
    public List<RatingDto> getUserRatings(String userEmail) {
        log.info("사용자 모든 별점 조회: user={}", userEmail);
        
        // 사용자 조회
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        
        // 별점 조회 - 효율적인 쿼리 사용
        List<Rating> ratings = ratingRepository.findByUser(user);
        
        return ratings.stream()
                .map(this::convertToDto)
                .toList();
    }

    /**
     * 영화별 평점 조회 (기존 메서드)
     */
    @Transactional(readOnly = true)
    public List<Rating> getRatingsByMovieDetail(String movieCd) {
        return ratingRepository.findByMovieDetailMovieCd(movieCd);
    }

    /**
     * 영화별 평점 개수 조회 (기존 메서드)
     */
    @Transactional(readOnly = true)
    public long getRatingCountByMovieDetail(String movieCd) {
        return ratingRepository.countByMovieDetailMovieCd(movieCd);
    }
    
    /**
     * MovieDetail의 평점 캐시 업데이트
     */
    private void updateMovieRatingCache(String movieCd) {
        try {
            MovieDetail movie = movieRepository.findByMovieCd(movieCd).orElse(null);
            if (movie != null) {
                Double averageRating = getAverageRating(movieCd);
                Integer ratingCount = (int) getRatingCountByMovieDetail(movieCd);
                
                movie.setAverageRating(averageRating);
                movie.setRatingCount(ratingCount);
                movie.setRatingUpdatedAt(LocalDateTime.now());
                
                movieRepository.save(movie);
                
                log.debug("영화 {}의 평점 캐시 업데이트: 평점={}, 개수={}", 
                        movie.getMovieNm(), averageRating, ratingCount);
            }
        } catch (Exception e) {
            log.warn("영화 {}의 평점 캐시 업데이트 실패: {}", movieCd, e.getMessage());
        }
    }
    
    /**
     * 영화의 평균 평점 조회
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "averageRatings", key = "#movieCd", unless="#result == null")
    public Double getAverageRating(String movieCd) {
        List<Rating> ratings = ratingRepository.findByMovieDetailMovieCd(movieCd);
        if (ratings.isEmpty()) {
            return null;
        }
        double average = ratings.stream()
                .mapToDouble(Rating::getScore)
                .average()
                .orElse(0.0);
        return Math.round(average * 10.0) / 10.0; // 소수점 첫째자리까지
    }
    
    /**
     * 영화의 별점 분포 조회 (0.5~5.0, 0.5 단위, 누락 구간은 0)
     */
    public Map<Double, Long> getRatingDistribution(String movieCd) {
        List<Rating> ratings = ratingRepository.findByMovieDetailMovieCd(movieCd);
        Map<Double, Long> distribution = new java.util.LinkedHashMap<>();
        // 0.5~5.0, 0.5 단위로 초기화
        for (double score = 0.5; score <= 5.0; score += 0.5) {
            distribution.put(Math.round(score * 10.0) / 10.0, 0L);
        }
        // 집계
        for (Rating rating : ratings) {
            double score = Math.round(rating.getScore() * 10.0) / 10.0;
            distribution.put(score, distribution.getOrDefault(score, 0L) + 1);
        }
        return distribution;
    }
    
    /**
     * Rating을 RatingDto로 변환
     */
    private RatingDto convertToDto(Rating rating) {
        return RatingDto.builder()
                .id(rating.getId())
                .movieCd(rating.getMovieDetail().getMovieCd())
                .movieNm(rating.getMovieDetail().getMovieNm())
                .score(rating.getScore())
                .createdAt(rating.getCreatedAt())
                .userEmail(rating.getUser().getEmail())
                .userNickname(rating.getUser().getNickname())
                .build();
    }
    
    /**
     * 여러 영화의 평균 평점을 한 번에 조회 (배치 조회 + 캐싱)
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "averageRatings", key = "#movieCds.hashCode()", unless="#result.isEmpty()")
    public Map<String, Double> getAverageRatingsForMovies(List<String> movieCds) {
        if (movieCds == null || movieCds.isEmpty()) {
            return new java.util.HashMap<>();
        }
        
        log.debug("배치 평점 조회 시작: {}개 영화", movieCds.size());
        
        List<Object[]> results = ratingRepository.getAverageRatingsForMovies(movieCds);
        Map<String, Double> averageRatings = new java.util.HashMap<>();
        
        for (Object[] result : results) {
            String movieCd = (String) result[0];
            Double avgRating = (Double) result[1];
            
            if (avgRating != null) {
                // 소수점 첫째자리까지 반올림
                double roundedRating = Math.round(avgRating * 10.0) / 10.0;
                averageRatings.put(movieCd, roundedRating);
            } else {
                averageRatings.put(movieCd, null);
            }
        }
        
        log.debug("배치 평점 조회 완료: {}개 영화의 평점 조회됨", averageRatings.size());
        return averageRatings;
    }
} 

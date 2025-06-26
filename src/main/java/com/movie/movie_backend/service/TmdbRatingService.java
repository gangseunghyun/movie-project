package com.movie.movie_backend.service;

import com.movie.movie_backend.entity.MovieDetail;
import com.movie.movie_backend.entity.Rating;
import com.movie.movie_backend.entity.User;
import com.movie.movie_backend.repository.PRDMovieRepository;
import com.movie.movie_backend.repository.REVRatingRepository;
import com.movie.movie_backend.repository.USRUserRepository;
import com.movie.movie_backend.repository.PRDMovieListRepository;
import com.movie.movie_backend.constant.UserRole;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import com.movie.movie_backend.mapper.TopRatedMovieMapper;
import com.movie.movie_backend.dto.TopRatedMovieDto;

@Slf4j
@Service
@RequiredArgsConstructor
public class TmdbRatingService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final PRDMovieRepository movieRepository;
    private final REVRatingRepository ratingRepository;
    private final USRUserRepository userRepository;
    private final PRDMovieListRepository movieListRepository;
    private final TopRatedMovieMapper topRatedMovieMapper;

    @Value("${tmdb.api.key}")
    private String tmdbApiKey;

    @Value("${tmdb.api.base-url}")
    private String tmdbBaseUrl;

    /**
     * Spring Boot 시작 시 자동으로 TMDB 평점 가져오기
     */
    // @PostConstruct  // 자동 실행 비활성화 (필요시 수동 호출)
    public void initializeRatings() {
        log.info("Spring Boot 시작 - TMDB 평점 자동 가져오기 시작");
        
        // 애플리케이션 시작 후 10초 대기 (다른 서비스들이 초기화될 시간)
        try {
            Thread.sleep(10000); // 10초 대기
            log.info("TMDB 평점 자동 가져오기 시작");
            fetchAndSaveTmdbRatings();
        } catch (Exception e) {
            log.error("Spring Boot 시작 시 TMDB 평점 가져오기 실패", e);
        }
    }

    /**
     * TMDB에서 영화 평점을 가져와서 Rating 테이블에 저장
     */
    @Transactional
    public void fetchAndSaveTmdbRatings() {
        log.info("TMDB 평점 가져오기 시작");
        
        try {
            // 모든 MovieDetail 조회
            List<MovieDetail> movies = movieRepository.findAll();
            log.info("총 {}개의 영화에서 TMDB 평점을 가져옵니다.", movies.size());
            
            // 임시 사용자 생성 (TMDB 평점용)
            User tmdbUser = getOrCreateTmdbUser();
            
            for (MovieDetail movie : movies) {
                try {
                    fetchAndSaveMovieRating(movie, tmdbUser);
                    Thread.sleep(250); // TMDB API 호출 제한 방지 (초당 4회)
                } catch (Exception e) {
                    log.warn("영화 {}의 TMDB 평점 가져오기 실패: {}", movie.getMovieNm(), e.getMessage());
                }
            }
            
            log.info("TMDB 평점 가져오기 완료");
            
        } catch (Exception e) {
            log.error("TMDB 평점 가져오기 중 오류 발생", e);
        }
    }

    /**
     * 특정 영화의 TMDB 평점을 가져와서 저장
     */
    @Transactional
    public void fetchAndSaveMovieRating(MovieDetail movie, User tmdbUser) {
        try {
            // TMDB에서 영화 검색
            String searchUrl = String.format("%s/search/movie?api_key=%s&query=%s&language=ko-KR",
                    tmdbBaseUrl, tmdbApiKey, movie.getMovieNm());
            
            String response = restTemplate.getForObject(searchUrl, String.class);
            JsonNode root = objectMapper.readTree(response);
            JsonNode results = root.get("results");
            
            if (results != null && results.isArray() && results.size() > 0) {
                // 첫 번째 검색 결과 사용 (가장 관련성 높은 결과)
                JsonNode firstResult = results.get(0);
                double voteAverage = firstResult.get("vote_average").asDouble();
                int voteCount = firstResult.get("vote_count").asInt();
                
                // 평점이 있는 경우만 저장 (vote_count > 0)
                if (voteCount > 0) {
                    // 기존 TMDB 평점이 있는지 확인
                    Optional<Rating> existingRating = ratingRepository.findAll().stream()
                            .filter(rating -> rating.getMovieDetail().getMovieCd().equals(movie.getMovieCd()) &&
                                            rating.getUser().getId().equals(tmdbUser.getId()))
                            .findFirst();
                    
                    if (existingRating.isPresent()) {
                        // 기존 평점 업데이트
                        Rating rating = existingRating.get();
                        rating.setScore((int) Math.round(voteAverage * 2)); // TMDB는 10점 만점, 우리는 5점 만점
                        rating.setCreatedAt(LocalDateTime.now());
                        ratingRepository.save(rating);
                        
                        // MovieDetail 캐시 업데이트
                        updateMovieDetailRatingCache(movie);
                        
                        log.info("영화 {}의 TMDB 평점 업데이트: {} (투표수: {})", 
                                movie.getMovieNm(), voteAverage, voteCount);
                    } else {
                        // 새 평점 생성
                        Rating rating = new Rating();
                        rating.setMovieDetail(movie);
                        rating.setUser(tmdbUser);
                        rating.setScore((int) Math.round(voteAverage * 2)); // TMDB는 10점 만점, 우리는 5점 만점
                        rating.setCreatedAt(LocalDateTime.now());
                        ratingRepository.save(rating);
                        
                        // MovieDetail 캐시 업데이트
                        updateMovieDetailRatingCache(movie);
                        
                        log.info("영화 {}의 TMDB 평점 저장: {} (투표수: {})", 
                                movie.getMovieNm(), voteAverage, voteCount);
                    }
                } else {
                    log.info("영화 {}의 TMDB 평점이 없습니다.", movie.getMovieNm());
                }
            } else {
                log.info("영화 {}을 TMDB에서 찾을 수 없습니다.", movie.getMovieNm());
            }
            
        } catch (Exception e) {
            log.warn("영화 {}의 TMDB 평점 가져오기 실패: {}", movie.getMovieNm(), e.getMessage());
        }
    }

    /**
     * TMDB 평점용 사용자 생성 또는 조회
     */
    private User getOrCreateTmdbUser() {
        Optional<User> existingUser = userRepository.findByLoginId("tmdb_rating_user");
        
        if (existingUser.isPresent()) {
            return existingUser.get();
        } else {
            User tmdbUser = User.builder()
                    .loginId("tmdb_rating_user")
                    .email("tmdb@rating.com")
                    .password("tmdb_rating_password")
                    .role(UserRole.USER)
                    .emailVerified(true)
                    .socialJoinCompleted(true)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            return userRepository.save(tmdbUser);
        }
    }

    /**
     * 영화의 평점 개수 조회
     */
    public Integer getRatingCount(String movieCd) {
        List<Rating> ratings = ratingRepository.findByMovieDetailMovieCd(movieCd);
        return ratings.size();
    }

    /**
     * 영화의 평균 평점 조회
     */
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
     * 평균 평점이 높은 영화 TOP-10 조회 (MovieList에도 있는 영화만)
     */
    public List<MovieDetail> getTopRatedMovies(int limit) {
        List<MovieDetail> allMovies = movieRepository.findAll();
        
        List<MovieDetail> topRatedMovies = allMovies.stream()
                .filter(movie -> {
                    // MovieList에도 있는 영화만 필터링
                    boolean hasMovieList = movieListRepository.findById(movie.getMovieCd()).isPresent();
                    // 평점이 있는 영화만 필터링
                    boolean hasRating = getAverageRating(movie.getMovieCd()) != null;
                    return hasMovieList && hasRating;
                })
                .sorted((m1, m2) -> {
                    Double rating1 = getAverageRating(m1.getMovieCd());
                    Double rating2 = getAverageRating(m2.getMovieCd());
                    return rating2.compareTo(rating1); // 내림차순 정렬
                })
                .limit(limit)
                .toList();
        
        // 포스터 URL이 없는 영화들은 자동으로 TMDB에서 가져오기
        for (MovieDetail movie : topRatedMovies) {
            try {
                var movieListOpt = movieListRepository.findById(movie.getMovieCd());
                if (movieListOpt.isPresent()) {
                    var movieList = movieListOpt.get();
                    if (movieList.getPosterUrl() == null || movieList.getPosterUrl().isEmpty()) {
                        // TMDB에서 포스터 URL 가져오기
                        String posterUrl = fetchPosterUrlFromTmdb(movie.getMovieNm(), movie.getOpenDt());
                        if (posterUrl != null) {
                            movieList.setPosterUrl(posterUrl);
                            movieListRepository.save(movieList);
                            log.info("Top Rated 영화 포스터 자동 가져오기: {} -> {}", movie.getMovieNm(), posterUrl);
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Top Rated 영화 포스터 자동 가져오기 실패: {}", movie.getMovieNm(), e);
            }
        }
        
        return topRatedMovies;
    }

    /**
     * TMDB에서 포스터 URL 가져오기
     */
    private String fetchPosterUrlFromTmdb(String movieName, LocalDate openDt) {
        try {
            String query = java.net.URLEncoder.encode(movieName, java.nio.charset.StandardCharsets.UTF_8);
            String year = (openDt != null) ? String.valueOf(openDt.getYear()) : null;
            String url = tmdbBaseUrl + "/search/movie" +
                    "?api_key=" + tmdbApiKey +
                    "&query=" + query +
                    (year != null ? "&year=" + year : "") +
                    "&language=ko-KR";

            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);
            JsonNode results = root.get("results");
            if (results != null && results.size() > 0) {
                String posterPath = results.get(0).get("poster_path").asText();
                if (posterPath != null && !posterPath.isEmpty()) {
                    return "https://image.tmdb.org/t/p/w500" + posterPath;
                }
            }
        } catch (Exception e) {
            log.warn("TMDB 포스터 검색 실패: {}", movieName, e);
        }
        return null;
    }

    /**
     * MovieDetail의 평점 캐시 업데이트
     */
    private void updateMovieDetailRatingCache(MovieDetail movie) {
        try {
            Double averageRating = getAverageRating(movie.getMovieCd());
            Integer ratingCount = getRatingCount(movie.getMovieCd());
            
            movie.setAverageRating(averageRating);
            movie.setRatingCount(ratingCount);
            movie.setRatingUpdatedAt(LocalDateTime.now());
            
            movieRepository.save(movie);
            
            log.debug("영화 {}의 평점 캐시 업데이트: 평점={}, 개수={}", 
                    movie.getMovieNm(), averageRating, ratingCount);
        } catch (Exception e) {
            log.warn("영화 {}의 평점 캐시 업데이트 실패: {}", movie.getMovieNm(), e.getMessage());
        }
    }

    /**
     * 평균 별점이 높은 영화 TOP-N 조회 (DTO)
     */
    public List<TopRatedMovieDto> getTopRatedMoviesAsDto(int limit) {
        List<MovieDetail> topRatedMovies = getTopRatedMovies(limit);
        return topRatedMovieMapper.toDtoList(topRatedMovies);
    }
} 

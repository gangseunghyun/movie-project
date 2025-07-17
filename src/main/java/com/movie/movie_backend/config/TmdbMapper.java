//package com.movie.movie_backend.config;
//
//import com.movie.movie_backend.repository.PRDMovieListRepository;
//import com.movie.movie_backend.entity.MovieList;
//import com.movie.movie_backend.service.TmdbPopularityService;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.core.annotation.Order;
//import org.springframework.scheduling.annotation.EnableScheduling;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.web.client.RestTemplate;
//import org.springframework.util.StringUtils;
//
//import java.util.List;
//import java.util.Map;
//import java.util.ArrayList;
//
//@Slf4j
//@Configuration
//@EnableScheduling
//@RequiredArgsConstructor
//public class TmdbMapper {
//
//    private final PRDMovieListRepository prdMovieListRepository;
//    private final RestTemplate restTemplate;
//    private final TmdbPopularityService tmdbPopularityService;
//
//    @Value("${tmdb.api.key}")
//    private String tmdbApiKey;
//
//    @Bean
//    @Order(6)  // KMDb 매핑 다음에 실행
//    public CommandLineRunner matchAndSaveTmdbIds() {
//        return args -> {
//            log.info("=== TMDB ID 매칭 및 저장 배치 시작 ===");
//
//            // TMDB API 테스트 먼저 실행
//            log.info("=== TMDB API 테스트 시작 ===");
//            testTmdbApi();
//            log.info("=== TMDB API 테스트 완료 ===");
//
//            // TMDB ID 매칭은 이미 완료되었으므로 주석처리
//            // // 1차 매칭 실행
//            // performInitialMatching();
//            // // 2차 매칭 실행 (실패한 영화들에 대해 추가 전략 적용)
//            // performSecondaryMatching();
//
//            // TMDB 인기도 업데이트
//            updateMoviePopularity();
//
//            log.info("=== TMDB 인기도 업데이트 배치 완료 ===");
//        };
//    }
//
//    // TMDB API 테스트
//    private void testTmdbApi() {
//        try {
//            String testUrl = String.format(
//                "https://api.themoviedb.org/3/search/movie?api_key=%s&query=인터스텔라&language=ko-KR",
//                tmdbApiKey
//            );
//
//            Map<String, Object> response = restTemplate.getForObject(testUrl, Map.class);
//            if (response != null && response.containsKey("results")) {
//                log.info("TMDB API 테스트 성공: {}개 결과", ((List<?>) response.get("results")).size());
//            } else {
//                log.warn("TMDB API 테스트 실패: 응답이 예상과 다름");
//            }
//        } catch (Exception e) {
//            log.error("TMDB API 테스트 실패: {}", e.getMessage());
//        }
//    }
//
//    // 1차 매칭 실행 메서드
//    private void performInitialMatching() {
//        log.info("=== 1차 TMDB ID 매칭 시작 ===");
//
//        // 배치 크기 설정
//        final int BATCH_SIZE = 10;
//        final int MAX_CONCURRENT_REQUESTS = 3;
//
//        List<MovieList> movieLists = prdMovieListRepository.findAll();
//        List<MovieList> unmappedMovies = movieLists.stream()
//            .filter(movie -> movie.getTmdbId() == null)
//            .collect(java.util.stream.Collectors.toList());
//
//        log.info("총 {}개 영화 중 {}개가 매칭 필요", movieLists.size(), unmappedMovies.size());
//
//        if (unmappedMovies.isEmpty()) {
//            log.info("모든 영화가 이미 매칭되어 있습니다.");
//            return;
//        }
//
//        int updated = 0, failed = 0, skipped = movieLists.size() - unmappedMovies.size();
//
//        // 배치 단위로 처리
//        for (int i = 0; i < unmappedMovies.size(); i += BATCH_SIZE) {
//            int endIndex = Math.min(i + BATCH_SIZE, unmappedMovies.size());
//            List<MovieList> batch = unmappedMovies.subList(i, endIndex);
//
//            log.info("1차 배치 처리 중: {}/{} ({}-{})",
//                (i / BATCH_SIZE) + 1,
//                (unmappedMovies.size() + BATCH_SIZE - 1) / BATCH_SIZE,
//                i + 1, endIndex);
//
//            // 배치 내에서 병렬 처리
//            java.util.concurrent.ExecutorService executor =
//                java.util.concurrent.Executors.newFixedThreadPool(MAX_CONCURRENT_REQUESTS);
//
//            try {
//                java.util.List<java.util.concurrent.Future<MovieMappingResult>> futures = new ArrayList<>();
//
//                for (MovieList movie : batch) {
//                    futures.add(executor.submit(() -> processMovie(movie)));
//                }
//
//                // 결과 수집
//                for (java.util.concurrent.Future<MovieMappingResult> future : futures) {
//                    try {
//                        MovieMappingResult result = future.get(30, java.util.concurrent.TimeUnit.SECONDS);
//                        if (result.success) {
//                            updated++;
//                            log.info("1차 매칭 성공: {} ({}) -> tmdbId={}",
//                                result.movieNm, result.movieCd, result.tmdbId);
//                        } else {
//                            failed++;
//                            if (failed <= 10) {
//                                log.warn("1차 매칭 실패: {} ({})", result.movieNm, result.movieCd);
//                            }
//                        }
//                    } catch (Exception e) {
//                        failed++;
//                        log.error("영화 처리 중 오류 발생: {}", e.getMessage());
//                    }
//                }
//
//            } finally {
//                executor.shutdown();
//                try {
//                    if (!executor.awaitTermination(60, java.util.concurrent.TimeUnit.SECONDS)) {
//                        executor.shutdownNow();
//                    }
//                } catch (InterruptedException e) {
//                    executor.shutdownNow();
//                    Thread.currentThread().interrupt();
//                }
//            }
//
//            // 배치 간 딜레이
//            if (i + BATCH_SIZE < unmappedMovies.size()) {
//                try {
//                    Thread.sleep(100);
//                } catch (InterruptedException e) {
//                    Thread.currentThread().interrupt();
//                    log.warn("1차 매칭 sleep 중단: {}", e.getMessage());
//                    break;
//                }
//            }
//        }
//
//        log.info("=== 1차 TMDB ID 매칭 완료: 성공 {}, 실패 {} ===", updated, failed);
//    }
//
//    // 2차 매칭 실행 메서드 (추가 검색 전략 적용)
//    private void performSecondaryMatching() {
//        log.info("=== 2차 TMDB ID 매칭 시작 (추가 전략) ===");
//
//        List<MovieList> movieLists = prdMovieListRepository.findAll();
//        List<MovieList> unmappedMovies = movieLists.stream()
//            .filter(movie -> movie.getTmdbId() == null)
//            .collect(java.util.stream.Collectors.toList());
//
//        log.info("2차 매칭 대상: {}개 영화", unmappedMovies.size());
//
//        if (unmappedMovies.isEmpty()) {
//            log.info("매칭할 영화가 없습니다.");
//            return;
//        }
//
//        int updated = 0, failed = 0;
//
//        for (MovieList movie : unmappedMovies) {
//            try {
//                MovieMappingResult result = processMovieWithAdvancedStrategy(movie);
//                if (result.success) {
//                    updated++;
//                    log.info("2차 매칭 성공: {} ({}) -> tmdbId={}",
//                        result.movieNm, result.movieCd, result.tmdbId);
//                } else {
//                    failed++;
//                    if (failed <= 20) {
//                        log.warn("2차 매칭 실패: {} ({})", result.movieNm, result.movieCd);
//                    }
//                }
//
//                // API 호출 제한을 위한 딜레이
//                try {
//                    Thread.sleep(200);
//                } catch (InterruptedException e) {
//                    Thread.currentThread().interrupt();
//                    log.warn("2차 매칭 sleep 중단: {}", e.getMessage());
//                    break;
//                }
//
//            } catch (Exception e) {
//                failed++;
//                log.error("2차 매칭 중 오류: {} ({}) - {}", movie.getMovieNm(), movie.getMovieCd(), e.getMessage());
//            }
//        }
//
//        log.info("=== 2차 TMDB ID 매칭 완료: 성공 {}, 실패 {} ===", updated, failed);
//    }
//
//    // TMDB 인기도 업데이트 메서드
//    private void updateMoviePopularity() {
//        log.info("=== TMDB 인기도 업데이트 시작 ===");
//        tmdbPopularityService.updateAllMoviePopularity();
//        log.info("=== TMDB 인기도 업데이트 완료 ===");
//    }
//
//    // 영화 처리 결과를 담는 클래스
//    private static class MovieMappingResult {
//        final boolean success;
//        final String movieCd;
//        final String movieNm;
//        final Integer tmdbId;
//
//        MovieMappingResult(boolean success, String movieCd, String movieNm, Integer tmdbId) {
//            this.success = success;
//            this.movieCd = movieCd;
//            this.movieNm = movieNm;
//            this.tmdbId = tmdbId;
//        }
//    }
//
//    // 기본 영화 처리 메서드
//    private MovieMappingResult processMovie(MovieList movie) {
//        try {
//            if (movie.getTmdbId() != null) {
//                return new MovieMappingResult(true, movie.getMovieCd(), movie.getMovieNm(), movie.getTmdbId());
//            }
//
//            Integer tmdbId = searchTmdbId(movie.getMovieNm());
//            if (tmdbId != null) {
//                movie.setTmdbId(tmdbId);
//                prdMovieListRepository.save(movie);
//                return new MovieMappingResult(true, movie.getMovieCd(), movie.getMovieNm(), tmdbId);
//            }
//
//            return new MovieMappingResult(false, movie.getMovieCd(), movie.getMovieNm(), null);
//
//        } catch (Exception e) {
//            log.error("영화 처리 중 오류: {} ({}) - {}", movie.getMovieNm(), movie.getMovieCd(), e.getMessage());
//            return new MovieMappingResult(false, movie.getMovieCd(), movie.getMovieNm(), null);
//        }
//    }
//
//    // 고급 전략을 사용한 영화 처리 메서드
//    private MovieMappingResult processMovieWithAdvancedStrategy(MovieList movie) {
//        try {
//            if (movie.getTmdbId() != null) {
//                return new MovieMappingResult(true, movie.getMovieCd(), movie.getMovieNm(), movie.getTmdbId());
//            }
//
//            // 1. 원제로 검색 시도
//            if (StringUtils.hasText(movie.getMovieNmEn())) {
//                Integer tmdbId = searchTmdbId(movie.getMovieNmEn());
//                if (tmdbId != null) {
//                    movie.setTmdbId(tmdbId);
//                    prdMovieListRepository.save(movie);
//                    return new MovieMappingResult(true, movie.getMovieCd(), movie.getMovieNm(), tmdbId);
//                }
//            }
//
//            // 2. 영화 제목에서 특수문자 제거 후 검색
//            String cleanedTitle = cleanMovieTitle(movie.getMovieNm());
//            if (!cleanedTitle.equals(movie.getMovieNm())) {
//                Integer tmdbId = searchTmdbId(cleanedTitle);
//                if (tmdbId != null) {
//                    movie.setTmdbId(tmdbId);
//                    prdMovieListRepository.save(movie);
//                    return new MovieMappingResult(true, movie.getMovieCd(), movie.getMovieNm(), tmdbId);
//                }
//            }
//
//            // 3. 영어 제목에서 특수문자 제거 후 검색
//            if (StringUtils.hasText(movie.getMovieNmEn())) {
//                String cleanedEnglishTitle = cleanMovieTitle(movie.getMovieNmEn());
//                if (!cleanedEnglishTitle.equals(movie.getMovieNmEn())) {
//                    Integer tmdbId = searchTmdbId(cleanedEnglishTitle);
//                    if (tmdbId != null) {
//                        movie.setTmdbId(tmdbId);
//                        prdMovieListRepository.save(movie);
//                        return new MovieMappingResult(true, movie.getMovieCd(), movie.getMovieNm(), tmdbId);
//                    }
//                }
//            }
//
//            return new MovieMappingResult(false, movie.getMovieCd(), movie.getMovieNm(), null);
//
//        } catch (Exception e) {
//            log.error("고급 전략 영화 처리 중 오류: {} ({}) - {}", movie.getMovieNm(), movie.getMovieCd(), e.getMessage());
//            return new MovieMappingResult(false, movie.getMovieCd(), movie.getMovieNm(), null);
//        }
//    }
//
//    // TMDB ID 검색 메서드
//    private Integer searchTmdbId(String movieTitle) {
//        try {
//            String url = String.format(
//                "https://api.themoviedb.org/3/search/movie?api_key=%s&query=%s&language=ko-KR",
//                tmdbApiKey, movieTitle
//            );
//
//            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
//            if (response != null && response.containsKey("results")) {
//                List<Map<String, Object>> results = (List<Map<String, Object>>) response.get("results");
//                if (!results.isEmpty()) {
//                    return (Integer) results.get(0).get("id");
//                }
//            }
//        } catch (Exception e) {
//            log.error("TMDB 검색 중 오류: {} - {}", movieTitle, e.getMessage());
//        }
//        return null;
//    }
//
//    // 영화 제목 정리 메서드
//    private String cleanMovieTitle(String title) {
//        if (title == null) return "";
//
//        return title
//            .replaceAll("[\\[\\](){}]", "") // 대괄호, 괄호 제거
//            .replaceAll("[:：]", "") // 콜론 제거
//            .replaceAll("\\s+", " ") // 연속된 공백을 하나로
//            .trim();
//    }
//
//    // 스케줄링된 인기도 업데이트 (매일 새벽 3시에 실행)
//    @Scheduled(cron = "0 0 3 * * ?")
//    public void scheduledTmdbPopularityUpdate() {
//        log.info("=== 스케줄된 TMDB 인기도 업데이트 시작 ===");
//
//        tmdbPopularityService.updateAllMoviePopularity();
//
//        log.info("=== 스케줄된 TMDB 인기도 업데이트 완료 ===");
//    }
//}
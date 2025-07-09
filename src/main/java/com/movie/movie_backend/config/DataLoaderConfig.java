package com.movie.movie_backend.config;

import com.movie.movie_backend.service.DataMigrationService;
import com.movie.movie_backend.service.KobisApiService;
import com.movie.movie_backend.service.PRDMovieListService;
import com.movie.movie_backend.service.TmdbPosterBatchService;
import com.movie.movie_backend.service.TmdbStillcutService;
import com.movie.movie_backend.service.TagDataService;
import com.movie.movie_backend.service.BoxOfficeService;
import com.movie.movie_backend.service.NaverMovieBatchService;
import com.movie.movie_backend.service.TmdbPopularMovieService;
import com.movie.movie_backend.service.KobisPopularMovieService;
import com.movie.movie_backend.repository.PRDMovieListRepository;
import com.movie.movie_backend.repository.PRDMovieRepository;
import com.movie.movie_backend.repository.BoxOfficeRepository;
import com.movie.movie_backend.repository.PRDDirectorRepository;
import com.movie.movie_backend.entity.MovieList;
import com.movie.movie_backend.entity.MovieDetail;
import com.movie.movie_backend.entity.Director;
import com.movie.movie_backend.entity.Stillcut;
import com.movie.movie_backend.dto.MovieListDto;
import com.movie.movie_backend.constant.MovieStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;
import java.util.ArrayList;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class DataLoaderConfig {

    private final DataMigrationService dataMigrationService;
    private final KobisApiService kobisApiService;
    private final PRDMovieListService movieListService;
    private final TmdbPosterBatchService tmdbPosterBatchService;
    private final TmdbStillcutService tmdbStillcutService;
    private final TagDataService tagDataService;
    private final BoxOfficeService boxOfficeService;
    private final NaverMovieBatchService naverMovieBatchService;
    private final TmdbPopularMovieService tmdbPopularMovieService;
    private final KobisPopularMovieService kobisPopularMovieService;
    private final PRDMovieListRepository prdMovieListRepository;
    private final PRDDirectorRepository directorRepository;
    private final PRDMovieRepository movieRepository;
    private final BoxOfficeRepository boxOfficeRepository;
    @Value("${kmdb.api.key}")
    private String kmdbApiKey;

    // @Bean
    // public CommandLineRunner loadMovieDescriptionsOnly() {
    //     return args -> {
    //         // 영화 줄거리(Description) 및 관람등급, 제작국가명 등 채워넣기
    //         fillMissingMovieDetails();
    //     };
    // }

    // @Bean
    // public CommandLineRunner loadStillcutsOnly() {
    //     return args -> {
    //         // 스틸컷만 업데이트
    //         log.info("=== Stillcut 업데이트 시작 ===");
    //         try {
    //             // 각 영화별로 개별 트랜잭션 처리
    //             List<MovieList> movieLists = prdMovieListRepository.findAll();
    //             int totalMovies = movieLists.size();
    //             int processed = 0;
    //             int success = 0;
    //             int failed = 0;
    //             
    //             log.info("총 {}개의 영화에 대해 스틸컷을 업데이트합니다.", totalMovies);
    //             
    //             for (MovieList movieList : movieLists) {
    //                 processed++;
    //                 try {
    //                     log.info("스틸컷 처리 중: {}/{} - {} ({})", 
    //                         processed, totalMovies, movieList.getMovieNm(), movieList.getMovieCd());
    //                     
    //                     // 개별 영화에 대해 스틸컷 처리
    //                     List<Stillcut> stillcuts = tmdbStillcutService.fetchAndSaveStillcuts(movieList.getMovieCd());
    //                     
    //                     if (!stillcuts.isEmpty()) {
    //                         success++;
    //                         log.info("스틸컷 처리 성공: {} -> {}개", movieList.getMovieNm(), stillcuts.size());
    //                     } else {
    //                         failed++;
    //                         log.warn("스틸컷 처리 실패: {} - 스틸컷을 찾을 수 없음", movieList.getMovieNm());
    //                     }
    //                     
    //                     // API 호출 제한을 위한 딜레이
    //                     Thread.sleep(100);
    //                     
    //                 } catch (Exception e) {
    //                     failed++;
    //                     log.error("스틸컷 처리 오류: {} ({}) - {}", 
    //                         movieList.getMovieNm(), movieList.getMovieCd(), e.getMessage());
    //                 }
    //             }
    //             
    //             log.info("=== Stillcut 업데이트 완료 ===");
    //             log.info("처리 결과: 총 {}개, 성공 {}개, 실패 {}개", totalMovies, success, failed);
    //             
    //         } catch (Exception e) {
    //             log.error("Stillcut 업데이트 실패", e);
    //         }
    //     };
    // }

    // @Bean
    // public CommandLineRunner loadKobisMovies() {
    //     return args -> {
    //         log.info("=== KOBIS 영화 데이터 로드 시작 ===");
    //         try {
    //             // 1. 매출액 기준 인기영화 700개 가져오기 (비활성화)
    //             // log.info("인기영화 700개 가져오기 시작...");
    //             // kobisPopularMovieService.getPopularMoviesBySales(700);
    //             // log.info("인기영화 700개 가져오기 완료");
    //             
    //             // 2. 최신영화 가져오기 (비활성화)
    //             // log.info("최신영화 300개 가져오기 시작...");
    //             // kobisPopularMovieService.getRecentMoviesByOpenDate(300);
    //             // log.info("최신영화 300개 가져오기 완료");
    //             
    //             // 3. 박스오피스 데이터 가져오기 (비활성화)
    //             // log.info("박스오피스 데이터 가져오기 시작...");
    //             // boxOfficeService.fetchDailyBoxOffice();
    //             // log.info("일일 박스오피스 가져오기 완료");
    //             // boxOfficeService.fetchWeeklyBoxOffice();
    //             // log.info("주간 박스오피스 가져오기 완료");
    //             
    //             // 4. 영화 상세정보 채워넣기 (비활성화)
    //             // log.info("영화 상세정보 채워넣기 시작...");
    //             // fillMissingMovieDetails();
    //             // log.info("영화 상세정보 채워넣기 완료");
    //             
    //             log.info("=== KOBIS 영화 데이터 로드 완료 ===");
    //         } catch (Exception e) {
    //             log.error("KOBIS 영화 데이터 로드 실패", e);
    //         }
    //     };
    // }

    /**
     * 감독 정보 저장
     */
    private Director saveDirector(String directorName) {
        // 기존 감독이 있는지 확인
        java.util.Optional<Director> existingDirector = 
            dataMigrationService.getAllMovieDetails().stream()
                .filter(movie -> movie.getDirector() != null && movie.getDirector().getName().equals(directorName))
                .map(MovieDetail::getDirector)
                .findFirst();
        
        if (existingDirector.isPresent()) {
            return existingDirector.get();
        }

        // 새 감독 생성
        Director director = Director.builder()
                .name(directorName)
                .build();
        
        return directorRepository.save(director);
    }

    /**
     * MovieDetail 채워넣기 (누락된 것 + description이 비어있는 것)
     */
    private void fillMissingMovieDetails() {
        try {
            log.info("=== MovieDetail 채워넣기 시작 ===");
            
            // 기존 데이터 개수 확인
            long existingMovieListCount = prdMovieListRepository.count();
            long existingMovieDetailCount = movieRepository.count();
            log.info("기존 데이터 확인 - MovieList: {}개, MovieDetail: {}개", 
                existingMovieListCount, existingMovieDetailCount);
            
            // 1. 누락된 MovieDetail 찾기
            List<String> missingMovieCds = new ArrayList<>();
            // 2. description이 비어있는 MovieDetail 찾기
            List<String> emptyDescriptionMovieCds = new ArrayList<>();
            
            List<MovieList> allMovieLists = prdMovieListRepository.findAll();
            
            for (MovieList movieList : allMovieLists) {
                String movieCd = movieList.getMovieCd();
                
                // MovieDetail이 없는 경우
                if (!movieRepository.existsByMovieCd(movieCd)) {
                    missingMovieCds.add(movieCd);
                } else {
                    // MovieDetail이 있지만 description이 비어있는 경우
                    MovieDetail existingDetail = movieRepository.findByMovieCd(movieCd).orElse(null);
                    if (existingDetail != null && 
                        (existingDetail.getDescription() == null || 
                         existingDetail.getDescription().trim().isEmpty())) {
                        emptyDescriptionMovieCds.add(movieCd);
                        log.info("description이 비어있는 영화 발견: {} ({})", movieList.getMovieNm(), movieCd);
                    }
                }
            }
            
            log.info("누락된 MovieDetail: {}개", missingMovieCds.size());
            log.info("description이 비어있는 MovieDetail: {}개", emptyDescriptionMovieCds.size());
            
            // 처리할 영화 목록 합치기
            List<String> allMovieCdsToProcess = new ArrayList<>();
            allMovieCdsToProcess.addAll(missingMovieCds);
            allMovieCdsToProcess.addAll(emptyDescriptionMovieCds);
            
            if (!allMovieCdsToProcess.isEmpty()) {
                log.info("총 {}개의 영화에 대해 줄거리를 채워넣습니다. (각 영화당 최대 3번 시도)", allMovieCdsToProcess.size());
                
                int successCount = 0;
                int failCount = 0;
                int totalAttempts = 0;
                final int MAX_RETRIES = 3;
                
                for (String movieCd : allMovieCdsToProcess) {
                    MovieList movieList = prdMovieListRepository.findById(movieCd).orElse(null);
                    if (movieList == null) {
                        log.warn("MovieList를 찾을 수 없음: {}", movieCd);
                        failCount++;
                        continue;
                    }
                    
                    boolean isMissing = missingMovieCds.contains(movieCd);
                    boolean isEmptyDescription = emptyDescriptionMovieCds.contains(movieCd);
                    
                    log.info("줄거리 채워넣기 시작: {} ({}) [누락: {}, 빈설명: {}]", 
                        movieList.getMovieNm(), movieCd, isMissing, isEmptyDescription);
                    
                    boolean success = false;
                    int retryCount = 0;
                    
                    // 최대 3번까지 재시도
                    while (!success && retryCount < MAX_RETRIES) {
                        retryCount++;
                        totalAttempts++;
                        
                        try {
                            log.info("시도 {}/{}: {} ({})", retryCount, MAX_RETRIES, movieList.getMovieNm(), movieCd);
                            
                            // KOBIS API로 MovieDetail 가져오기 (Actor, Cast 포함)
                            MovieDetail movieDetail = kobisPopularMovieService.getMovieDetailWithFallback(movieCd, movieList.getMovieNm());
                            
                            if (movieDetail != null && 
                                movieDetail.getDescription() != null && 
                                !movieDetail.getDescription().trim().isEmpty()) {
                                success = true;
                                successCount++;
                                log.info("줄거리 채워넣기 성공: {} ({}) - 시도 {}회, description 길이: {}", 
                                    movieList.getMovieNm(), movieCd, retryCount, 
                                    movieDetail.getDescription().length());
                            } else {
                                log.warn("시도 {}/{} 실패: {} ({}) - description이 비어있음", 
                                    retryCount, MAX_RETRIES, movieList.getMovieNm(), movieCd);
                            }
                            
                        } catch (Exception e) {
                            log.error("시도 {}/{} 실패: {} ({}) - {}", 
                                retryCount, MAX_RETRIES, movieList.getMovieNm(), movieCd, e.getMessage());
                        }
                        
                        // 재시도 전 딜레이 (마지막 시도가 아니면)
                        if (!success && retryCount < MAX_RETRIES) {
                            try {
                                Thread.sleep(500); // 재시도 간 0.5초 딜레이
                            } catch (InterruptedException ie) {
                                Thread.currentThread().interrupt();
                                break;
                            }
                        }
                    }
                    
                    if (!success) {
                        failCount++;
                        log.warn("최종 실패: {} ({}) - {}회 시도 후에도 성공하지 못함", 
                            movieList.getMovieNm(), movieCd, MAX_RETRIES);
                    }
                    
                    // API 호출 제한을 위한 딜레이
                    Thread.sleep(200);
                }
                
                log.info("줄거리 채워넣기 완료: 성공={}, 실패={}, 총시도={}", successCount, failCount, totalAttempts);
                
                // 업데이트 후 개수 재확인
                long updatedMovieDetailCount = movieRepository.count();
                log.info("업데이트 후 MovieDetail 개수: {}개", updatedMovieDetailCount);
                
            } else {
                log.info("줄거리를 채워넣을 영화가 없습니다. 모든 MovieDetail이 완성되어 있습니다.");
            }
            
            // Stillcut 업데이트 추가
            log.info("=== Stillcut 업데이트 시작 ===");
            try {
                tmdbStillcutService.updateStillcutsForAllMovies();
                log.info("=== Stillcut 업데이트 완료 ===");
            } catch (Exception e) {
                log.error("Stillcut 업데이트 실패", e);
            }
            
            log.info("=== MovieDetail 채워넣기 완료 ===");
            
        } catch (Exception e) {
            log.error("MovieDetail 채워넣기 실패", e);
        }
    }

    @Bean
    public CommandLineRunner autoMapMovieDetailTags(TagDataService tagDataService) {
        return args -> {
            log.info("=== TagDataService.setupTagData() 자동 실행 ===");
            tagDataService.setupTagData();
        };
    }

    // 제작국가명/관람등급명만 빠르게 업데이트하는 Bean
    // @Bean
    // public CommandLineRunner updateNationAndGradeOnly() {
    //     return args -> {
    //         log.info("=== 제작국가명/관람등급명만 업데이트 시작 ===");
    //         List<MovieList> movieLists = prdMovieListRepository.findAll();
    //         int total = movieLists.size();
    //         int updated = 0, failed = 0;
    //         for (MovieList movieList : movieLists) {
    //             try {
    //                 String movieCd = movieList.getMovieCd();
    //                 var result = kobisApiService.fetchNationAndGrade(movieCd);
    //                 if (result != null) {
    //                     MovieDetail detail = movieRepository.findByMovieCd(movieCd).orElse(null);
    //                     if (detail == null) {
    //                         detail = MovieDetail.builder()
    //                                 .movieCd(movieCd)
    //                                 .movieNm(movieList.getMovieNm())
    //                                 .build();
    //                     }
    //                     detail.setNationNm(result.nationNm);
    //                     detail.setWatchGradeNm(result.watchGradeNm);
    //                     movieRepository.save(detail);
    //                     updated++;
    //                     log.info("[{} / {}] {} ({}) - 국가/등급 저장 완료: {}, {}", updated, total, movieList.getMovieNm(), movieCd, result.nationNm, result.watchGradeNm);
    //                 }
    //             } catch (Exception e) {
    //                 failed++;
    //                 log.warn("국가/등급 저장 실패: {} ({}) - {}", movieList.getMovieNm(), movieList.getMovieCd(), e.getMessage());
    //             }
    //         }
    //         log.info("=== 제작국가명/관람등급명만 업데이트 완료: 성공 {}, 실패 {} ===", updated, failed);
    //     };
    // }

    // @Bean
    // public CommandLineRunner matchAndSaveKmdbIds() {
    //     return args -> {
    //         log.info("=== KMDb ID 매칭 및 저장 배치 시작 ===");
    //         
    //         // KMDb API 테스트 먼저 실행
    //         log.info("=== KMDb API 테스트 시작 ===");
    //         testKmdbApi();
    //         log.info("=== KMDb API 테스트 완료 ===");
    //         
    //         // 1차 매칭 실행
    //         performInitialMatching();
    //         
    //         // 2차 매칭 실행 (실패한 영화들에 대해 추가 전략 적용)
    //         performSecondaryMatching();
    //         
    //         // 배치 크기 설정
    //         final int BATCH_SIZE = 10;
    //         final int MAX_CONCURRENT_REQUESTS = 3;
    //         
    //         List<MovieList> movieLists = prdMovieListRepository.findAll();
    //         List<MovieList> unmappedMovies = movieLists.stream()
    //             .filter(movie -> !StringUtils.hasText(movie.getKmdbId()))
    //             .collect(java.util.stream.Collectors.toList());
    //         
    //         log.info("총 {}개 영화 중 {}개가 매칭 필요", movieLists.size(), unmappedMovies.size());
    //         
    //         // 디버깅: 매칭이 필요한 영화들의 제목 확인
    //         if (unmappedMovies.size() > 0) {
    //             log.info("매칭 필요한 영화 샘플 (처음 5개):");
    //             unmappedMovies.stream().limit(5).forEach(movie -> 
    //                 log.info("  - {} ({})", movie.getMovieNm(), movie.getMovieCd()));
    //         }
    //         
    //         if (unmappedMovies.isEmpty()) {
    //             log.info("모든 영화가 이미 매칭되어 있습니다.");
    //             return;
    //         }
    //         
    //         int updated = 0, failed = 0, skipped = movieLists.size() - unmappedMovies.size();
    //         
    //         // 배치 단위로 처리
    //         for (int i = 0; i < unmappedMovies.size(); i += BATCH_SIZE) {
    //             int endIndex = Math.min(i + BATCH_SIZE, unmappedMovies.size());
    //             List<MovieList> batch = unmappedMovies.subList(i, endIndex);
    //             
    //             log.info("배치 처리 중: {}/{} ({}-{})", 
    //                 (i / BATCH_SIZE) + 1, 
    //                 (unmappedMovies.size() + BATCH_SIZE - 1) / BATCH_SIZE,
    //                 i + 1, endIndex);
    //             
    //             // 배치 내에서 병렬 처리 (최대 3개 동시 요청)
    //             java.util.concurrent.ExecutorService executor = 
    //                 java.util.concurrent.Executors.newFixedThreadPool(MAX_CONCURRENT_REQUESTS);
    //             
    //             try {
    //                 java.util.List<java.util.concurrent.Future<MovieMappingResult>> futures = new ArrayList<>();
    //                 
    //                 for (MovieList movie : batch) {
    //                         futures.add(executor.submit(() -> processMovie(movie)));
    //                     }
    //                     
    //                     // 결과 수집
    //                     for (java.util.concurrent.Future<MovieMappingResult> future : futures) {
    //                             try {
    //                                 MovieMappingResult result = future.get(30, java.util.concurrent.TimeUnit.SECONDS);
    //                                 if (result.success) {
    //                                     updated++;
    //                                     log.info("매칭 성공: {} ({}) -> kmdbId={}", 
    //                                         result.movieNm, result.movieCd, result.kmdbId);
    //                                 } else {
    //                                     failed++;
    //                                     // 실패한 영화들의 상세 로깅 (처음 20개)
    //                                     if (failed <= 20) {
    //                                         log.warn("KMDb 매칭 실패: {} ({}) - 검색어: {}", result.movieNm, result.movieCd, result.movieNm);
    //                                     } else if (failed % 50 == 0) {
    //                                         log.warn("KMDb 매칭 실패: {} ({})", result.movieNm, result.movieCd);
    //                                     }
    //                                 }
    //                             } catch (Exception e) {
    //                                 failed++;
    //                                 log.error("영화 처리 중 오류 발생: {}", e.getMessage());
    //                             }
    //                         }
    //                         
    //                     } finally {
    //                         executor.shutdown();
    //                         try {
    //                             if (!executor.awaitTermination(60, java.util.concurrent.TimeUnit.SECONDS)) {
    //                                 executor.shutdownNow();
    //                             }
    //                         } catch (InterruptedException e) {
    //                             executor.shutdownNow();
    //                             Thread.currentThread().interrupt();
    //                         }
    //                     }
    //                     
    //                     // 배치 간 딜레이 (API 쿼터 보호)
    //                     if (i + BATCH_SIZE < unmappedMovies.size()) {
    //                         Thread.sleep(100);
    //                     }
    //                 }
    //                 
    //                 log.info("=== 1차 KMDb ID 매칭 완료: 성공 {}, 실패 {}, 스킵 {} ===", updated, failed, skipped);
    //             };
    //         }
    
    // 1차 매칭 실행 메서드
    private void performInitialMatching() {
        log.info("=== 1차 KMDb ID 매칭 시작 ===");
        
        // 배치 크기 설정
        final int BATCH_SIZE = 10;
        final int MAX_CONCURRENT_REQUESTS = 3;
        
        List<MovieList> movieLists = prdMovieListRepository.findAll();
        List<MovieList> unmappedMovies = movieLists.stream()
            .filter(movie -> !StringUtils.hasText(movie.getKmdbId()))
            .collect(java.util.stream.Collectors.toList());
        
        log.info("총 {}개 영화 중 {}개가 매칭 필요", movieLists.size(), unmappedMovies.size());
        
        if (unmappedMovies.isEmpty()) {
            log.info("모든 영화가 이미 매칭되어 있습니다.");
            return;
        }
        
        int updated = 0, failed = 0, skipped = movieLists.size() - unmappedMovies.size();
        
        // 배치 단위로 처리
        for (int i = 0; i < unmappedMovies.size(); i += BATCH_SIZE) {
            int endIndex = Math.min(i + BATCH_SIZE, unmappedMovies.size());
            List<MovieList> batch = unmappedMovies.subList(i, endIndex);
            
            log.info("1차 배치 처리 중: {}/{} ({}-{})", 
                (i / BATCH_SIZE) + 1, 
                (unmappedMovies.size() + BATCH_SIZE - 1) / BATCH_SIZE,
                i + 1, endIndex);
            
            // 배치 내에서 병렬 처리
            java.util.concurrent.ExecutorService executor = 
                java.util.concurrent.Executors.newFixedThreadPool(MAX_CONCURRENT_REQUESTS);
            
            try {
                java.util.List<java.util.concurrent.Future<MovieMappingResult>> futures = new ArrayList<>();
                
                for (MovieList movie : batch) {
                    futures.add(executor.submit(() -> processMovie(movie)));
                }
                
                // 결과 수집
                for (java.util.concurrent.Future<MovieMappingResult> future : futures) {
                    try {
                        MovieMappingResult result = future.get(30, java.util.concurrent.TimeUnit.SECONDS);
                        if (result.success) {
                            updated++;
                            log.info("1차 매칭 성공: {} ({}) -> kmdbId={}", 
                                result.movieNm, result.movieCd, result.kmdbId);
                        } else {
                            failed++;
                            if (failed <= 10) {
                                log.warn("1차 매칭 실패: {} ({})", result.movieNm, result.movieCd);
                            }
                        }
                    } catch (Exception e) {
                        failed++;
                        log.error("영화 처리 중 오류 발생: {}", e.getMessage());
                    }
                }
                
            } finally {
                executor.shutdown();
                try {
                    if (!executor.awaitTermination(60, java.util.concurrent.TimeUnit.SECONDS)) {
                        executor.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    executor.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }
            
            // 배치 간 딜레이
            if (i + BATCH_SIZE < unmappedMovies.size()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("1차 매칭 sleep 중단: {}", e.getMessage());
                    break;
                }
            }
        }
        
        log.info("=== 1차 KMDb ID 매칭 완료: 성공 {}, 실패 {} ===", updated, failed);
    }
    
    // 2차 매칭 실행 메서드 (추가 검색 전략 적용)
    private void performSecondaryMatching() {
        log.info("=== 2차 KMDb ID 매칭 시작 (추가 전략) ===");
        
        List<MovieList> movieLists = prdMovieListRepository.findAll();
        List<MovieList> unmappedMovies = movieLists.stream()
            .filter(movie -> !StringUtils.hasText(movie.getKmdbId()))
            .collect(java.util.stream.Collectors.toList());
        
        log.info("2차 매칭 대상: {}개 영화", unmappedMovies.size());
        
        if (unmappedMovies.isEmpty()) {
            log.info("매칭할 영화가 없습니다.");
            return;
        }
        
        int updated = 0, failed = 0;
        
        for (MovieList movie : unmappedMovies) {
            try {
                MovieMappingResult result = processMovieWithAdvancedStrategy(movie);
                if (result.success) {
                    updated++;
                    log.info("2차 매칭 성공: {} ({}) -> kmdbId={}", 
                        result.movieNm, result.movieCd, result.kmdbId);
                } else {
                    failed++;
                    if (failed <= 20) {
                        log.warn("2차 매칭 실패: {} ({})", result.movieNm, result.movieCd);
                    }
                }
                
                // API 호출 제한을 위한 딜레이
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("2차 매칭 sleep 중단: {}", e.getMessage());
                    break;
                }
                
            } catch (Exception e) {
                failed++;
                log.error("2차 매칭 중 오류: {} ({}) - {}", movie.getMovieNm(), movie.getMovieCd(), e.getMessage());
            }
        }
        
        log.info("=== 2차 KMDb ID 매칭 완료: 성공 {}, 실패 {} ===", updated, failed);
    }
    
    // 영화 처리 결과를 담는 클래스
    private static class MovieMappingResult {
        final boolean success;
        final String movieCd;
        final String movieNm;
        final String kmdbId;
        
        MovieMappingResult(boolean success, String movieCd, String movieNm, String kmdbId) {
            this.success = success;
            this.movieCd = movieCd;
            this.movieNm = movieNm;
            this.kmdbId = kmdbId;
        }
    }
    
    // 개별 영화 처리 메서드
    private MovieMappingResult processMovie(MovieList movie) {
        try {
            String movieNm = movie.getMovieNm();
            String movieNmEn = movie.getMovieNmEn();
            String openDt = movie.getOpenDt() != null ? movie.getOpenDt().toString() : null;
            String directorName = null;
            
            // 검색어 정규화: 특수문자 제거 및 핵심 키워드 추출
            String normalizedTitle = normalizeSearchTitle(movieNm);
            log.debug("원본 제목: {}, 정규화된 제목: {}", movieNm, normalizedTitle);
            
            // 감독 정보 가져오기
            MovieDetail detail = movieRepository.findByMovieCd(movie.getMovieCd()).orElse(null);
            if (detail != null && detail.getDirector() != null) {
                try {
                    directorName = detail.getDirector().getName();
                } catch (Exception e) {
                    log.debug("감독 정보 로드 실패: {} ({}) - {}", movie.getMovieNm(), movie.getMovieCd(), e.getMessage());
                }
            }
            
            RestTemplate restTemplate = new RestTemplate();
            String kmdbId = null;
            
            // 1. 정규화된 한글명으로 검색
            if (StringUtils.hasText(normalizedTitle)) {
                kmdbId = searchKmdbIdFromKmdbApi(normalizedTitle, openDt, directorName, restTemplate);
            }
            // 2. 원본 한글명으로 검색 (실패시)
            if (kmdbId == null && StringUtils.hasText(movieNm)) {
                kmdbId = searchKmdbIdFromKmdbApi(movieNm, openDt, directorName, restTemplate);
            }
            // 3. 영문명으로 검색 (실패시)
            if (kmdbId == null && StringUtils.hasText(movieNmEn)) {
                kmdbId = searchKmdbIdFromKmdbApi(movieNmEn, openDt, directorName, restTemplate);
            }
                    // 4. 핵심 키워드만 추출해서 검색 (실패시)
                    if (kmdbId == null && StringUtils.hasText(movieNm)) {
                        String[] words = movieNm.split("\\s+");
                        if (words.length > 2) {
                            // 첫 2-3개 단어만 사용
                            int wordCount = Math.min(3, words.length);
                            StringBuilder keywordTitle = new StringBuilder();
                            for (int i = 0; i < wordCount; i++) {
                                if (i > 0) keywordTitle.append(" ");
                                keywordTitle.append(words[i]);
                            }
                            String shortTitle = keywordTitle.toString();
                            if (shortTitle.length() > 2) {
                                kmdbId = searchKmdbIdFromKmdbApi(shortTitle, openDt, directorName, restTemplate);
                            }
                        }
                    }
            
            if (kmdbId != null) {
                // 트랜잭션 내에서 저장
                movie.setKmdbId(kmdbId);
                prdMovieListRepository.save(movie);
                return new MovieMappingResult(true, movie.getMovieCd(), movieNm, kmdbId);
            } else {
                return new MovieMappingResult(false, movie.getMovieCd(), movieNm, null);
            }
            
        } catch (Exception e) {
            log.error("영화 처리 중 오류 발생: {} ({}) - {}", movie.getMovieNm(), movie.getMovieCd(), e.getMessage());
            return new MovieMappingResult(false, movie.getMovieCd(), movie.getMovieNm(), null);
        }
    }

    // KMDb 검색 결과가 여러 개일 때 감독명까지 일치하는 영화 우선 선택
    private String searchKmdbIdFromKmdbApi(String movieNm, String openDt, String directorName, RestTemplate restTemplate) {
        try {
            if (!StringUtils.hasText(movieNm)) return null;
            
            log.debug("KMDb 검색 시작: 제목={}, 개봉일={}, 감독={}", movieNm, openDt, directorName);
            
            // 1. 먼저 제목만으로 검색 (연도 없이)
            String encodedTitle = java.net.URLEncoder.encode(movieNm, "UTF-8");
            String url = String.format(
                "https://api.koreafilm.or.kr/openapi-data2/wisenut/search_api/search_json2.jsp?collection=kmdb_new2&title=%s&ServiceKey=%s&listCount=10&sort=prodYear,1",
                encodedTitle,
                kmdbApiKey
            );
            
            // 만약 UTF-8 인코딩이 안 되면 EUC-KR로 시도
            if (encodedTitle.contains("%")) {
                try {
                    String eucKrEncoded = new String(movieNm.getBytes("EUC-KR"), "ISO-8859-1");
                    url = String.format(
                        "https://api.koreafilm.or.kr/openapi-data2/wisenut/search_api/search_json2.jsp?collection=kmdb_new2&title=%s&ServiceKey=%s&listCount=10&sort=prodYear,1",
                        eucKrEncoded,
                        kmdbApiKey
                    );
                    log.debug("EUC-KR 인코딩 사용: {}", eucKrEncoded);
                } catch (Exception e) {
                    log.debug("EUC-KR 인코딩 실패, UTF-8 사용");
                }
            }
            
            log.debug("KMDb API URL: {}", url);
            
            // 만약 첫 번째 검색이 실패하면 다른 검색 방법 시도
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String responseBody = response.getBody();
                JSONObject json = new JSONObject(responseBody);
                
                // TotalCount가 0이면 다른 검색 방법 시도
                if (json.has("TotalCount") && json.getInt("TotalCount") == 0) {
                    log.debug("제목 검색 실패, 다른 검색 방법 시도: {}", movieNm);
                    
                    // 1. 키워드 검색으로 재시도
                    String keywordUrl = String.format(
                        "https://api.koreafilm.or.kr/openapi-data2/wisenut/search_api/search_json2.jsp?collection=kmdb_new2&keyword=%s&ServiceKey=%s&listCount=10&sort=prodYear,1",
                        encodedTitle,
                        kmdbApiKey
                    );
                    
                    log.debug("KMDb 키워드 검색 URL: {}", keywordUrl);
                    response = restTemplate.getForEntity(keywordUrl, String.class);
                    
                    if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                        responseBody = response.getBody();
                        json = new JSONObject(responseBody);
                    }
                    
                    // 2. 여전히 실패하면 감독명으로 검색 시도
                    if (json.has("TotalCount") && json.getInt("TotalCount") == 0 && StringUtils.hasText(directorName)) {
                        log.debug("키워드 검색 실패, 감독명 검색 시도: {}", directorName);
                        
                        String directorUrl = String.format(
                            "https://api.koreafilm.or.kr/openapi-data2/wisenut/search_api/search_json2.jsp?collection=kmdb_new2&director=%s&ServiceKey=%s&listCount=10&sort=prodYear,1",
                            java.net.URLEncoder.encode(directorName, "UTF-8"),
                            kmdbApiKey
                        );
                        
                        log.debug("KMDb 감독명 검색 URL: {}", directorUrl);
                        response = restTemplate.getForEntity(directorUrl, String.class);
                        
                        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                            responseBody = response.getBody();
                            json = new JSONObject(responseBody);
                        }
                    }
                }
            }
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String responseBody = response.getBody();
                
                // 디버깅을 위해 첫 번째 실패한 영화의 응답을 로깅
                if (movieNm.contains("극장판 다이노맨") || movieNm.contains("망국전쟁")) {
                    log.info("KMDb API 응답 (디버깅): {} -> {}", movieNm, responseBody.substring(0, Math.min(1000, responseBody.length())));
                }
                
                JSONObject json = new JSONObject(responseBody);
                
                // KMDb API 응답 구조 확인 (실제 응답 구조에 맞게 수정)
                JSONArray movies = null;
                
                // 실제 응답 구조: Data[0].Result
                if (json.has("Data")) {
                    JSONArray data = json.getJSONArray("Data");
                    if (data.length() > 0) {
                        JSONObject firstData = data.getJSONObject(0);
                        if (firstData.has("Result")) {
                            movies = firstData.getJSONArray("Result");
                        }
                    }
                }
                
                if (movies != null && movies.length() > 0) {
                    // TotalCount 체크 (최상위 레벨에서 확인)
                    if (json.has("TotalCount") && json.getInt("TotalCount") == 0) {
                        log.debug("KMDb 검색 결과 없음 (TotalCount=0): {}", movieNm);
                        return null;
                    }
                    
                    log.debug("KMDb 검색 성공: {} -> {}개 결과 발견", movieNm, movies.length());
                    
                    // 첫 번째 영화 정보 가져오기
                    JSONObject firstMovie = movies.getJSONObject(0);
                    
                    // 디버깅: 첫 번째 결과의 모든 필드 확인
                    log.info("KMDb 첫 번째 결과 전체 구조: {}", firstMovie.toString());
                    
                    // 실제 응답에서 DOCID 필드 사용 (KMDb 고유 ID)
                    String docId = firstMovie.optString("DOCID", null);
                    
                    log.info("KMDb ID 필드 확인: DOCID={}, movieId={}, movieSeq={}", 
                        docId,
                        firstMovie.optString("movieId", "null"),
                        firstMovie.optString("movieSeq", "null"));
                    
                    // 1. 감독명까지 일치하는 영화 우선 선택
                    if (StringUtils.hasText(directorName)) {
                        for (int i = 0; i < movies.length(); i++) {
                            JSONObject movieObj = movies.getJSONObject(i);
                            
                            // 감독 정보 파싱 (directors.director 배열에서 감독명 추출)
                            String kmdbDirectorName = null;
                            if (movieObj.has("directors")) {
                                JSONObject directors = movieObj.getJSONObject("directors");
                                if (directors.has("director")) {
                                    JSONArray directorArray = directors.getJSONArray("director");
                                    if (directorArray.length() > 0) {
                                        JSONObject director = directorArray.getJSONObject(0);
                                        kmdbDirectorName = director.optString("directorNm", "");
                                    }
                                }
                            }
                            
                            if (StringUtils.hasText(kmdbDirectorName) && kmdbDirectorName.contains(directorName)) {
                                String finalDocId = movieObj.optString("DOCID", null);
                                log.info("감독명 일치 발견: {} -> DOCID={}", movieNm, finalDocId);
                                return finalDocId;
                            }
                        }
                    }
                    
                    // 2. 감독명 일치 영화가 없으면 첫 번째 결과 반환
                    log.info("첫 번째 결과 선택: {} -> DOCID={}", movieNm, docId);
                    return docId;
                } else {
                    log.debug("KMDb 검색 결과 없음: {}", movieNm);
                }
                
                // 검색 결과 없음
            } else {
                log.warn("KMDb API 호출 실패: status={}, movieNm={}", response.getStatusCode(), movieNm);
            }
            
        } catch (Exception e) {
            log.warn("KMDb API 호출/파싱 실패: {} ({}) - {}", movieNm, openDt, e.getMessage());
        }
        return null;
    }
    
    // KMDb API 테스트 메서드
    private void testKmdbApi() {
        try {
            log.info("KMDb API 키 확인: {}", kmdbApiKey != null ? kmdbApiKey.substring(0, Math.min(10, kmdbApiKey.length())) + "..." : "null");
            
            RestTemplate restTemplate = new RestTemplate();
            
            // 잘 알려진 영화로 테스트
            String testMovies[] = {"기생충", "올드보이", "부산행"};
            
            for (String testMovie : testMovies) {
                String url = String.format(
                    "https://api.koreafilm.or.kr/openapi-data2/wisenut/search_api/search_json2.jsp?collection=kmdb_new2&title=%s&ServiceKey=%s&listCount=5",
                    java.net.URLEncoder.encode(testMovie, "UTF-8"),
                    kmdbApiKey
                );
                
                log.info("KMDb API 테스트: {} -> {}", testMovie, url);
                
                ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    String responseBody = response.getBody();
                    log.info("KMDb API 테스트 응답 ({}): {}", testMovie, responseBody.substring(0, Math.min(1000, responseBody.length())));
                    
                    // 응답 구조 분석
                    try {
                        JSONObject json = new JSONObject(responseBody);
                        if (json.has("TotalCount")) {
                            log.info("KMDb API 테스트 - TotalCount: {}", json.getInt("TotalCount"));
                        }
                        if (json.has("Data")) {
                            JSONArray data = json.getJSONArray("Data");
                            if (data.length() > 0) {
                                JSONObject firstData = data.getJSONObject(0);
                                if (firstData.has("Result")) {
                                    JSONArray result = firstData.getJSONArray("Result");
                                    log.info("KMDb API 테스트 - Result 개수: {}", result.length());
                                    if (result.length() > 0) {
                                        JSONObject firstMovie = result.getJSONObject(0);
                                        log.info("KMDb API 테스트 - 첫 번째 영화 DOCID: {}", firstMovie.optString("DOCID", "null"));
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        log.warn("KMDb API 테스트 응답 파싱 실패: {}", e.getMessage());
                    }
                } else {
                    log.warn("KMDb API 테스트 실패: {} - status={}", testMovie, response.getStatusCode());
                }
                
                Thread.sleep(100);
            }
            
            log.info("=== KMDb API 테스트 완료 ===");
            
        } catch (Exception e) {
            log.error("KMDb API 테스트 중 오류 발생", e);
        }
    }
    
    // 검색어 정규화 메서드
    private String normalizeSearchTitle(String title) {
        if (!StringUtils.hasText(title)) {
            return title;
        }
        
        String normalized = title;
        
        // 1. 콜론(:) 이후 부분 제거
        if (normalized.contains(":")) {
            normalized = normalized.split(":")[0].trim();
        }
        
        // 2. 괄호 부분 제거
        normalized = normalized.replaceAll("[\\(（].*?[\\)）]", "").trim();
        
        // 3. 특정 접미사 제거
        normalized = normalized
            .replaceAll("극장판\\s*", "")
            .replaceAll("시리즈\\s*", "")
            .replaceAll("\\s+", " ")
            .trim();
        
        // 4. 특수문자 제거 (하이픈, 점 등)
        normalized = normalized
            .replaceAll("[-\\s\\.]+", " ")
            .trim();
        
        // 5. 너무 짧으면 원본 반환
        if (normalized.length() < 2) {
            return title;
        }
        
        return normalized;
    }
    
    // 고급 검색 전략을 사용한 영화 처리 메서드
    private MovieMappingResult processMovieWithAdvancedStrategy(MovieList movie) {
        try {
            String movieNm = movie.getMovieNm();
            String movieNmEn = movie.getMovieNmEn();
            String openDt = movie.getOpenDt() != null ? movie.getOpenDt().toString() : null;
            String directorName = null;
            
            // 감독 정보 가져오기
            MovieDetail detail = movieRepository.findByMovieCd(movie.getMovieCd()).orElse(null);
            if (detail != null && detail.getDirector() != null) {
                try {
                    directorName = detail.getDirector().getName();
                } catch (Exception e) {
                    log.debug("감독 정보 로드 실패: {} ({}) - {}", movie.getMovieNm(), movie.getMovieCd(), e.getMessage());
                }
            }
            
            RestTemplate restTemplate = new RestTemplate();
            String kmdbId = null;
            
            // 고급 검색 전략들
            String[] searchStrategies = {
                // 1. 영문명으로 검색 (한글 검색이 안 될 수 있음)
                movieNmEn,
                // 2. 제목에서 첫 단어만 사용
                extractFirstWord(movieNm),
                // 3. 제목에서 마지막 단어만 사용
                extractLastWord(movieNm),
                // 4. 제목에서 숫자 제거 후 검색
                removeNumbers(movieNm),
                // 5. 제목을 거꾸로 검색 (일부 영화는 부제목이 메인일 수 있음)
                reverseTitle(movieNm),
                // 6. 감독명 + 제목 조합
                directorName != null ? directorName + " " + extractFirstWord(movieNm) : null
            };
            
            for (String searchTerm : searchStrategies) {
                if (StringUtils.hasText(searchTerm) && searchTerm.length() > 1) {
                    log.debug("고급 검색 시도: {} -> {}", movieNm, searchTerm);
                    kmdbId = searchKmdbIdFromKmdbApi(searchTerm, openDt, directorName, restTemplate);
                    if (kmdbId != null) {
                        log.info("고급 검색 성공: {} -> {} (검색어: {})", movieNm, kmdbId, searchTerm);
                        break;
                    }
                }
            }
            
            if (kmdbId != null) {
                // 트랜잭션 내에서 저장
                movie.setKmdbId(kmdbId);
                prdMovieListRepository.save(movie);
                return new MovieMappingResult(true, movie.getMovieCd(), movieNm, kmdbId);
            } else {
                return new MovieMappingResult(false, movie.getMovieCd(), movieNm, null);
            }
            
        } catch (Exception e) {
            log.error("고급 검색 중 오류 발생: {} ({}) - {}", movie.getMovieNm(), movie.getMovieCd(), e.getMessage());
            return new MovieMappingResult(false, movie.getMovieCd(), movie.getMovieNm(), null);
        }
    }
    
    // 첫 번째 단어 추출
    private String extractFirstWord(String title) {
        if (!StringUtils.hasText(title)) return null;
        String[] words = title.split("\\s+");
        return words.length > 0 ? words[0] : null;
    }
    
    // 마지막 단어 추출
    private String extractLastWord(String title) {
        if (!StringUtils.hasText(title)) return null;
        String[] words = title.split("\\s+");
        return words.length > 0 ? words[words.length - 1] : null;
    }
    
    // 숫자 제거
    private String removeNumbers(String title) {
        if (!StringUtils.hasText(title)) return null;
        return title.replaceAll("\\d+", "").replaceAll("\\s+", " ").trim();
    }
    
    // 제목 뒤집기 (콜론 기준)
    private String reverseTitle(String title) {
        if (!StringUtils.hasText(title) || !title.contains(":")) return null;
        String[] parts = title.split(":");
        if (parts.length >= 2) {
            return parts[1].trim() + " " + parts[0].trim();
        }
        return null;
    }

    // @Bean
    // public CommandLineRunner updateMovieStatusFromKmdb() {
    //     return args -> {
    //         log.info("=== KMDb status 동기화 시작 ===");
    //         List<MovieList> movies = prdMovieListRepository.findByKmdbIdIsNotNull();
    //         RestTemplate restTemplate = new RestTemplate();
    //         int updated = 0, failed = 0;
    //         for (MovieList movie : movies) {
    //             try {
    //                 String kmdbId = movie.getKmdbId();
    //                 String url = String.format(
    //                     "https://api.koreafilm.or.kr/openapi-data2/wisenut/search_api/search_json2.jsp?collection=kmdb_new2&DOCID=%s&ServiceKey=%s",
    //                     kmdbId, kmdbApiKey
    //                 );
    //                 ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
    //                 String repRlsDate = null;
    //                 if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
    //                     JSONObject json = new JSONObject(response.getBody());
    //                     JSONArray dataArr = json.optJSONArray("Data");
    //                     if (dataArr != null && dataArr.length() > 0) {
    //                         JSONObject firstData = dataArr.getJSONObject(0);
    //                         JSONArray resultArr = firstData.optJSONArray("Result");
    //                         if (resultArr != null && resultArr.length() > 0) {
    //                             JSONObject movieObj = resultArr.getJSONObject(0);
    //                             repRlsDate = movieObj.optString("repRlsDate", "");
    //                         }
    //                     }
    //                 }
    //                 // repRlsDate가 없으면 openDt(내 DB) 사용
    //                 String baseDate = null;
    //                 if (repRlsDate != null && !repRlsDate.isBlank()) {
    //                     baseDate = repRlsDate.replace("-", "");
    //                 } else if (movie.getOpenDt() != null) {
    //                     baseDate = movie.getOpenDt().toString().replace("-", "");
    //                 }
    //                 String today = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE); // yyyyMMdd
    //                 MovieStatus status;
    //                 if (baseDate == null || baseDate.isBlank()) {
    //                     status = MovieStatus.COMING_SOON;
    //                 } else {
    //                     java.time.LocalDate openDate = java.time.LocalDate.parse(baseDate, java.time.format.DateTimeFormatter.BASIC_ISO_DATE);
    //                     java.time.LocalDate todayDate = java.time.LocalDate.now();
    //                     if (openDate.isAfter(todayDate)) {
    //                         status = MovieStatus.COMING_SOON;
    //                     } else if (openDate.plusDays(45).isBefore(todayDate)) {
    //                         status = MovieStatus.ENDED;
    //                     } else {
    //                         status = MovieStatus.NOW_PLAYING;
    //                     }
    //                 }
    //                 movie.setStatus(status);
    //                 prdMovieListRepository.save(movie);
    //                 updated++;
    //                 log.info("상태 업데이트: {}({}) -> {} (repRlsDate: {}, openDt: {})", movie.getMovieNm(), kmdbId, status, repRlsDate, movie.getOpenDt());
    //                 Thread.sleep(100);
    //             } catch (Exception e) {
    //                 failed++;
    //                 log.error("상태 업데이트 실패: {}({}) - {}", movie.getMovieNm(), movie.getKmdbId(), e.getMessage());
    //             }
    //         }
    //         log.info("=== KMDb status 동기화 완료: 성공 {}, 실패 {} ===", updated, failed);
    //     };
    // }
} 

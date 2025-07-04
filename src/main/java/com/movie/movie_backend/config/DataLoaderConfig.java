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
    private final PRDMovieListRepository prdMovieListRepository;
    private final PRDDirectorRepository directorRepository;
    private final KobisPopularMovieService kobisPopularMovieService;
    private final PRDMovieRepository movieRepository;
    private final BoxOfficeRepository boxOfficeRepository;

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
} 

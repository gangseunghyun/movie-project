// package com.movie.movie_backend.config;

// import com.movie.movie_backend.service.KobisPopularMovieService;
// import com.movie.movie_backend.repository.PRDMovieListRepository;
// import com.movie.movie_backend.entity.MovieList;
// import com.movie.movie_backend.dto.MovieListDto;
// import com.movie.movie_backend.mapper.MovieListMapper;
// import lombok.RequiredArgsConstructor;
// import lombok.extern.slf4j.Slf4j;
// import org.springframework.boot.CommandLineRunner;
// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Configuration;
// import org.springframework.core.annotation.Order;
// import org.springframework.scheduling.annotation.EnableScheduling;
// import org.springframework.scheduling.annotation.Scheduled;

// import java.util.List;
// import java.util.ArrayList;
// import java.util.HashSet;
// import com.movie.movie_backend.repository.MovieDetailRepository;
// import com.movie.movie_backend.service.KobisApiService;
// import com.movie.movie_backend.entity.MovieDetail;
// import java.util.Set;

// @Slf4j
// @Configuration
// @EnableScheduling
// @RequiredArgsConstructor
// public class MovieListLoader {

//     private final KobisPopularMovieService kobisPopularMovieService;
//     private final PRDMovieListRepository prdMovieListRepository;
//     private final MovieListMapper movieListMapper;
//     private final MovieDetailRepository movieRepository;
//     private final KobisApiService kobisApiService;

//     @Bean
//     @Order(2)  // BoxOffice 다음에 실행
//     public CommandLineRunner loadKobisMoviesAndDetails() {
//         return args -> {
//             log.info("=== KOBIS MovieList 로드 시작 (2025년 이후 영화) ===");
//             try {
//                 // 2025년 이후 영화만 가져오기
//                 int currentYear = 2025;
//                 log.info("KOBIS API에서 {}년 이후 영화 목록 가져오기 시작...", currentYear);
                
//                 List<MovieListDto> allMovies = new ArrayList<>();
//                 // 2025년, 2024년, 2023년 인기영화를 한 번의 API 호출로 가져오기 (API 호출 제한 고려)
//                 log.info("2025년, 2024년, 2023년 인기영화를 한 번의 API 호출로 가져오기...");
//                 try {
//                     log.info("인기영화 1100개 가져오기 (API 호출 제한 고려)...");
//                     List<MovieListDto> allPopularMovies = kobisPopularMovieService.getPopularMoviesBySales(1100);
//                     int count2025 = 0;
//                     int count2024 = 0;
//                     int count2023 = 0;
//                     for (MovieListDto dto : allPopularMovies) {
//                         if (dto.getOpenDt() != null) {
//                             if (dto.getOpenDt().getYear() == 2025) {
//                                 allMovies.add(dto);
//                                 count2025++;
//                             } else if (dto.getOpenDt().getYear() == 2024) {
//                                 allMovies.add(dto);
//                                 count2024++;
//                             } else if (dto.getOpenDt().getYear() == 2023) {
//                                 allMovies.add(dto);
//                                 count2023++;
//                             }
//                         }
//                     }
//                     log.info("2025년 인기영화 {}개, 2024년 {}개, 2023년 {}개 필터링됨", count2025, count2024, count2023);
//                 } catch (Exception e) {
//                     log.error("인기영화 가져오기 실패: {}", e.getMessage());
//                 }
//                 log.info("KOBIS API에서 가져온 총 영화 수: {}개", allMovies.size());
//                 if (allMovies.isEmpty()) {
//                     log.error("KOBIS API에서 영화를 가져오지 못했습니다!");
//                     return;
//                 }
//                 int savedMovieList = 0;
//                 int skippedMovieList = 0;
//                 for (MovieListDto dto : allMovies) {
//                     if (!prdMovieListRepository.existsByMovieCd(dto.getMovieCd())) {
//                         prdMovieListRepository.save(movieListMapper.toEntity(dto));
//                         savedMovieList++;
//                         if (savedMovieList <= 10) {
//                             log.info("MovieList 저장: {} ({}) - 개봉일: {}", dto.getMovieNm(), dto.getMovieCd(), dto.getOpenDt());
//                         }
//                     } else {
//                         skippedMovieList++;
//                     }
//                 }
//                 log.info("MovieList 적재 완료: {}개 저장, {}개 스킵 (이미 존재)", savedMovieList, skippedMovieList);
//                 log.info("=== KOBIS MovieList 로드 완료 ===");

//                 // MovieDetail 저장 (청불 제외, 1100개 제한)
//                 log.info("=== MovieDetail 로드 시작 (청불 제외, 1100개 제한) ===");
//                 long targetDetailCount = 1100L;
//                 long initialDetailCount = movieRepository.count();
//                 long savedDetailCount = initialDetailCount;
//                 Set<String> alreadySavedMovieCds = new HashSet<>();
//                 movieRepository.findAll().forEach(md -> alreadySavedMovieCds.add(md.getMovieCd()));
//                 log.info("MovieDetail 적재 시작: 현재 {}개, 목표 {}개, 이미 저장된 영화 {}개", initialDetailCount, targetDetailCount, alreadySavedMovieCds.size());
//                 List<MovieList> movieLists = prdMovieListRepository.findAll();
//                 int processedCount = 0;
//                 int addedCount = 0;
//                 int skippedCount = 0;
//                 int restrictedCount = 0;
//                 int failedCount = 0;
//                 for (MovieList movieList : movieLists) {
//                     if (savedDetailCount >= targetDetailCount) {
//                         log.info("목표 개수 도달: {}개, 처리 중단", targetDetailCount);
//                         break;
//                     }
//                     processedCount++;
//                     // 청불 제외
//                     if (movieList.getWatchGradeNm() == null || !movieList.getWatchGradeNm().contains("청소년관람불가")) {
//                         if (!alreadySavedMovieCds.contains(movieList.getMovieCd())) {
//                             if (addedCount < 10) {
//                                 log.info("MovieDetail 시도: {} ({}) - 관람등급: {}", movieList.getMovieNm(), movieList.getMovieCd(), movieList.getWatchGradeNm());
//                             }
//                             MovieDetail detail = kobisApiService.fetchAndSaveMovieDetail(movieList.getMovieCd());
//                             if (detail != null) {
//                                 savedDetailCount++;
//                                 addedCount++;
//                                 alreadySavedMovieCds.add(movieList.getMovieCd());
//                                 if (addedCount <= 10) {
//                                     log.info("MovieDetail 추가 성공: {} ({}) - 총 {}개", movieList.getMovieNm(), movieList.getMovieCd(), savedDetailCount);
//                                 } else if (addedCount % 50 == 0) {
//                                     log.info("MovieDetail 진행상황: {}개 추가됨 (총 {}개)", addedCount, savedDetailCount);
//                                 }
//                             } else {
//                                 failedCount++;
//                                 log.warn("MovieDetail 추가 실패: {} ({}) - API 응답 null", movieList.getMovieNm(), movieList.getMovieCd());
//                             }
//                         } else {
//                             skippedCount++;
//                             if (skippedCount <= 5) {
//                                 log.debug("MovieDetail 스킵 (이미 있음): {} ({})", movieList.getMovieNm(), movieList.getMovieCd());
//                             }
//                         }
//                     } else {
//                         restrictedCount++;
//                         if (restrictedCount <= 5) {
//                             log.debug("MovieDetail 스킵 (청불): {} ({}) - 관람등급: {}", movieList.getMovieNm(), movieList.getMovieCd(), movieList.getWatchGradeNm());
//                         }
//                     }
//                 }
//                 log.info("MovieDetail 적재 완료: 기존 {}개, 추가 {}개, 스킵(이미있음) {}개, 스킵(청불) {}개, 실패 {}개, 총 {}개", initialDetailCount, addedCount, skippedCount, restrictedCount, failedCount, savedDetailCount);
//                 log.info("=== MovieDetail 로드 완료: 상세정보 {}개 확보 ===", savedDetailCount);
//             } catch (Exception e) {
//                 log.error("KOBIS MovieList/Detail 로드 실패", e);
//             }
//         };
//     }

//     // 스케줄러: 2025년 최신영화 업데이트 (매일 새벽 1시에 실행)
//     @Scheduled(cron = "0 0 1 * * ?")
//     public void scheduledMovieListUpdate() {
//         log.info("=== 스케줄러: 2025년 최신영화 업데이트 시작 ===");
//         try {
//             // 2025년 인기영화 가져오기
//             log.info("스케줄러 - 2025년 인기영화 가져오기...");
//             List<MovieListDto> movies2025 = kobisPopularMovieService.getPopularMoviesBySales(200);
            
//             int savedMovieList = 0;
//             int skippedMovieList = 0;
            
//             for (MovieListDto dto : movies2025) {
//                 // 2025년 영화만 필터링 (모든 영화 포함)
//                 if (dto.getOpenDt() != null && dto.getOpenDt().getYear() == 2025) {
//                     if (!prdMovieListRepository.existsByMovieCd(dto.getMovieCd())) {
//                         prdMovieListRepository.save(movieListMapper.toEntity(dto));
//                         savedMovieList++;
//                         if (savedMovieList <= 10) {
//                             log.info("스케줄러 - 2025년 MovieList 저장: {} ({}) - 개봉일: {} - 관람등급: {}", dto.getMovieNm(), dto.getMovieCd(), dto.getOpenDt(), dto.getWatchGradeNm());
//                         }
//                     } else {
//                         skippedMovieList++;
//                     }
//                 }
//             }
            
//             log.info("스케줄러 - 2025년 MovieList 업데이트 완료: {}개 저장, {}개 스킵 (이미 존재)", savedMovieList, skippedMovieList);
//             log.info("=== 스케줄러: 2025년 최신영화 업데이트 완료 ===");
//         } catch (Exception e) {
//             log.error("스케줄러: 2025년 최신영화 업데이트 실패", e);
//         }
//     }
// } 
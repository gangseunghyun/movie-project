//package com.movie.movie_backend.config;
//
//import com.movie.movie_backend.repository.PRDMovieListRepository;
//import com.movie.movie_backend.repository.PRDMovieRepository;
//import com.movie.movie_backend.entity.MovieList;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.core.annotation.Order;
//import org.springframework.scheduling.annotation.EnableScheduling;
//import org.springframework.scheduling.annotation.Scheduled;
//
//import java.util.List;
//
//@Slf4j
//@Configuration
//@EnableScheduling
//@RequiredArgsConstructor
//public class CleanupLoader {
//
//    private final PRDMovieListRepository prdMovieListRepository;
//    private final PRDMovieRepository movieRepository;
//
//    // @Bean
//    // public CommandLineRunner cleanupOrphanMovieList() {
//    //     return args -> {
//    //         log.info("=== MovieDetail이 없는 MovieList 정리 시작 (최우선 실행) ===");
//    //         try {
//    //             // MovieDetail이 없는 MovieList 찾기
//    //             List<MovieList> allMovieLists = prdMovieListRepository.findAll();
//    //             int totalMovieList = allMovieLists.size();
//    //             int deletedCount = 0;
//    //
//    //             log.info("총 MovieList 개수: {}개", totalMovieList);
//    //
//    //             for (MovieList movieList : allMovieLists) {
//    //                 String movieCd = movieList.getMovieCd();
//    //
//    //                 // MovieDetail이 없는 경우 삭제
//    //                 if (!movieRepository.existsByMovieCd(movieCd)) {
//    //                     prdMovieListRepository.deleteById(movieCd);
//    //                     deletedCount++;
//    //
//    //                     if (deletedCount <= 10) {
//    //                         log.info("Orphan MovieList 삭제: {} ({})", movieList.getMovieNm(), movieCd);
//    //                     }
//    //                 }
//    //             }
//    //
//    //             log.info("=== MovieList 정리 완료 ===");
//    //             log.info("삭제된 Orphan MovieList: {}개", deletedCount);
//    //             log.info("남은 MovieList: {}개", totalMovieList - deletedCount);
//    //
//    //         } catch (Exception e) {
//    //             log.error("MovieList 정리 실패", e);
//    //         }
//    //     };
//    // }
//
//    // 스케줄러로도 실행 (매일 새벽 2시에 실행) - 비활성화됨
//    // @Scheduled(cron = "0 0 2 * * ?")
//    // public void scheduledCleanup() {
//    //     log.info("=== 스케줄러: MovieDetail이 없는 MovieList 정리 시작 ===");
//    //     try {
//    //         // MovieDetail이 없는 MovieList 찾기
//    //         List<MovieList> allMovieLists = prdMovieListRepository.findAll();
//    //         int totalMovieList = allMovieLists.size();
//    //         int deletedCount = 0;
//    //
//    //         log.info("총 MovieList 개수: {}개", totalMovieList);
//    //
//    //         for (MovieList movieList : allMovieLists) {
//    //             String movieCd = movieList.getMovieCd();
//    //
//    //             // MovieDetail이 없는 경우 삭제
//    //             if (!movieRepository.existsByMovieCd(movieCd)) {
//    //                 prdMovieListRepository.deleteById(movieCd);
//    //                 deletedCount++;
//    //
//    //                     if (deletedCount <= 10) {
//    //                         log.info("스케줄러 - Orphan MovieList 삭제: {} ({})", movieList.getMovieNm(), movieCd);
//    //                     }
//    //                 }
//    //             }
//    //
//    //             log.info("=== 스케줄러: MovieList 정리 완료 ===");
//    //             log.info("삭제된 Orphan MovieList: {}개", deletedCount);
//    //             log.info("남은 MovieList: {}개", totalMovieList - deletedCount);
//    //
//    //         } catch (Exception e) {
//    //             log.error("스케줄러: MovieList 정리 실패", e);
//    //         }
//    //     }
//}
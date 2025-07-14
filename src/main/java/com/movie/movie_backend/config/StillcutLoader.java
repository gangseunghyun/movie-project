// package com.movie.movie_backend.config;
//
// import com.movie.movie_backend.service.TmdbStillcutService;
// import com.movie.movie_backend.repository.PRDMovieRepository;
// import com.movie.movie_backend.repository.StillcutRepository;
// import com.movie.movie_backend.entity.MovieDetail;
// import com.movie.movie_backend.entity.Stillcut;
// import lombok.RequiredArgsConstructor;
// import lombok.extern.slf4j.Slf4j;
// import org.springframework.boot.CommandLineRunner;
// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Configuration;
// import org.springframework.core.annotation.Order;
// import org.springframework.scheduling.annotation.EnableScheduling;
// import org.springframework.scheduling.annotation.Scheduled;
//
// import java.util.List;
//
// @Slf4j
// @Configuration
// @EnableScheduling
// @RequiredArgsConstructor
// public class StillcutLoader {
//
//     private final TmdbStillcutService tmdbStillcutService;
//     private final PRDMovieRepository movieRepository;
//     private final StillcutRepository stillcutRepository;
//
//     @Bean
//     @Order(4)  // 줄거리 채우기 다음에 실행
//     public CommandLineRunner loadStillcutsOnly() {
//         return args -> {
//             // 처음 실행 시: 모든 MovieDetail에 스틸컷 세팅
//             log.info("=== Stillcut 초기 세팅 시작 (모든 MovieDetail에 스틸컷 세팅) ===");
//             try {
//                 // MovieDetail이 있는 영화들만 처리
//                 List<MovieDetail> movieDetails = movieRepository.findAll();
//                 int totalMovies = movieDetails.size();
//                 int processed = 0;
//                 int success = 0;
//                 int failed = 0;
//
//                 log.info("총 {}개의 MovieDetail에 대해 스틸컷을 세팅합니다.", totalMovies);
//
//                 for (MovieDetail movieDetail : movieDetails) {
//                     processed++;
//                     try {
//                         log.info("스틸컷 세팅 중: {}/{} - {} ({})",
//                             processed, totalMovies, movieDetail.getMovieNm(), movieDetail.getMovieCd());
//
//                         // 모든 MovieDetail에 스틸컷 세팅
//                         List<Stillcut> newStillcuts = tmdbStillcutService.fetchAndSaveStillcuts(movieDetail.getMovieCd());
//
//                         if (!newStillcuts.isEmpty()) {
//                             success++;
//                             log.info("스틸컷 세팅 성공: {} -> {}개", movieDetail.getMovieNm(), newStillcuts.size());
//                         } else {
//                             failed++;
//                             log.warn("스틸컷 세팅 실패: {} - 스틸컷을 찾을 수 없음", movieDetail.getMovieNm());
//                         }
//
//                         // API 호출 제한을 위한 딜레이
//                         Thread.sleep(100);
//
//                     } catch (Exception e) {
//                         failed++;
//                         log.error("스틸컷 세팅 오류: {} ({}) - {}",
//                             movieDetail.getMovieNm(), movieDetail.getMovieCd(), e.getMessage());
//                     }
//                 }
//
//                 log.info("=== Stillcut 초기 세팅 완료 ===");
//                 log.info("처리 결과: 총 {}개, 성공 {}개, 실패 {}개", totalMovies, success, failed);
//
//             } catch (Exception e) {
//                 log.error("Stillcut 초기 세팅 실패", e);
//             }
//         };
//     }
//
//     // 스케줄러: 7개 미만인 경우만 스틸컷 업로드 (매일 새벽 3시에 실행)
//     @Scheduled(cron = "0 0 3 * * ?")
//     public void scheduledStillcutUpdate() {
//         log.info("=== 스케줄러: Stillcut 업데이트 시작 (7개 미만인 경우만) ===");
//         try {
//             List<MovieDetail> movieDetails = movieRepository.findAll();
//             int totalMovies = movieDetails.size();
//             int processed = 0;
//             int success = 0;
//             int failed = 0;
//             int skipped = 0;
//
//             log.info("총 {}개의 MovieDetail에 대해 스틸컷을 확인합니다.", totalMovies);
//
//             for (MovieDetail movieDetail : movieDetails) {
//                 processed++;
//                 try {
//                     // 해당 MovieDetail의 스틸컷 개수 확인
//                     long existingCount = stillcutRepository.countByMovieDetailId(movieDetail.getId());
//
//                     log.info("스케줄러 - 스틸컷 확인 중: {}/{} - {} ({}) - 기존 스틸컷: {}개",
//                         processed, totalMovies, movieDetail.getMovieNm(), movieDetail.getMovieCd(), existingCount);
//
//                     // 7개 미만인 경우만 스틸컷 업로드
//                     if (existingCount < 7) {
//                         log.info("스케줄러 - 스틸컷 업로드 시작: {} ({}) - 기존 {}개",
//                             movieDetail.getMovieNm(), movieDetail.getMovieCd(), existingCount);
//
//                         // 개별 영화에 대해 스틸컷 처리
//                         List<Stillcut> newStillcuts = tmdbStillcutService.fetchAndSaveStillcuts(movieDetail.getMovieCd());
//
//                         if (!newStillcuts.isEmpty()) {
//                             success++;
//                             log.info("스케줄러 - 스틸컷 처리 성공: {} -> {}개 추가됨", movieDetail.getMovieNm(), newStillcuts.size());
//                         } else {
//                             failed++;
//                             log.warn("스케줄러 - 스틸컷 처리 실패: {} - 스틸컷을 찾을 수 없음", movieDetail.getMovieNm());
//                         }
//                     } else {
//                         skipped++;
//                         log.debug("스케줄러 - 스틸컷 스킵: {} ({}) - 이미 {}개 있음 (7개 이상)",
//                             movieDetail.getMovieNm(), movieDetail.getMovieCd(), existingCount);
//                     }
//
//                     // API 호출 제한을 위한 딜레이
//                     Thread.sleep(100);
//
//                 } catch (Exception e) {
//                     failed++;
//                     log.error("스케줄러 - 스틸컷 처리 오류: {} ({}) - {}",
//                         movieDetail.getMovieNm(), movieDetail.getMovieCd(), e.getMessage());
//                 }
//             }
//
//             log.info("=== 스케줄러: Stillcut 업데이트 완료 ===");
//             log.info("처리 결과: 총 {}개, 성공 {}개, 실패 {}개, 스킵 {}개", totalMovies, success, failed, skipped);
//
//         } catch (Exception e) {
//             log.error("스케줄러: Stillcut 업데이트 실패", e);
//         }
//     }
// }
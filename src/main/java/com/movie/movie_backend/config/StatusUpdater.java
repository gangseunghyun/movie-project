 package com.movie.movie_backend.config;

 import com.movie.movie_backend.repository.PRDMovieListRepository;
 import com.movie.movie_backend.entity.MovieList;
 import com.movie.movie_backend.constant.MovieStatus;
 import lombok.RequiredArgsConstructor;
 import lombok.extern.slf4j.Slf4j;
 import org.springframework.boot.CommandLineRunner;
 import org.springframework.context.annotation.Bean;
 import org.springframework.context.annotation.Configuration;
 import org.springframework.core.annotation.Order;
 import org.springframework.scheduling.annotation.EnableScheduling;
 import org.springframework.scheduling.annotation.Scheduled;
 import org.springframework.beans.factory.annotation.Value;
 import org.springframework.web.client.RestTemplate;
 import org.springframework.http.ResponseEntity;
 import org.json.JSONArray;
 import org.json.JSONObject;

 import java.util.List;

 @Slf4j
 @Configuration
 @EnableScheduling
 @RequiredArgsConstructor
 public class StatusUpdater {

     private final PRDMovieListRepository prdMovieListRepository;
    
     @Value("${kmdb.api.key}")
     private String kmdbApiKey;

     @Bean
     @Order(7)  // 태그 매핑 다음에 실행
     public CommandLineRunner updateMovieStatusFromKmdb() {
         return args -> {
             log.info("=== KMDb status 동기화 시작 ===");
             List<MovieList> movies = prdMovieListRepository.findByKmdbIdIsNotNull();
             RestTemplate restTemplate = new RestTemplate();
             int updated = 0, failed = 0;
             for (MovieList movie : movies) {
                 try {
                     String kmdbId = movie.getKmdbId();
                     String url = String.format(
                         "https://api.koreafilm.or.kr/openapi-data2/wisenut/search_api/search_json2.jsp?collection=kmdb_new2&DOCID=%s&ServiceKey=%s",
                         kmdbId, kmdbApiKey
                     );
                     ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
                     String repRlsDate = null;
                     if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                         JSONObject json = new JSONObject(response.getBody());
                         JSONArray dataArr = json.optJSONArray("Data");
                         if (dataArr != null && dataArr.length() > 0) {
                             JSONObject firstData = dataArr.getJSONObject(0);
                             JSONArray resultArr = firstData.optJSONArray("Result");
                             if (resultArr != null && resultArr.length() > 0) {
                                 JSONObject movieObj = resultArr.getJSONObject(0);
                                 repRlsDate = movieObj.optString("repRlsDate", "");
                             }
                         }
                     }
                     // repRlsDate가 없으면 openDt(내 DB) 사용
                     String baseDate = null;
                     if (repRlsDate != null && !repRlsDate.isBlank()) {
                         baseDate = repRlsDate.replace("-", "");
                     } else if (movie.getOpenDt() != null) {
                         baseDate = movie.getOpenDt().toString().replace("-", "");
                     }
                     String today = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE); // yyyyMMdd
                     MovieStatus status;
                     if (baseDate == null || baseDate.isBlank()) {
                         status = MovieStatus.COMING_SOON;
                     } else {
                         java.time.LocalDate openDate = java.time.LocalDate.parse(baseDate, java.time.format.DateTimeFormatter.BASIC_ISO_DATE);
                         java.time.LocalDate todayDate = java.time.LocalDate.now();
                         if (openDate.isAfter(todayDate)) {
                             status = MovieStatus.COMING_SOON;
                         } else if (openDate.plusDays(45).isBefore(todayDate)) {
                             status = MovieStatus.ENDED;
                         } else {
                             status = MovieStatus.NOW_PLAYING;
                         }
                     }
                     movie.setStatus(status);
                     prdMovieListRepository.save(movie);
                     updated++;
                     log.info("상태 업데이트: {}({}) -> {} (repRlsDate: {}, openDt: {})", movie.getMovieNm(), kmdbId, status, repRlsDate, movie.getOpenDt());
                     Thread.sleep(100);
                 } catch (Exception e) {
                     failed++;
                     log.error("상태 업데이트 실패: {}({}) - {}", movie.getMovieNm(), movie.getKmdbId(), e.getMessage());
                 }
             }
             log.info("=== KMDb status 동기화 완료: 성공 {}, 실패 {} ===", updated, failed);
         };
     }

     // 스케줄러: 영화 상태 업데이트 (매일 새벽 2시 30분에 실행)
     @Scheduled(cron = "0 30 2 * * ?")
     public void scheduledStatusUpdate() {
         log.info("=== 스케줄러: 영화 상태 업데이트 시작 ===");
         try {
             List<MovieList> movies = prdMovieListRepository.findByKmdbIdIsNotNull();
             log.info("스케줄러 - 상태 업데이트 대상 영화 {}개", movies.size());
            
             RestTemplate restTemplate = new RestTemplate();
             int updatedCount = 0;
             int unchangedCount = 0;
             int errorCount = 0;
            
             for (MovieList movie : movies) {
                 try {
                     String kmdbId = movie.getKmdbId();
                     String url = String.format(
                         "https://api.koreafilm.or.kr/openapi-data2/wisenut/search_api/search_json2.jsp?collection=kmdb_new2&DOCID=%s&ServiceKey=%s",
                         kmdbId, kmdbApiKey
                     );
                     ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
                     String repRlsDate = null;
                     if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                         JSONObject json = new JSONObject(response.getBody());
                         JSONArray dataArr = json.optJSONArray("Data");
                         if (dataArr != null && dataArr.length() > 0) {
                             JSONObject firstData = dataArr.getJSONObject(0);
                             JSONArray resultArr = firstData.optJSONArray("Result");
                             if (resultArr != null && resultArr.length() > 0) {
                                 JSONObject movieObj = resultArr.getJSONObject(0);
                                 repRlsDate = movieObj.optString("repRlsDate", "");
                             }
                         }
                     }
                    
                     // repRlsDate가 없으면 openDt(내 DB) 사용
                     String baseDate = null;
                     if (repRlsDate != null && !repRlsDate.isBlank()) {
                         baseDate = repRlsDate.replace("-", "");
                     } else if (movie.getOpenDt() != null) {
                         baseDate = movie.getOpenDt().toString().replace("-", "");
                     }
                    
                     String today = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE); // yyyyMMdd
                     MovieStatus newStatus;
                     if (baseDate == null || baseDate.isBlank()) {
                         newStatus = MovieStatus.COMING_SOON;
                     } else {
                         java.time.LocalDate openDate = java.time.LocalDate.parse(baseDate, java.time.format.DateTimeFormatter.BASIC_ISO_DATE);
                         java.time.LocalDate todayDate = java.time.LocalDate.now();
                         if (openDate.isAfter(todayDate)) {
                             newStatus = MovieStatus.COMING_SOON;
                         } else if (openDate.plusDays(45).isBefore(todayDate)) {
                             newStatus = MovieStatus.ENDED;
                         } else {
                             newStatus = MovieStatus.NOW_PLAYING;
                         }
                     }
                    
                     MovieStatus currentStatus = movie.getStatus();
                     if (!newStatus.equals(currentStatus)) {
                         movie.setStatus(newStatus);
                         prdMovieListRepository.save(movie);
                         updatedCount++;
                         if (updatedCount <= 10) {
                             log.info("스케줄러 - 상태 업데이트: {} ({}) {} -> {} (repRlsDate: {}, openDt: {})",
                                 movie.getMovieNm(), kmdbId, currentStatus, newStatus, repRlsDate, movie.getOpenDt());
                         }
                     } else {
                         unchangedCount++;
                     }
                    
                     // API 호출 제한을 위한 딜레이
                     Thread.sleep(100);
                 } catch (Exception e) {
                     errorCount++;
                     if (errorCount <= 5) {
                         log.error("스케줄러 - 상태 업데이트 오류: {} ({}) - {}",
                             movie.getMovieNm(), movie.getKmdbId(), e.getMessage());
                     }
                 }
             }
            
             log.info("스케줄러 - 상태 업데이트 완료: 변경 {}개, 유지 {}개, 오류 {}개",
                 updatedCount, unchangedCount, errorCount);
             log.info("=== 스케줄러: 영화 상태 업데이트 완료 ===");
         } catch (Exception e) {
             log.error("스케줄러: 영화 상태 업데이트 실패", e);
         }
     }
 }
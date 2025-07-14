// package com.movie.movie_backend.config;
//
// import com.movie.movie_backend.repository.PRDMovieListRepository;
// import com.movie.movie_backend.repository.PRDMovieRepository;
// import com.movie.movie_backend.entity.MovieList;
// import com.movie.movie_backend.entity.MovieDetail;
// import lombok.RequiredArgsConstructor;
// import lombok.extern.slf4j.Slf4j;
// import org.springframework.boot.CommandLineRunner;
// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Configuration;
// import org.springframework.core.annotation.Order;
// import org.springframework.scheduling.annotation.EnableScheduling;
// import org.springframework.scheduling.annotation.Scheduled;
// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.web.client.RestTemplate;
// import org.springframework.http.ResponseEntity;
// import org.springframework.util.StringUtils;
// import org.json.JSONArray;
// import org.json.JSONObject;
//
// import java.util.List;
// import java.util.ArrayList;
//
// @Slf4j
// @Configuration
// @EnableScheduling
// @RequiredArgsConstructor
// public class KmdbMapper {
//
//     private final PRDMovieListRepository prdMovieListRepository;
//     private final PRDMovieRepository movieRepository;
//
//     @Value("${kmdb.api.key}")
//     private String kmdbApiKey;
//
//     @Bean
//     @Order(5)  // 스틸컷 다음에 실행
//     public CommandLineRunner matchAndSaveKmdbIds() {
//         return args -> {
//             log.info("=== KMDb ID 매칭 및 저장 배치 시작 ===");
//
//             // KMDb API 테스트 먼저 실행
//             log.info("=== KMDb API 테스트 시작 ===");
//             testKmdbApi();
//             log.info("=== KMDb API 테스트 완료 ===");
//
//             // 1차 매칭 실행
//             performInitialMatching();
//
//             // 2차 매칭 실행 (실패한 영화들에 대해 추가 전략 적용)
//             performSecondaryMatching();
//
//             log.info("=== KMDb ID 매칭 및 저장 배치 완료 ===");
//         };
//     }
//
//     // 1차 매칭 실행 메서드
//     private void performInitialMatching() {
//         log.info("=== 1차 KMDb ID 매칭 시작 ===");
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
//             log.info("1차 배치 처리 중: {}/{} ({}-{})",
//                 (i / BATCH_SIZE) + 1,
//                 (unmappedMovies.size() + BATCH_SIZE - 1) / BATCH_SIZE,
//                 i + 1, endIndex);
//
//             // 배치 내에서 병렬 처리
//             java.util.concurrent.ExecutorService executor =
//                 java.util.concurrent.Executors.newFixedThreadPool(MAX_CONCURRENT_REQUESTS);
//
//             try {
//                 java.util.List<java.util.concurrent.Future<MovieMappingResult>> futures = new ArrayList<>();
//
//                 for (MovieList movie : batch) {
//                     futures.add(executor.submit(() -> processMovie(movie)));
//                 }
//
//                 // 결과 수집
//                 for (java.util.concurrent.Future<MovieMappingResult> future : futures) {
//                     try {
//                         MovieMappingResult result = future.get(30, java.util.concurrent.TimeUnit.SECONDS);
//                         if (result.success) {
//                             updated++;
//                             log.info("1차 매칭 성공: {} ({}) -> kmdbId={}",
//                                 result.movieNm, result.movieCd, result.kmdbId);
//                         } else {
//                             failed++;
//                             if (failed <= 10) {
//                                 log.warn("1차 매칭 실패: {} ({})", result.movieNm, result.movieCd);
//                             }
//                         }
//                     } catch (Exception e) {
//                         failed++;
//                         log.error("영화 처리 중 오류 발생: {}", e.getMessage());
//                     }
//                 }
//
//             } finally {
//                 executor.shutdown();
//                 try {
//                     if (!executor.awaitTermination(60, java.util.concurrent.TimeUnit.SECONDS)) {
//                         executor.shutdownNow();
//                     }
//                 } catch (InterruptedException e) {
//                     executor.shutdownNow();
//                     Thread.currentThread().interrupt();
//                 }
//             }
//
//             // 배치 간 딜레이
//             if (i + BATCH_SIZE < unmappedMovies.size()) {
//                 try {
//                     Thread.sleep(100);
//                 } catch (InterruptedException e) {
//                     Thread.currentThread().interrupt();
//                     log.warn("1차 매칭 sleep 중단: {}", e.getMessage());
//                     break;
//                 }
//             }
//         }
//
//         log.info("=== 1차 KMDb ID 매칭 완료: 성공 {}, 실패 {} ===", updated, failed);
//     }
//
//     // 2차 매칭 실행 메서드 (추가 검색 전략 적용)
//     private void performSecondaryMatching() {
//         log.info("=== 2차 KMDb ID 매칭 시작 (추가 전략) ===");
//
//         List<MovieList> movieLists = prdMovieListRepository.findAll();
//         List<MovieList> unmappedMovies = movieLists.stream()
//             .filter(movie -> !StringUtils.hasText(movie.getKmdbId()))
//             .collect(java.util.stream.Collectors.toList());
//
//         log.info("2차 매칭 대상: {}개 영화", unmappedMovies.size());
//
//         if (unmappedMovies.isEmpty()) {
//             log.info("매칭할 영화가 없습니다.");
//             return;
//         }
//
//         int updated = 0, failed = 0;
//
//         for (MovieList movie : unmappedMovies) {
//             try {
//                 MovieMappingResult result = processMovieWithAdvancedStrategy(movie);
//                 if (result.success) {
//                     updated++;
//                     log.info("2차 매칭 성공: {} ({}) -> kmdbId={}",
//                         result.movieNm, result.movieCd, result.kmdbId);
//                 } else {
//                     failed++;
//                     if (failed <= 20) {
//                         log.warn("2차 매칭 실패: {} ({})", result.movieNm, result.movieCd);
//                     }
//                 }
//
//                 // API 호출 제한을 위한 딜레이
//                 try {
//                     Thread.sleep(200);
//                 } catch (InterruptedException e) {
//                     Thread.currentThread().interrupt();
//                     log.warn("2차 매칭 sleep 중단: {}", e.getMessage());
//                     break;
//                 }
//
//             } catch (Exception e) {
//                 failed++;
//                 log.error("2차 매칭 중 오류: {} ({}) - {}", movie.getMovieNm(), movie.getMovieCd(), e.getMessage());
//             }
//         }
//
//         log.info("=== 2차 KMDb ID 매칭 완료: 성공 {}, 실패 {} ===", updated, failed);
//     }
//
//     // 영화 처리 결과를 담는 클래스
//     private static class MovieMappingResult {
//         final boolean success;
//         final String movieCd;
//         final String movieNm;
//         final String kmdbId;
//
//         MovieMappingResult(boolean success, String movieCd, String movieNm, String kmdbId) {
//             this.success = success;
//             this.movieCd = movieCd;
//             this.movieNm = movieNm;
//             this.kmdbId = kmdbId;
//         }
//     }
//
//     // 개별 영화 처리 메서드
//     private MovieMappingResult processMovie(MovieList movie) {
//         try {
//             String movieNm = movie.getMovieNm();
//             String movieNmEn = movie.getMovieNmEn();
//             String openDt = movie.getOpenDt() != null ? movie.getOpenDt().toString() : null;
//             String directorName = null;
//
//             // 검색어 정규화: 특수문자 제거 및 핵심 키워드 추출
//             String normalizedTitle = normalizeSearchTitle(movieNm);
//             log.debug("원본 제목: {}, 정규화된 제목: {}", movieNm, normalizedTitle);
//
//             // 감독 정보 가져오기
//             MovieDetail detail = movieRepository.findByMovieCd(movie.getMovieCd()).orElse(null);
//             if (detail != null && detail.getDirector() != null) {
//                 try {
//                     directorName = detail.getDirector().getName();
//                 } catch (Exception e) {
//                     log.debug("감독 정보 로드 실패: {} ({}) - {}", movie.getMovieNm(), movie.getMovieCd(), e.getMessage());
//                 }
//             }
//
//             RestTemplate restTemplate = new RestTemplate();
//             String kmdbId = null;
//
//             // 1. 정규화된 한글명으로 검색
//             if (StringUtils.hasText(normalizedTitle)) {
//                 kmdbId = searchKmdbIdFromKmdbApi(normalizedTitle, openDt, directorName, restTemplate);
//             }
//             // 2. 원본 한글명으로 검색 (실패시)
//             if (kmdbId == null && StringUtils.hasText(movieNm)) {
//                 kmdbId = searchKmdbIdFromKmdbApi(movieNm, openDt, directorName, restTemplate);
//             }
//             // 3. 영문명으로 검색 (실패시)
//             if (kmdbId == null && StringUtils.hasText(movieNmEn)) {
//                 kmdbId = searchKmdbIdFromKmdbApi(movieNmEn, openDt, directorName, restTemplate);
//             }
//
//             if (kmdbId != null) {
//                 // 트랜잭션 내에서 저장
//                 movie.setKmdbId(kmdbId);
//                 prdMovieListRepository.save(movie);
//                 return new MovieMappingResult(true, movie.getMovieCd(), movieNm, kmdbId);
//             } else {
//                 return new MovieMappingResult(false, movie.getMovieCd(), movieNm, null);
//             }
//
//         } catch (Exception e) {
//             log.error("영화 처리 중 오류 발생: {} ({}) - {}", movie.getMovieNm(), movie.getMovieCd(), e.getMessage());
//             return new MovieMappingResult(false, movie.getMovieCd(), movie.getMovieNm(), null);
//         }
//     }
//
//     // KMDb 검색 결과가 여러 개일 때 감독명까지 일치하는 영화 우선 선택
//     private String searchKmdbIdFromKmdbApi(String movieNm, String openDt, String directorName, RestTemplate restTemplate) {
//         try {
//             if (!StringUtils.hasText(movieNm)) return null;
//
//             log.debug("KMDb 검색 시작: 제목={}, 개봉일={}, 감독={}", movieNm, openDt, directorName);
//
//             // 1. 먼저 제목만으로 검색 (연도 없이)
//             String encodedTitle = java.net.URLEncoder.encode(movieNm, "UTF-8");
//             String url = String.format(
//                 "https://api.koreafilm.or.kr/openapi-data2/wisenut/search_api/search_json2.jsp?collection=kmdb_new2&title=%s&ServiceKey=%s&listCount=10&sort=prodYear,1",
//                 encodedTitle,
//                 kmdbApiKey
//             );
//
//             log.debug("KMDb API URL: {}", url);
//
//             ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
//
//             if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
//                 String responseBody = response.getBody();
//                 JSONObject json = new JSONObject(responseBody);
//
//                 // KMDb API 응답 구조 확인 (실제 응답 구조에 맞게 수정)
//                 JSONArray movies = null;
//
//                 // 실제 응답 구조: Data[0].Result
//                 if (json.has("Data")) {
//                     JSONArray data = json.getJSONArray("Data");
//                     if (data.length() > 0) {
//                         JSONObject firstData = data.getJSONObject(0);
//                         if (firstData.has("Result")) {
//                             movies = firstData.getJSONArray("Result");
//                         }
//                     }
//                 }
//
//                 if (movies != null && movies.length() > 0) {
//                     // TotalCount 체크 (최상위 레벨에서 확인)
//                     if (json.has("TotalCount") && json.getInt("TotalCount") == 0) {
//                         log.debug("KMDb 검색 결과 없음 (TotalCount=0): {}", movieNm);
//                         return null;
//                     }
//
//                     log.debug("KMDb 검색 성공: {} -> {}개 결과 발견", movieNm, movies.length());
//
//                     // 첫 번째 영화 정보 가져오기
//                     JSONObject firstMovie = movies.getJSONObject(0);
//
//                     // 실제 응답에서 DOCID 필드 사용 (KMDb 고유 ID)
//                     String docId = firstMovie.optString("DOCID", null);
//
//                     log.info("KMDb ID 필드 확인: DOCID={}, movieId={}, movieSeq={}",
//                         docId,
//                         firstMovie.optString("movieId", "null"),
//                         firstMovie.optString("movieSeq", "null"));
//
//                     // 1. 감독명까지 일치하는 영화 우선 선택
//                     if (StringUtils.hasText(directorName)) {
//                         for (int i = 0; i < movies.length(); i++) {
//                             JSONObject movieObj = movies.getJSONObject(i);
//
//                             // 감독 정보 파싱 (directors.director 배열에서 감독명 추출)
//                             String kmdbDirectorName = null;
//                             if (movieObj.has("directors")) {
//                                 JSONObject directors = movieObj.getJSONObject("directors");
//                                 if (directors.has("director")) {
//                                     JSONArray directorArray = directors.getJSONArray("director");
//                                     if (directorArray.length() > 0) {
//                                         JSONObject director = directorArray.getJSONObject(0);
//                                         kmdbDirectorName = director.optString("directorNm", "");
//                                     }
//                                 }
//                             }
//
//                             if (StringUtils.hasText(kmdbDirectorName) && kmdbDirectorName.contains(directorName)) {
//                                 String finalDocId = movieObj.optString("DOCID", null);
//                                 log.info("감독명 일치 발견: {} -> DOCID={}", movieNm, finalDocId);
//                                 return finalDocId;
//                             }
//                         }
//                     }
//
//                     // 2. 감독명 일치 영화가 없으면 첫 번째 결과 반환
//                     log.info("첫 번째 결과 선택: {} -> DOCID={}", movieNm, docId);
//                     return docId;
//                 } else {
//                     log.debug("KMDb 검색 결과 없음: {}", movieNm);
//                 }
//
//                 // 검색 결과 없음
//             } else {
//                 log.warn("KMDb API 호출 실패: status={}, movieNm={}", response.getStatusCode(), movieNm);
//             }
//
//         } catch (Exception e) {
//             log.warn("KMDb API 호출/파싱 실패: {} ({}) - {}", movieNm, openDt, e.getMessage());
//         }
//         return null;
//     }
//
//     // KMDb API 테스트 메서드
//     private void testKmdbApi() {
//         try {
//             log.info("KMDb API 키 확인: {}", kmdbApiKey != null ? kmdbApiKey.substring(0, Math.min(10, kmdbApiKey.length())) + "..." : "null");
//
//             RestTemplate restTemplate = new RestTemplate();
//
//             // 잘 알려진 영화로 테스트
//             String testMovies[] = {"기생충", "올드보이", "부산행"};
//
//             for (String testMovie : testMovies) {
//                 String url = String.format(
//                     "https://api.koreafilm.or.kr/openapi-data2/wisenut/search_api/search_json2.jsp?collection=kmdb_new2&title=%s&ServiceKey=%s&listCount=5",
//                     java.net.URLEncoder.encode(testMovie, "UTF-8"),
//                     kmdbApiKey
//                 );
//
//                 log.info("KMDb API 테스트: {} -> {}", testMovie, url);
//
//                 ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
//                 if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
//                     String responseBody = response.getBody();
//                     log.info("KMDb API 테스트 응답 ({}): {}", testMovie, responseBody.substring(0, Math.min(1000, responseBody.length())));
//
//                     // 응답 구조 분석
//                     try {
//                         JSONObject json = new JSONObject(responseBody);
//                         if (json.has("TotalCount")) {
//                             log.info("KMDb API 테스트 - TotalCount: {}", json.getInt("TotalCount"));
//                         }
//                         if (json.has("Data")) {
//                             JSONArray data = json.getJSONArray("Data");
//                             if (data.length() > 0) {
//                                 JSONObject firstData = data.getJSONObject(0);
//                                 if (firstData.has("Result")) {
//                                     JSONArray result = firstData.getJSONArray("Result");
//                                     log.info("KMDb API 테스트 - Result 개수: {}", result.length());
//                                     if (result.length() > 0) {
//                                         JSONObject firstMovie = result.getJSONObject(0);
//                                         log.info("KMDb API 테스트 - 첫 번째 영화 DOCID: {}", firstMovie.optString("DOCID", "null"));
//                                     }
//                                 }
//                             }
//                         }
//                     } catch (Exception e) {
//                         log.warn("KMDb API 테스트 응답 파싱 실패: {}", e.getMessage());
//                     }
//                 } else {
//                     log.warn("KMDb API 테스트 실패: {} - status={}", testMovie, response.getStatusCode());
//                 }
//
//                 Thread.sleep(100);
//             }
//
//             log.info("=== KMDb API 테스트 완료 ===");
//
//         } catch (Exception e) {
//             log.error("KMDb API 테스트 중 오류 발생", e);
//         }
//     }
//
//     // 검색어 정규화 메서드
//     private String normalizeSearchTitle(String title) {
//         if (!StringUtils.hasText(title)) {
//             return title;
//         }
//
//         String normalized = title;
//
//         // 1. 콜론(:) 이후 부분 제거
//         if (normalized.contains(":")) {
//             normalized = normalized.split(":")[0].trim();
//         }
//
//         // 2. 괄호 부분 제거
//         normalized = normalized.replaceAll("[\\(（].*?[\\)）]", "").trim();
//
//         // 3. 특정 접미사 제거
//         normalized = normalized
//             .replaceAll("극장판\\s*", "")
//             .replaceAll("시리즈\\s*", "")
//             .replaceAll("\\s+", " ")
//             .trim();
//
//         // 4. 특수문자 제거 (하이픈, 점 등)
//         normalized = normalized
//             .replaceAll("[-\\s\\.]+", " ")
//             .trim();
//
//         // 5. 너무 짧으면 원본 반환
//         if (normalized.length() < 2) {
//             return title;
//         }
//
//         return normalized;
//     }
//
//     // 고급 검색 전략을 사용한 영화 처리 메서드
//     private MovieMappingResult processMovieWithAdvancedStrategy(MovieList movie) {
//         try {
//             String movieNm = movie.getMovieNm();
//             String movieNmEn = movie.getMovieNmEn();
//             String openDt = movie.getOpenDt() != null ? movie.getOpenDt().toString() : null;
//             String directorName = null;
//
//             // 감독 정보 가져오기
//             MovieDetail detail = movieRepository.findByMovieCd(movie.getMovieCd()).orElse(null);
//             if (detail != null && detail.getDirector() != null) {
//                 try {
//                     directorName = detail.getDirector().getName();
//                 } catch (Exception e) {
//                     log.debug("감독 정보 로드 실패: {} ({}) - {}", movie.getMovieNm(), movie.getMovieCd(), e.getMessage());
//                 }
//             }
//
//             RestTemplate restTemplate = new RestTemplate();
//             String kmdbId = null;
//
//             // 고급 검색 전략들
//             String[] searchStrategies = {
//                 // 1. 영문명으로 검색 (한글 검색이 안 될 수 있음)
//                 movieNmEn,
//                 // 2. 제목에서 첫 단어만 사용
//                 extractFirstWord(movieNm),
//                 // 3. 제목에서 마지막 단어만 사용
//                 extractLastWord(movieNm),
//                 // 4. 제목에서 숫자 제거 후 검색
//                 removeNumbers(movieNm),
//                 // 5. 제목을 거꾸로 검색 (일부 영화는 부제목이 메인일 수 있음)
//                 reverseTitle(movieNm),
//                 // 6. 감독명 + 제목 조합
//                 directorName != null ? directorName + " " + extractFirstWord(movieNm) : null
//             };
//
//             for (String searchTerm : searchStrategies) {
//                 if (StringUtils.hasText(searchTerm) && searchTerm.length() > 1) {
//                     log.debug("고급 검색 시도: {} -> {}", movieNm, searchTerm);
//                     kmdbId = searchKmdbIdFromKmdbApi(searchTerm, openDt, directorName, restTemplate);
//                     if (kmdbId != null) {
//                         log.info("고급 검색 성공: {} -> {} (검색어: {})", movieNm, kmdbId, searchTerm);
//                         break;
//                     }
//                 }
//             }
//
//             if (kmdbId != null) {
//                 // 트랜잭션 내에서 저장
//                 movie.setKmdbId(kmdbId);
//                 prdMovieListRepository.save(movie);
//                 return new MovieMappingResult(true, movie.getMovieCd(), movieNm, kmdbId);
//             } else {
//                 return new MovieMappingResult(false, movie.getMovieCd(), movieNm, null);
//             }
//
//         } catch (Exception e) {
//             log.error("고급 검색 중 오류 발생: {} ({}) - {}", movie.getMovieNm(), movie.getMovieCd(), e.getMessage());
//             return new MovieMappingResult(false, movie.getMovieCd(), movie.getMovieNm(), null);
//         }
//     }
//
//     // 첫 번째 단어 추출
//     private String extractFirstWord(String title) {
//         if (!StringUtils.hasText(title)) return null;
//         String[] words = title.split("\\s+");
//         return words.length > 0 ? words[0] : null;
//     }
//
//     // 마지막 단어 추출
//     private String extractLastWord(String title) {
//         if (!StringUtils.hasText(title)) return null;
//         String[] words = title.split("\\s+");
//         return words.length > 0 ? words[words.length - 1] : null;
//     }
//
//     // 숫자 제거
//     private String removeNumbers(String title) {
//         if (!StringUtils.hasText(title)) return null;
//         return title.replaceAll("\\d+", "").replaceAll("\\s+", " ").trim();
//     }
//
//     // 제목 뒤집기 (콜론 기준)
//     private String reverseTitle(String title) {
//         if (!StringUtils.hasText(title) || !title.contains(":")) return null;
//         String[] parts = title.split(":");
//         if (parts.length >= 2) {
//             return parts[1].trim() + " " + parts[0].trim();
//         }
//         return null;
//     }
//
//     // 스케줄러: KMDb ID 매핑 업데이트 (매일 새벽 2시에 실행)
//     @Scheduled(cron = "0 0 2 * * ?")
//     public void scheduledKmdbMapping() {
//         log.info("=== 스케줄러: KMDb ID 매핑 시작 ===");
//         try {
//             List<MovieList> movieLists = prdMovieListRepository.findAll();
//             List<MovieList> unmappedMovies = movieLists.stream()
//                 .filter(movie -> !StringUtils.hasText(movie.getKmdbId()))
//                 .collect(java.util.stream.Collectors.toList());
//
//             log.info("스케줄러 - KMDb ID가 없는 영화 {}개 발견", unmappedMovies.size());
//
//             if (unmappedMovies.isEmpty()) {
//                 log.info("스케줄러 - 모든 영화가 이미 KMDb ID로 매핑되어 있습니다.");
//                 return;
//             }
//
//             int successCount = 0;
//             int failCount = 0;
//
//             for (MovieList movie : unmappedMovies) {
//                 try {
//                     MovieMappingResult result = processMovie(movie);
//                     if (result.success) {
//                         successCount++;
//                         if (successCount <= 10) {
//                             log.info("스케줄러 - KMDb ID 매핑 성공: {} ({}) -> {}", movie.getMovieNm(), movie.getMovieCd(), result.kmdbId);
//                         }
//                     } else {
//                         failCount++;
//                         if (failCount <= 5) {
//                             log.warn("스케줄러 - KMDb ID 매핑 실패: {} ({})", movie.getMovieNm(), movie.getMovieCd());
//                         }
//                     }
//                 } catch (Exception e) {
//                     failCount++;
//                     if (failCount <= 5) {
//                         log.error("스케줄러 - KMDb ID 매핑 오류: {} ({}) - {}", movie.getMovieNm(), movie.getMovieCd(), e.getMessage());
//                     }
//                 }
//
//                 // API 호출 제한을 위한 딜레이
//                 try {
//                     Thread.sleep(200);
//                 } catch (InterruptedException e) {
//                     Thread.currentThread().interrupt();
//                     log.warn("스케줄러 - KMDb ID 매핑 sleep 중단: {}", e.getMessage());
//                     break;
//                 }
//             }
//
//             log.info("스케줄러 - KMDb ID 매핑 완료: 성공 {}개, 실패 {}개", successCount, failCount);
//             log.info("=== 스케줄러: KMDb ID 매핑 완료 ===");
//         } catch (Exception e) {
//             log.error("스케줄러: KMDb ID 매핑 실패", e);
//         }
//     }
// }
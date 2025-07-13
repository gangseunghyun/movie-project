 package com.movie.movie_backend.config;

 import com.movie.movie_backend.service.KobisPopularMovieService;
 import com.movie.movie_backend.service.KobisApiService;
 import com.movie.movie_backend.repository.PRDMovieListRepository;
 import com.movie.movie_backend.repository.MovieDetailRepository;
 import com.movie.movie_backend.entity.MovieList;
 import com.movie.movie_backend.entity.MovieDetail;
 import com.movie.movie_backend.dto.MovieListDto;
 import com.movie.movie_backend.mapper.MovieListMapper;
 import lombok.RequiredArgsConstructor;
 import lombok.extern.slf4j.Slf4j;
 import org.springframework.boot.CommandLineRunner;
 import org.springframework.context.annotation.Bean;
 import org.springframework.context.annotation.Configuration;
 import org.springframework.core.annotation.Order;
 import org.springframework.beans.factory.annotation.Value;
 import org.springframework.web.client.RestTemplate;
 import org.springframework.http.ResponseEntity;
 import com.fasterxml.jackson.databind.JsonNode;
 import com.fasterxml.jackson.databind.ObjectMapper;
 import org.json.JSONArray;
 import org.json.JSONObject;
 import java.util.List;
 import java.util.ArrayList;
 import java.util.HashSet;
 import java.util.Set;

 @Slf4j
 @Configuration
 @RequiredArgsConstructor
 public class MovieLoaderConfig {
     private final KobisPopularMovieService kobisPopularMovieService;
     private final PRDMovieListRepository prdMovieListRepository;
     private final MovieListMapper movieListMapper;
     private final MovieDetailRepository movieRepository;
     private final KobisApiService kobisApiService;
     private final RestTemplate restTemplate;
     private final ObjectMapper objectMapper;

     @Value("${kmdb.api.key}")
     private String kmdbApiKey;

     @Value("${kobis.api.key}")
     private String kobisApiKey;

     @Value("${tmdb.api.key}")
     private String tmdbApiKey;

     @Bean
     @Order(1)  // BoxOffice보다 먼저 실행
     public CommandLineRunner loadKobisMoviesAndDetails() {
         return args -> {
             log.info("=== KOBIS MovieList 로드 시작 (2025년 이후 영화) ===");
             try {
                 // 연도별 limit 변수 선언 (청불 제외 후 1000개 목표, API 키 2개 사용)
                 int limit2025 = 1200;
                 int limit2024 = 1200;
                 int limit2023 = 1000;
                 int[] years = {2025, 2024, 2023};
                 int[] limits = {limit2025, limit2024, limit2023};
                 List<MovieListDto> allMovies = new ArrayList<>();
                 Set<String> addedMovieCds = new HashSet<>();
                 // MovieList 가져오기 (API 제한 고려)
                 try {
                     for (int i = 0; i < years.length; i++) {
                         List<MovieListDto> yearMovies = kobisPopularMovieService.getPopularMoviesBySales(limits[i], years[i]);
                         for (MovieListDto dto : yearMovies) {
                             if (!addedMovieCds.contains(dto.getMovieCd())) {
                                 allMovies.add(dto);
                                 addedMovieCds.add(dto.getMovieCd());
                             }
                         }
                     }
                 } catch (Exception e) {
                     log.warn("MovieList 가져오기 중 API 제한 또는 오류 발생: {}", e.getMessage());
                     log.info("기존 MovieList에서 MovieDetail 추가를 계속 진행합니다.");
                 }
                 log.info("연도별 인기영화 합산 후 총 MovieList 수: {}개", allMovies.size());
                 if (allMovies.isEmpty()) {
                     log.error("KOBIS API에서 영화를 가져오지 못했습니다!");
                     return;
                 }
                 int savedMovieList = 0;
                 int skippedMovieList = 0;
                 for (MovieListDto dto : allMovies) {
                     if (!prdMovieListRepository.existsByMovieCd(dto.getMovieCd())) {
                         prdMovieListRepository.save(movieListMapper.toEntity(dto));
                         savedMovieList++;
                         if (savedMovieList <= 10) {
                             log.info("MovieList 저장: {} ({}) - 개봉일: {}", dto.getMovieNm(), dto.getMovieCd(), dto.getOpenDt());
                         }
                     } else {
                         skippedMovieList++;
                     }
                 }
                 log.info("MovieList 적재 완료: {}개 저장, {}개 스킵 (이미 존재)", savedMovieList, skippedMovieList);
                 log.info("=== KOBIS MovieList 로드 완료 ===");

                 // MovieDetail 저장 (청불 제외, 2000개 제한 - API 호출 제한 고려)
                 log.info("=== MovieDetail 로드 시작 (청불 제외, 2000개 제한) ===");
                 long targetDetailCount = 2000L;
                 long initialDetailCount = movieRepository.count();
                 long savedDetailCount = initialDetailCount;
                 Set<String> alreadySavedMovieCds = new HashSet<>();
                 movieRepository.findAll().forEach(md -> alreadySavedMovieCds.add(md.getMovieCd()));
                 log.info("MovieDetail 적재 시작: 현재 {}개, 목표 {}개, 이미 저장된 영화 {}개", initialDetailCount, targetDetailCount, alreadySavedMovieCds.size());
                 List<MovieList> movieLists = prdMovieListRepository.findAll();
                 int processedCount = 0;
                 int addedCount = 0;
                 int skippedCount = 0;
                 int restrictedCount = 0;
                 int failedCount = 0;
                 for (MovieList movieList : movieLists) {
                     if (savedDetailCount >= targetDetailCount) {
                         log.info("목표 개수 도달: {}개, 처리 중단", targetDetailCount);
                         break;
                     }
                     processedCount++;
                     // 청불 제외
                     if (movieList.getWatchGradeNm() == null || !movieList.getWatchGradeNm().contains("청소년관람불가")) {
                         if (!alreadySavedMovieCds.contains(movieList.getMovieCd())) {
                             if (addedCount < 10) {
                                 log.info("MovieDetail 시도: {} ({}) - 관람등급: {}", movieList.getMovieNm(), movieList.getMovieCd(), movieList.getWatchGradeNm());
                             }
                             MovieDetail detail = kobisApiService.fetchAndSaveMovieDetail(movieList.getMovieCd());
                             if (detail != null) {
                                 savedDetailCount++;
                                 addedCount++;
                                 alreadySavedMovieCds.add(movieList.getMovieCd());
                                 if (addedCount <= 10) {
                                     log.info("MovieDetail 추가 성공: {} ({}) - 총 {}개", movieList.getMovieNm(), movieList.getMovieCd(), savedDetailCount);
                                 } else if (addedCount % 50 == 0) {
                                     log.info("MovieDetail 진행상황: {}개 추가됨 (총 {}개)", addedCount, savedDetailCount);
                                 }
                             } else {
                                 failedCount++;
                                 log.warn("MovieDetail 추가 실패: {} ({}) - API 응답 null", movieList.getMovieNm(), movieList.getMovieCd());
                             }
                         } else {
                             skippedCount++;
                             if (skippedCount <= 5) {
                                 log.debug("MovieDetail 스킵 (이미 있음): {} ({})", movieList.getMovieNm(), movieList.getMovieCd());
                             }
                         }
                     } else {
                         restrictedCount++;
                         if (restrictedCount <= 5) {
                             log.debug("MovieDetail 스킵 (청불): {} ({}) - 관람등급: {}", movieList.getMovieNm(), movieList.getMovieCd(), movieList.getWatchGradeNm());
                         }
                     }
                 }
                 log.info("MovieDetail 적재 완료: 기존 {}개, 추가 {}개, 스킵(이미있음) {}개, 스킵(청불) {}개, 실패 {}개, 총 {}개 (2000개 제한)", initialDetailCount, addedCount, skippedCount, restrictedCount, failedCount, savedDetailCount);
                 log.info("=== MovieDetail 로드 완료: 상세정보 {}개 확보 ===", savedDetailCount);
             } catch (Exception e) {
                 log.error("KOBIS MovieList/Detail 로드 실패", e);
             }
         };
     }

     @Bean
     @Order(3)  // MovieDetail 다음에 실행
     public CommandLineRunner fillMovieDescriptions() {
         return args -> {
             log.info("=== 영화 줄거리(Description) 채우기 시작 ===");
             try {
                 // MovieDetail이 있는 영화만 대상으로 함 (청불 영화는 이미 제외됨)
                 List<MovieDetail> movieDetails = movieRepository.findAll();
                 log.info("줄거리 채우기 대상: {}개 영화", movieDetails.size());
                
                 int successCount = 0;
                 int skipCount = 0;
                 int failCount = 0;
                
                 for (MovieDetail movieDetail : movieDetails) {
                     try {
                         // 이미 줄거리가 있으면 스킵
                         if (movieDetail.getDescription() != null && !movieDetail.getDescription().trim().isEmpty()) {
                             skipCount++;
                             if (skipCount <= 5) {
                                 log.debug("줄거리 스킵 (이미 있음): {} ({})", movieDetail.getMovieNm(), movieDetail.getMovieCd());
                             }
                             continue;
                         }
                        
                         log.info("줄거리 채우기 시도: {} ({})", movieDetail.getMovieNm(), movieDetail.getMovieCd());
                        
                         // KOBIS → TMDB → KMDb 순서로 줄거리 가져오기
                         String description = getDescriptionFromKobis(movieDetail.getMovieCd());
                        
                         if (description == null || description.trim().isEmpty()) {
                             description = getDescriptionFromTmdb(movieDetail.getMovieNm(), movieDetail.getOpenDt());
                         }
                        
                         if (description == null || description.trim().isEmpty()) {
                             description = getDescriptionFromKmdb(movieDetail.getMovieCd());
                         }
                        
                         if (description != null && !description.trim().isEmpty()) {
                             movieDetail.setDescription(description);
                             movieRepository.save(movieDetail);
                             successCount++;
                             log.info("줄거리 채우기 성공: {} ({}) - 길이: {}자",
                                 movieDetail.getMovieNm(), movieDetail.getMovieCd(), description.length());
                         } else {
                             failCount++;
                             log.warn("줄거리 채우기 실패: {} ({}) - 모든 소스에서 실패",
                                 movieDetail.getMovieNm(), movieDetail.getMovieCd());
                         }
                        
                         // API 호출 제한을 위한 딜레이
                         Thread.sleep(100);
                        
                     } catch (Exception e) {
                         failCount++;
                         log.error("줄거리 채우기 중 오류: {} ({}) - {}",
                             movieDetail.getMovieNm(), movieDetail.getMovieCd(), e.getMessage());
                     }
                 }
                
                 log.info("=== 영화 줄거리 채우기 완료 ===");
                 log.info("성공: {}개, 스킵(이미있음): {}개, 실패: {}개", successCount, skipCount, failCount);
                
             } catch (Exception e) {
                 log.error("영화 줄거리 채우기 실패", e);
             }
         };
     }

     // KOBIS에서 줄거리 가져오기
     private String getDescriptionFromKobis(String movieCd) {
         try {
             String url = String.format("http://www.kobis.or.kr/kobisopenapi/webservice/rest/movie/searchMovieInfo.json?key=%s&movieCd=%s",
                 kobisApiKey, movieCd);
            
             ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
             if (response.getStatusCode().is2xxSuccessful()) {
                 JsonNode rootNode = objectMapper.readTree(response.getBody());
                 JsonNode movieInfo = rootNode.path("movieInfoResult").path("movieInfo");
                
                 if (movieInfo.has("plot") && !movieInfo.get("plot").isNull()) {
                     String plot = movieInfo.get("plot").asText();
                     if (!plot.trim().isEmpty()) {
                         log.info("KOBIS에서 줄거리 발견: movieCd={}, 길이={}자", movieCd, plot.length());
                         return plot;
                     }
                 }
             }
         } catch (Exception e) {
             log.warn("KOBIS 줄거리 가져오기 실패: movieCd={} - {}", movieCd, e.getMessage());
         }
         return null;
     }

     // TMDB에서 줄거리 가져오기
     private String getDescriptionFromTmdb(String movieNm, java.time.LocalDate openDt) {
         try {
             String query = java.net.URLEncoder.encode(movieNm, java.nio.charset.StandardCharsets.UTF_8);
             String year = (openDt != null) ? String.valueOf(openDt.getYear()) : null;
            
             String url = String.format("https://api.themoviedb.org/3/search/movie?api_key=%s&query=%s&language=ko-KR%s",
                 tmdbApiKey, query, year != null ? "&year=" + year : "");
            
             ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
             if (response.getStatusCode().is2xxSuccessful()) {
                 JsonNode rootNode = objectMapper.readTree(response.getBody());
                 JsonNode results = rootNode.get("results");
                
                 if (results != null && results.size() > 0) {
                     JsonNode movie = results.get(0);
                     String overview = movie.has("overview") ? movie.get("overview").asText() : "";
                    
                     if (!overview.trim().isEmpty()) {
                         log.info("TMDB에서 줄거리 발견: 영화={}, 길이={}자", movieNm, overview.length());
                         return overview;
                     }
                 }
             }
         } catch (Exception e) {
             log.warn("TMDB 줄거리 가져오기 실패: 영화={} - {}", movieNm, e.getMessage());
         }
         return null;
     }

     // KMDb에서 줄거리 가져오기
     private String getDescriptionFromKmdb(String movieCd) {
         try {
             // MovieList에서 kmdbId 가져오기
             MovieList movieList = prdMovieListRepository.findById(movieCd).orElse(null);
             if (movieList == null || movieList.getKmdbId() == null) {
                 return null;
             }
            
             String url = String.format(
                 "https://api.koreafilm.or.kr/openapi-data2/wisenut/search_api/search_json2.jsp?collection=kmdb_new2&DOCID=%s&ServiceKey=%s",
                 movieList.getKmdbId(), kmdbApiKey
             );
            
             ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
             if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                 JSONObject json = new JSONObject(response.getBody());
                 JSONArray dataArr = json.optJSONArray("Data");
                 if (dataArr != null && dataArr.length() > 0) {
                     JSONObject firstData = dataArr.getJSONObject(0);
                     JSONArray resultArr = firstData.optJSONArray("Result");
                     if (resultArr != null && resultArr.length() > 0) {
                         JSONObject movieObj = resultArr.getJSONObject(0);
                        
                         // KMDb에서 줄거리 필드들 확인 (plot, plots, plotsText 등)
                         String[] plotFields = {"plot", "plots", "plotsText", "plotText"};
                         for (String field : plotFields) {
                             if (movieObj.has(field) && !movieObj.isNull(field)) {
                                 String plot = movieObj.optString(field, "");
                                 if (!plot.trim().isEmpty()) {
                                     log.info("KMDb에서 줄거리 발견: movieCd={}, 필드={}, 길이={}자", movieCd, field, plot.length());
                                     return plot;
                                 }
                             }
                         }
                     }
                 }
             }
         } catch (Exception e) {
             log.warn("KMDb 줄거리 가져오기 실패: movieCd={} - {}", movieCd, e.getMessage());
         }
         return null;
     }
 }
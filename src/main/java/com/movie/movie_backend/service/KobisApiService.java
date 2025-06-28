package com.movie.movie_backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.movie.movie_backend.entity.Director;
import com.movie.movie_backend.entity.MovieDetail;
import com.movie.movie_backend.entity.MovieList;
import com.movie.movie_backend.repository.PRDDirectorRepository;
import com.movie.movie_backend.repository.PRDMovieRepository;
import com.movie.movie_backend.repository.PRDMovieListRepository;
import com.movie.movie_backend.dto.MovieListDto;
import com.movie.movie_backend.constant.MovieStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class KobisApiService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final PRDMovieRepository movieRepository;
    private final PRDMovieListRepository prdMovieListRepository;
    private final PRDDirectorRepository directorRepository;

    @Value("${tmdb.api.key}")
    private String tmdbApiKey;

    @Value("${kobis.api.key}")
    private String kobisApiKey;

    /**
     * MovieList에 있는 영화들의 한글제목/영문제목으로 TMDB에서 검색하여 MovieDetail 보완
     */
    public void fillMissingMovieDetailsFromTmdb() {
        try {
            log.info("=== TMDB로 MovieDetail 보완 시작 ===");
            
            // MovieList에서 MovieDetail이 없는 영화들 찾기
            List<MovieList> allMovieLists = prdMovieListRepository.findAll();
            List<String> missingMovieCds = new ArrayList<>();
            
            for (MovieList movieList : allMovieLists) {
                if (!movieRepository.existsById(movieList.getMovieCd())) {
                    missingMovieCds.add(movieList.getMovieCd());
                }
            }
            
            log.info("MovieDetail이 누락된 영화 {}개 발견", missingMovieCds.size());
            
            int successCount = 0;
            int failCount = 0;
            int attemptCount = 0;
            int maxAttempts = 3; // 최대 3번으로 제한
            
            for (String movieCd : missingMovieCds) {
                if (attemptCount >= maxAttempts) {
                    log.info("TMDB MovieDetail 보완 시도 횟수 제한에 도달했습니다. (최대 {}회)", maxAttempts);
                    break;
                }
                
                try {
                    MovieList movieList = prdMovieListRepository.findById(movieCd).orElse(null);
                    if (movieList == null) {
                        failCount++;
                        continue;
                    }
                    
                    log.info("TMDB로 MovieDetail 보완 시도: {} ({}) - 시도 {}/{}", 
                        movieList.getMovieNm(), movieCd, attemptCount + 1, maxAttempts);
                    
                    // TMDB에서 영화 검색
                    MovieDetail movieDetail = searchAndSaveMovieDetailFromTmdb(movieList);
                    
                    if (movieDetail != null) {
                        successCount++;
                        log.info("TMDB MovieDetail 보완 성공: {} ({})", movieList.getMovieNm(), movieCd);
                    } else {
                        failCount++;
                        log.warn("TMDB MovieDetail 보완 실패: {} ({})", movieList.getMovieNm(), movieCd);
                    }
                    
                    attemptCount++;
                    
                    // API 호출 제한을 위한 딜레이
                    Thread.sleep(200);
                    
                } catch (Exception e) {
                    failCount++;
                    attemptCount++;
                    log.error("TMDB MovieDetail 보완 실패: {} - {}", movieCd, e.getMessage());
                }
            }
            
            log.info("=== TMDB로 MovieDetail 보완 완료 ===");
            log.info("성공: {}개, 실패: {}개, 시도 횟수: {}/{}", successCount, failCount, attemptCount, maxAttempts);
            
        } catch (Exception e) {
            log.error("TMDB로 MovieDetail 보완 실패", e);
        }
    }

    /**
     * 영화 제목으로 TMDB에서 검색하여 MovieDetail 저장
     */
    private MovieDetail searchAndSaveMovieDetailFromTmdb(MovieList movieList) {
        try {
            // 검색할 제목 결정 (영문제목 우선, 없으면 한글제목)
            String searchTitle = movieList.getMovieNmEn();
            if (searchTitle == null || searchTitle.isEmpty()) {
                searchTitle = movieList.getMovieNm();
            }
            
            log.info("TMDB 검색 시작: {} (원본: {})", searchTitle, movieList.getMovieNm());
            
            // TMDB 검색 API 호출
            String encodedTitle = java.net.URLEncoder.encode(searchTitle, java.nio.charset.StandardCharsets.UTF_8);
            String url = String.format("https://api.themoviedb.org/3/search/movie?api_key=%s&query=%s&language=ko-KR&page=1", 
                tmdbApiKey, encodedTitle);
            
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                log.error("TMDB 검색 API 호출 실패: status={}", response.getStatusCode());
                return null;
            }
            
            JsonNode rootNode = objectMapper.readTree(response.getBody());
            JsonNode results = rootNode.get("results");
            
            if (results == null || results.size() == 0) {
                log.warn("TMDB에서 영화를 찾을 수 없음: {}", searchTitle);
                return null;
            }
            
            // 첫 번째 결과 사용
            JsonNode tmdbMovie = results.get(0);
            String tmdbTitle = tmdbMovie.get("title").asText();
            String tmdbId = tmdbMovie.get("id").asText();
            
            log.info("TMDB에서 영화 발견: {} (TMDB ID: {})", tmdbTitle, tmdbId);
            
            // TMDB 영화 상세정보 가져오기
            String detailUrl = String.format("https://api.themoviedb.org/3/movie/%s?api_key=%s&language=ko-KR&append_to_response=credits", 
                tmdbId, tmdbApiKey);
            
            ResponseEntity<String> detailResponse = restTemplate.getForEntity(detailUrl, String.class);
            if (!detailResponse.getStatusCode().is2xxSuccessful()) {
                log.error("TMDB 상세정보 API 호출 실패: status={}", detailResponse.getStatusCode());
                return null;
            }
            
            JsonNode detailNode = objectMapper.readTree(detailResponse.getBody());
            
            // 기본 정보 추출
            String overview = detailNode.has("overview") ? detailNode.get("overview").asText() : "";
            String releaseDateStr = detailNode.has("release_date") ? detailNode.get("release_date").asText() : "";
            int runtime = detailNode.has("runtime") ? detailNode.get("runtime").asInt() : 0;
            double voteAverage = detailNode.has("vote_average") ? detailNode.get("vote_average").asDouble() : 0.0;
            
            // 개봉일 파싱
            java.time.LocalDate releaseDate = null;
            if (!releaseDateStr.isEmpty()) {
                try {
                    releaseDate = java.time.LocalDate.parse(releaseDateStr, java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                } catch (Exception e) {
                    log.warn("TMDB 날짜 파싱 실패: {}", releaseDateStr);
                }
            }
            
            // 장르 정보
            String genreNm = "";
            if (detailNode.has("genres") && detailNode.get("genres").isArray()) {
                JsonNode genres = detailNode.get("genres");
                List<String> genreNames = new ArrayList<>();
                for (JsonNode genre : genres) {
                    genreNames.add(genre.get("name").asText());
                }
                genreNm = String.join(", ", genreNames);
            }
            
            // 감독 정보
            String directorName = "";
            if (detailNode.has("credits") && detailNode.get("credits").has("crew")) {
                JsonNode crew = detailNode.get("credits").get("crew");
                for (JsonNode person : crew) {
                    if ("Director".equals(person.get("job").asText())) {
                        directorName = person.get("name").asText();
                        break;
                    }
                }
            }
            
            // MovieDetail 엔티티 생성 (기존 movieCd 사용)
            MovieDetail movieDetail = MovieDetail.builder()
                .movieCd(movieList.getMovieCd()) // 기존 KOBIS movieCd 유지
                .movieNm(movieList.getMovieNm()) // 기존 한글제목 유지
                .movieNmEn(movieList.getMovieNmEn()) // 기존 영문제목 유지
                .description(overview)
                .openDt(releaseDate != null ? releaseDate : movieList.getOpenDt())
                .showTm(runtime)
                .genreNm(genreNm.isEmpty() ? movieList.getGenreNm() : genreNm)
                .nationNm(movieList.getNationNm())
                .watchGradeNm(movieList.getWatchGradeNm())
                .companyNm("")
                .totalAudience(0)
                .reservationRate(0.0)
                .averageRating(voteAverage)
                .status(movieList.getStatus())
                .build();
            
            // 감독 정보 저장
            if (!directorName.isEmpty()) {
                Director director = saveDirectorByName(directorName);
                movieDetail.setDirector(director);
            }
            
            // MovieDetail 저장
            MovieDetail savedMovieDetail = movieRepository.save(movieDetail);
            
            log.info("TMDB MovieDetail 저장 완료: {} ({})", savedMovieDetail.getMovieNm(), savedMovieDetail.getMovieCd());
            return savedMovieDetail;
            
        } catch (Exception e) {
            log.error("TMDB 검색 및 저장 실패: {} - {}", movieList.getMovieNm(), e.getMessage());
            return null;
        }
    }

    /**
     * 감독 이름으로 감독 정보 저장
     */
    private Director saveDirectorByName(String directorName) {
        // 기존 감독이 있는지 확인
        Optional<Director> existingDirector = directorRepository.findByName(directorName);
        
        if (existingDirector.isPresent()) {
            return existingDirector.get();
        }

        // 새 감독 생성 (TMDB에서 이미지 URL 조회)
        String photoUrl = fetchDirectorImageUrlFromTmdb(directorName);
        Director director = Director.builder()
                .name(directorName)
                .photoUrl(photoUrl)
                .build();
        
        return directorRepository.save(director);
    }

    /**
     * TMDB에서 감독 이미지 URL 가져오기
     */
    private String fetchDirectorImageUrlFromTmdb(String directorName) {
        try {
            String encodedName = java.net.URLEncoder.encode(directorName, java.nio.charset.StandardCharsets.UTF_8);
            String url = String.format("https://api.themoviedb.org/3/search/person?api_key=%s&query=%s&language=ko-KR", 
                tmdbApiKey, encodedName);
            
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode rootNode = objectMapper.readTree(response.getBody());
                JsonNode results = rootNode.get("results");
                
                if (results != null && results.size() > 0) {
                    JsonNode person = results.get(0);
                    if (person.has("profile_path") && !person.get("profile_path").isNull()) {
                        String profilePath = person.get("profile_path").asText();
                        return "https://image.tmdb.org/t/p/w500" + profilePath;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("TMDB 감독 이미지 URL 조회 실패: {} - {}", directorName, e.getMessage());
        }
        
        return null;
    }

    /**
     * KOBIS API로 영화 상세정보 가져오기
     */
    public MovieDetail fetchAndSaveMovieDetail(String movieCd) {
        try {
            log.info("KOBIS API로 MovieDetail 가져오기 시작: {}", movieCd);
            
            String url = String.format("http://www.kobis.or.kr/kobisopenapi/webservice/rest/movie/searchMovieInfo.json?key=%s&movieCd=%s", 
                kobisApiKey, movieCd);
            
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                log.error("KOBIS API 호출 실패: status={}", response.getStatusCode());
                return null;
            }
            
            JsonNode rootNode = objectMapper.readTree(response.getBody());
            JsonNode movieInfoResult = rootNode.get("movieInfoResult");
            
            if (movieInfoResult == null || movieInfoResult.get("movieInfo") == null) {
                log.warn("KOBIS API 응답에 movieInfo가 없음: {}", movieCd);
                return null;
            }
            
            JsonNode movieInfo = movieInfoResult.get("movieInfo");
            
            // MovieList에서 기본 정보 가져오기
            MovieList movieList = prdMovieListRepository.findById(movieCd).orElse(null);
            if (movieList == null) {
                log.warn("MovieList를 찾을 수 없음: {}", movieCd);
                return null;
            }
            
            // 상세 정보 추출
            String description = "";
            if (movieInfo.has("plot") && !movieInfo.get("plot").isNull()) {
                description = movieInfo.get("plot").asText();
            }
            
            int showTm = 0;
            if (movieInfo.has("showTm") && !movieInfo.get("showTm").isNull()) {
                showTm = movieInfo.get("showTm").asInt();
            }
            
            String companyNm = "";
            if (movieInfo.has("companys") && movieInfo.get("companys").isArray()) {
                JsonNode companys = movieInfo.get("companys");
                for (JsonNode company : companys) {
                    if ("제작사".equals(company.get("companyPartNm").asText())) {
                        companyNm = company.get("companyNm").asText();
                        break;
                    }
                }
            }
            
            // 감독 정보
            Director director = null;
            if (movieInfo.has("directors") && movieInfo.get("directors").isArray()) {
                JsonNode directors = movieInfo.get("directors");
                if (directors.size() > 0) {
                    String directorName = directors.get(0).get("peopleNm").asText();
                    director = saveDirectorByName(directorName);
                }
            }
            
            // TMDB에서 영어 제목과 장르 정보 보완
            String movieNmEn = movieList.getMovieNmEn();
            String genreNm = movieList.getGenreNm();
            
            // KOBIS에 영어 제목이 없거나 장르 정보가 부족하면 TMDB에서 보완
            if ((movieNmEn == null || movieNmEn.isEmpty() || genreNm == null || genreNm.isEmpty()) 
                && movieList.getOpenDt() != null) {
                
                try {
                    // TMDB에서 영화 검색하여 영어 제목과 장르 정보 가져오기
                    String tmdbInfo = getTmdbMovieInfo(movieList.getMovieNm(), movieList.getOpenDt());
                    if (tmdbInfo != null) {
                        String[] tmdbData = tmdbInfo.split("\\|");
                        if (tmdbData.length >= 2) {
                            if (movieNmEn == null || movieNmEn.isEmpty()) {
                                movieNmEn = tmdbData[0]; // 영어 제목
                            }
                            if (genreNm == null || genreNm.isEmpty()) {
                                genreNm = tmdbData[1]; // 장르
                            }
                        }
                    }
                } catch (Exception e) {
                    log.warn("TMDB 정보 보완 실패: {} - {}", movieList.getMovieNm(), e.getMessage());
                }
            }
            
            // MovieDetail 엔티티 생성
            MovieDetail movieDetail = MovieDetail.builder()
                .movieCd(movieCd)
                .movieNm(movieList.getMovieNm())
                .movieNmEn(movieNmEn != null ? movieNmEn : "")
                .description(description)
                .openDt(movieList.getOpenDt())
                .showTm(showTm)
                .genreNm(genreNm != null ? genreNm : "")
                .nationNm(movieList.getNationNm())
                .watchGradeNm(movieList.getWatchGradeNm())
                .companyNm(companyNm)
                .totalAudience(0)
                .reservationRate(0.0)
                .averageRating(0.0)
                .status(movieList.getStatus())
                .build();
            
            if (director != null) {
                movieDetail.setDirector(director);
            }
            
            // 저장
            MovieDetail savedMovieDetail = movieRepository.save(movieDetail);
            log.info("KOBIS MovieDetail 저장 완료: {} ({}) - 영문제목: {}, 장르: {}", 
                savedMovieDetail.getMovieNm(), movieCd, movieNmEn, genreNm);
            
            return savedMovieDetail;
            
        } catch (Exception e) {
            log.error("KOBIS API로 MovieDetail 가져오기 실패: {} - {}", movieCd, e.getMessage());
            return null;
        }
    }

    /**
     * TMDB에서 영화 정보 가져오기 (영어 제목, 장르)
     */
    private String getTmdbMovieInfo(String movieNm, LocalDate openDt) {
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
                    String originalTitle = movie.has("original_title") ? movie.get("original_title").asText() : "";
                    
                    // 장르 정보
                    String genres = "";
                    if (movie.has("genre_ids") && movie.get("genre_ids").isArray()) {
                        List<String> genreNames = new ArrayList<>();
                        for (JsonNode genreId : movie.get("genre_ids")) {
                            String genreName = getGenreNameById(genreId.asInt());
                            if (!genreName.isEmpty()) {
                                genreNames.add(genreName);
                            }
                        }
                        genres = String.join(", ", genreNames);
                    }
                    
                    return originalTitle + "|" + genres;
                }
            }
        } catch (Exception e) {
            log.warn("TMDB 영화 정보 조회 실패: {} - {}", movieNm, e.getMessage());
        }
        
        return null;
    }

    /**
     * KOBIS에서 개봉예정작 가져오기
     */
    public List<MovieListDto> fetchComingSoonMovies(int limit) {
        List<MovieListDto> comingSoonMovies = new ArrayList<>();
        
        try {
            log.info("KOBIS에서 개봉예정작 가져오기 시작 (제한: {}개)", limit);
            
            // 현재 날짜부터 6개월 후까지의 개봉예정작 조회 (더 넓은 범위로 조회)
            java.time.LocalDate today = java.time.LocalDate.now();
            java.time.LocalDate endDate = today.plusMonths(6);
            
            String startDateStr = today.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
            String endDateStr = endDate.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
            
            log.info("개봉예정작 조회 기간: {} ~ {}", startDateStr, endDateStr);
            
            String url = String.format("http://www.kobis.or.kr/kobisopenapi/webservice/rest/movie/searchMovieList.json?key=%s&openStartDt=%s&openEndDt=%s&itemPerPage=%d", 
                kobisApiKey, startDateStr, endDateStr, limit);
            
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                log.error("KOBIS 개봉예정작 API 호출 실패: status={}", response.getStatusCode());
                return comingSoonMovies;
            }
            
            JsonNode rootNode = objectMapper.readTree(response.getBody());
            JsonNode movieListResult = rootNode.get("movieListResult");
            
            if (movieListResult == null || movieListResult.get("movieList") == null) {
                log.warn("KOBIS API 응답에 movieList가 없음");
                return comingSoonMovies;
            }
            
            JsonNode movieList = movieListResult.get("movieList");
            
            for (JsonNode movie : movieList) {
                try {
                    java.time.LocalDate openDt = parseKobisDate(movie.get("openDt").asText());
                    
                    // 개봉예정작 판별 로직 개선
                    if (openDt != null && isComingSoonMovie(openDt, today)) {
                        MovieListDto movieDto = MovieListDto.builder()
                            .movieCd(movie.get("movieCd").asText())
                            .movieNm(movie.get("movieNm").asText())
                            .movieNmEn(movie.has("movieNmEn") ? movie.get("movieNmEn").asText() : "")
                            .openDt(openDt)
                            .genreNm(movie.get("genreNm").asText())
                            .nationNm(movie.get("nationNm").asText())
                            .watchGradeNm(movie.get("watchGradeNm").asText())
                            .posterUrl("")
                            .status(MovieStatus.COMING_SOON)
                            .build();
                        
                        comingSoonMovies.add(movieDto);
                        log.debug("개봉예정작 추가: {} (개봉일: {})", movie.get("movieNm").asText(), openDt);
                    } else {
                        log.debug("개봉예정작 제외: {} (개봉일: {}) - 이미 개봉했거나 너무 먼 미래", 
                            movie.get("movieNm").asText(), openDt);
                    }
                    
                } catch (Exception e) {
                    log.warn("개봉예정작 파싱 실패: {}", e.getMessage());
                }
            }
            
            log.info("KOBIS에서 개봉예정작 {}개 가져오기 완료 (현재 날짜: {})", comingSoonMovies.size(), today);
            
        } catch (Exception e) {
            log.error("KOBIS 개봉예정작 가져오기 실패", e);
        }
        
        return comingSoonMovies;
    }

    /**
     * 개봉예정작 판별 로직
     * - 현재 날짜보다 미래이면서
     * - 6개월 이내에 개봉하는 영화만 개봉예정작으로 분류
     */
    private boolean isComingSoonMovie(java.time.LocalDate openDt, java.time.LocalDate today) {
        if (openDt == null) return false;
        
        // 현재 날짜보다 미래인지 확인
        if (!openDt.isAfter(today)) {
            return false;
        }
        
        // 6개월 이내에 개봉하는지 확인
        java.time.LocalDate sixMonthsLater = today.plusMonths(6);
        if (openDt.isAfter(sixMonthsLater)) {
            return false;
        }
        
        return true;
    }

    /**
     * TMDB에서 개봉예정작 가져오기
     */
    public List<MovieListDto> fetchComingSoonMoviesFromTmdb(int limit) {
        List<MovieListDto> comingSoonMovies = new ArrayList<>();
        
        try {
            log.info("TMDB에서 개봉예정작 가져오기 시작 (제한: {}개)", limit);
            
            // TMDB는 한 페이지당 20개씩 반환하므로 여러 페이지를 가져와야 함
            int page = 1;
            int maxPages = (limit + 19) / 20; // 올림 나눗셈으로 필요한 페이지 수 계산
            
            while (comingSoonMovies.size() < limit && page <= maxPages) {
                String url = String.format("https://api.themoviedb.org/3/movie/upcoming?api_key=%s&language=ko-KR&page=%d", 
                    tmdbApiKey, page);
                
                ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
                if (!response.getStatusCode().is2xxSuccessful()) {
                    log.error("TMDB 개봉예정작 API 호출 실패: status={}, page={}", response.getStatusCode(), page);
                    break;
                }
                
                JsonNode rootNode = objectMapper.readTree(response.getBody());
                JsonNode results = rootNode.get("results");
                
                if (results == null || results.size() == 0) {
                    log.info("TMDB 페이지 {}에 더 이상 데이터가 없습니다.", page);
                    break;
                }
                
                log.info("TMDB 페이지 {}에서 {}개 영화 처리 중...", page, results.size());
                
                for (JsonNode movie : results) {
                    if (comingSoonMovies.size() >= limit) break;
                    
                    try {
                        // 개봉일 파싱
                        java.time.LocalDate releaseDate = null;
                        if (movie.has("release_date") && !movie.get("release_date").isNull()) {
                            String releaseDateStr = movie.get("release_date").asText();
                            releaseDate = java.time.LocalDate.parse(releaseDateStr, java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                        }
                        
                        // 장르 정보
                        String genreNm = "";
                        if (movie.has("genre_ids") && movie.get("genre_ids").isArray()) {
                            // TMDB 장르 ID를 한국어 장르명으로 변환 (간단한 매핑)
                            List<String> genres = new ArrayList<>();
                            for (JsonNode genreId : movie.get("genre_ids")) {
                                String genreName = getGenreNameById(genreId.asInt());
                                if (!genreName.isEmpty()) {
                                    genres.add(genreName);
                                }
                            }
                            genreNm = String.join(", ", genres);
                        }
                        
                        // 고유한 movieCd 생성 (TMDB ID 기반)
                        String movieCd = "TMDB" + movie.get("id").asText();
                        
                        MovieListDto movieDto = MovieListDto.builder()
                            .movieCd(movieCd)
                            .movieNm(movie.get("title").asText())
                            .movieNmEn(movie.has("original_title") ? movie.get("original_title").asText() : "")
                            .openDt(releaseDate)
                            .genreNm(genreNm.isEmpty() ? "기타" : genreNm)
                            .nationNm("해외")
                            .watchGradeNm("전체관람가")
                            .posterUrl(movie.has("poster_path") && !movie.get("poster_path").isNull() ? 
                                "https://image.tmdb.org/t/p/w500" + movie.get("poster_path").asText() : "")
                            .status(MovieStatus.COMING_SOON)
                            .build();
                        
                        comingSoonMovies.add(movieDto);
                        
                    } catch (Exception e) {
                        log.warn("TMDB 개봉예정작 파싱 실패: {}", e.getMessage());
                    }
                }
                
                page++;
                
                // API 호출 제한을 위한 딜레이
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            
            log.info("TMDB에서 개봉예정작 {}개 가져오기 완료 ({}페이지 처리)", comingSoonMovies.size(), page - 1);
            
        } catch (Exception e) {
            log.error("TMDB 개봉예정작 가져오기 실패", e);
        }
        
        return comingSoonMovies;
    }

    /**
     * TMDB 장르 ID를 한국어 장르명으로 변환
     */
    private String getGenreNameById(int genreId) {
        switch (genreId) {
            case 28: return "액션";
            case 12: return "모험";
            case 16: return "애니메이션";
            case 35: return "코미디";
            case 80: return "범죄";
            case 99: return "다큐멘터리";
            case 18: return "드라마";
            case 10751: return "가족";
            case 14: return "판타지";
            case 36: return "역사";
            case 27: return "공포";
            case 10402: return "음악";
            case 9648: return "미스터리";
            case 10749: return "로맨스";
            case 878: return "SF";
            case 10770: return "TV 영화";
            case 53: return "스릴러";
            case 10752: return "전쟁";
            case 37: return "서부";
            default: return "";
        }
    }

    /**
     * KOBIS 날짜 형식(yyyyMMdd)을 LocalDate로 파싱
     */
    private java.time.LocalDate parseKobisDate(String dateStr) {
        try {
            return java.time.LocalDate.parse(dateStr, java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        } catch (Exception e) {
            log.warn("KOBIS 날짜 파싱 실패: {}", dateStr);
            return null;
        }
    }

    /**
     * movieCd로 MovieDetail 찾기
     */
    public java.util.Optional<MovieDetail> findMovieDetailByCode(String movieCd) {
        try {
            return movieRepository.findById(movieCd);
        } catch (Exception e) {
            log.warn("MovieDetail 조회 실패: {} - {}", movieCd, e.getMessage());
            return java.util.Optional.empty();
        }
    }

    /**
     * 기존 개봉예정작 데이터 정리 (과거 개봉 영화들을 상영중으로 변경)
     */
    public void cleanupComingSoonMovies() {
        try {
            log.info("=== 기존 개봉예정작 데이터 정리 시작 ===");
            
            java.time.LocalDate today = java.time.LocalDate.now();
            log.info("현재 날짜: {}", today);
            
            List<MovieList> allMovieLists = prdMovieListRepository.findAll();
            int updatedCount = 0;
            int skippedCount = 0;
            int comingSoonCount = 0;
            
            for (MovieList movieList : allMovieLists) {
                // 개봉예정작 상태인 영화만 확인
                if (MovieStatus.COMING_SOON.equals(movieList.getStatus())) {
                    java.time.LocalDate openDt = movieList.getOpenDt();
                    
                    if (openDt != null) {
                        // 개봉일이 지난 영화를 상영중으로 변경
                        if (!openDt.isAfter(today)) {
                            movieList.setStatus(MovieStatus.NOW_PLAYING);
                            prdMovieListRepository.save(movieList);
                            updatedCount++;
                            
                            log.info("개봉예정작 → 상영중 변경: {} (개봉일: {})", 
                                movieList.getMovieNm(), openDt);
                        } else {
                            // 여전히 개봉예정작인 영화
                            comingSoonCount++;
                            log.debug("개봉예정작 유지: {} (개봉일: {})", 
                                movieList.getMovieNm(), openDt);
                        }
                    } else {
                        // 개봉일이 없는 영화는 개봉예정작으로 유지
                        comingSoonCount++;
                        log.debug("개봉일 없는 개봉예정작 유지: {}", movieList.getMovieNm());
                    }
                } else {
                    skippedCount++;
                }
            }
            
            log.info("=== 기존 개봉예정작 데이터 정리 완료 ===");
            log.info("상태 변경: {}개, 개봉예정작 유지: {}개, 건너뜀: {}개", 
                updatedCount, comingSoonCount, skippedCount);
            
        } catch (Exception e) {
            log.error("개봉예정작 데이터 정리 실패", e);
        }
    }
} 

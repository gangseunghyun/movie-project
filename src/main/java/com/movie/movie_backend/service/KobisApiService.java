package com.movie.movie_backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.movie.movie_backend.entity.Director;
import com.movie.movie_backend.entity.MovieDetail;
import com.movie.movie_backend.entity.MovieList;
import com.movie.movie_backend.entity.Actor;
import com.movie.movie_backend.entity.Cast;
import com.movie.movie_backend.repository.PRDDirectorRepository;
import com.movie.movie_backend.repository.PRDMovieRepository;
import com.movie.movie_backend.repository.PRDMovieListRepository;
import com.movie.movie_backend.repository.PRDActorRepository;
import com.movie.movie_backend.repository.CastRepository;
import com.movie.movie_backend.dto.MovieListDto;
import com.movie.movie_backend.constant.MovieStatus;
import com.movie.movie_backend.constant.RoleType;
import com.movie.movie_backend.util.MovieTitleUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

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
    private final PRDActorRepository actorRepository;
    private final CastRepository castRepository;

    @Value("${tmdb.api.key}")
    private String tmdbApiKey;

    @Value("${kobis.api.key}")
    private String kobisApiKey;

    private static final String MOVIE_INFO_URL = "http://www.kobis.or.kr/kobisopenapi/webservice/rest/movie/searchMovieInfo.json";

    /**
     * MovieList에 있는 영화들의 한글제목/영문제목으로 TMDB에서 검색하여 MovieDetail 보완
     */
    public void fillMissingMovieDetailsFromTmdb() {
        try {
            log.info("=== TMDB로 MovieDetail 보완 시작 ===");
            
            // MovieList에서 MovieDetail이 없는 영화들 찾기
            List<MovieList> allMovieLists = getAllMovieListsChunked();
            List<String> missingMovieCds = new ArrayList<>();
            
            for (MovieList movieList : allMovieLists) {
                if (!movieRepository.existsByMovieCd(movieList.getMovieCd())) {
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

    private List<MovieList> getAllMovieListsChunked() {
        List<MovieList> allMovieLists = new ArrayList<>();
        int page = 0, size = 1000;
        Page<MovieList> moviePage;
        do {
            moviePage = prdMovieListRepository.findAll(PageRequest.of(page++, size));
            allMovieLists.addAll(moviePage.getContent());
        } while (moviePage.hasNext());
        return allMovieLists;
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
            
            // TMDB overview 필드 로깅 추가
            if (!overview.isEmpty()) {
                log.info("TMDB overview 필드 발견: 영화={}, overview 길이={}, overview 내용={}", 
                    movieList.getMovieNm(), overview.length(), 
                    overview.length() > 100 ? overview.substring(0, 100) + "..." : overview);
            } else {
                log.warn("TMDB overview 필드 없음: 영화={}, TMDB ID={}", movieList.getMovieNm(), tmdbId);
            }
            
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
                .averageRating(0.0) // TMDB 평점 저장 제거, 0.0으로 초기화
//                .status(movieList.getStatus())
                .build();
            
            // 감독 정보 저장
            if (!directorName.isEmpty()) {
                Director director = saveDirectorByName(directorName);
                movieDetail.setDirector(director);
            }
            
            // MovieDetail 저장
            MovieDetail savedMovieDetail = movieRepository.save(movieDetail);
            
            log.info("TMDB MovieDetail 저장 완료: {} ({}) - TMDB ID: {}", 
                savedMovieDetail.getMovieNm(), movieList.getMovieCd(), tmdbId);
            
            // TMDB description 저장 확인 로깅 추가
            log.info("TMDB description 저장 확인: 영화={}, description 길이={}, description 내용={}", 
                savedMovieDetail.getMovieNm(), 
                savedMovieDetail.getDescription() != null ? savedMovieDetail.getDescription().length() : 0,
                savedMovieDetail.getDescription() != null && savedMovieDetail.getDescription().length() > 100 
                    ? savedMovieDetail.getDescription().substring(0, 100) + "..." 
                    : savedMovieDetail.getDescription());
            
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
            // 1차 시도: 원본 이름으로 검색
            String photoUrl = searchDirectorImageFromTmdb(directorName);
            if (photoUrl != null && !photoUrl.isEmpty()) {
                log.debug("TMDB 감독 이미지 1차 시도 성공 (원본): {} -> {}", directorName, photoUrl);
                return photoUrl;
            }
            
            // 2차 시도: 영문 이름으로 검색 (영문 이름이 있는 경우)
            if (directorName != null && !directorName.isEmpty()) {
                // 영문 이름 추출 시도 (괄호 안 영문 이름)
                String englishName = MovieTitleUtil.extractEnglishTitle(directorName);
                if (englishName != null && !englishName.isEmpty()) {
                    photoUrl = searchDirectorImageFromTmdb(englishName);
                    if (photoUrl != null && !photoUrl.isEmpty()) {
                        log.debug("TMDB 감독 이미지 2차 시도 성공 (영문): {} -> {} (원본: {})", englishName, photoUrl, directorName);
                        return photoUrl;
                    }
                }
            }
            
            log.debug("TMDB 감독 이미지 검색 실패: {} (원본/영문 모두 시도)", directorName);
            
        } catch (Exception e) {
            log.warn("TMDB 감독 이미지 URL 조회 실패: {} - {}", directorName, e.getMessage());
        }
        
        return null;
    }

    /**
     * TMDB에서 감독 이미지 검색 (공통 로직)
     */
    private String searchDirectorImageFromTmdb(String name) {
        try {
            String encodedName = java.net.URLEncoder.encode(name, java.nio.charset.StandardCharsets.UTF_8);
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
            log.debug("TMDB 감독 이미지 검색 실패: {} - {}", name, e.getMessage());
        }
        
        return null;
    }

    /**
     * KOBIS API로 영화 상세정보 가져오기
     */
    public MovieDetail fetchAndSaveMovieDetail(String movieCd) {
        try {
            // TMDB_ 접두사가 붙은 영화는 처리하지 않음 (KOBIS 데이터만 사용)
            if (movieCd.startsWith("TMDB_")) {
                log.warn("TMDB 영화는 처리하지 않음: {} - KOBIS 데이터만 사용", movieCd);
                return null;
            }
            
            log.info("KOBIS API로 MovieDetail 가져오기 시작: {}", movieCd);
            
            // MovieList에서 기본 정보 가져오기 (API 호출 전에 확인)
            MovieList movieList = prdMovieListRepository.findById(movieCd).orElse(null);
            if (movieList == null) {
                log.warn("MovieList를 찾을 수 없음: {}", movieCd);
                return null;
            }
            
            log.info("KOBIS API 호출: 영화명={}, movieCd={}", movieList.getMovieNm(), movieCd);
            
            // KOBIS API 호출
            String url = String.format("%s?key=%s&movieCd=%s", MOVIE_INFO_URL, kobisApiKey, movieCd);
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            
            if (!response.getStatusCode().is2xxSuccessful()) {
                log.error("KOBIS API 호출 실패: status={}, movieCd={}", response.getStatusCode(), movieCd);
                return null;
            }
            
            // API 응답 로깅 (디버깅용)
            String responseBody = response.getBody();
            log.debug("KOBIS API 응답: {}", responseBody);
            
            // plot 필드 확인을 위한 응답 로깅 추가
            log.info("KOBIS API 응답 확인: 영화={}, movieCd={}, 응답 길이={}", 
                movieList.getMovieNm(), movieCd, responseBody != null ? responseBody.length() : 0);
            
            JsonNode rootNode = objectMapper.readTree(responseBody);
            JsonNode movieInfoResult = rootNode.get("movieInfoResult");
            
            if (movieInfoResult == null) {
                log.warn("KOBIS API 응답에 movieInfoResult가 없음: movieCd={}, 응답={}", movieCd, responseBody);
                return null;
            }
            
            JsonNode movieInfo = movieInfoResult.get("movieInfo");
            
            if (movieInfo == null) {
                log.warn("KOBIS API 응답에 movieInfo가 없음: movieCd={}, movieInfoResult={}", movieCd, movieInfoResult.toString());
                
                // KOBIS에서 실패했으므로 영화명으로 재검색 시도
                log.info("movieCd로 실패했으므로 영화명으로 재검색 시도: {}", movieList.getMovieNm());
                MovieDetail fallbackResult = searchMovieByTitleFromKobis(movieList);
                if (fallbackResult != null) {
                    log.info("영화명으로 KOBIS 검색 성공: {}", movieList.getMovieNm());
                    return fallbackResult;
                }
                
                // KOBIS에서 완전히 실패하면 null 반환 (TMDB fallback 제거)
                log.warn("KOBIS에서 완전히 실패: {} - TMDB fallback을 사용하지 않음", movieCd);
                return null;
            }
            
            log.info("KOBIS API에서 영화 정보 발견: {} ({})", movieList.getMovieNm(), movieCd);
            
            // KOBIS API 응답 전체 로깅 (plot 필드 확인용)
            log.info("KOBIS movieInfo 전체 응답: {}", movieInfo.toString());
            
            // 상세 정보 추출
            String description = "";
            if (movieInfo.has("plot") && !movieInfo.get("plot").isNull()) {
                description = movieInfo.get("plot").asText();
                log.info("KOBIS plot 필드 발견: 영화={}, plot 길이={}, plot 내용={}", 
                    movieList.getMovieNm(), description.length(), 
                    description.length() > 100 ? description.substring(0, 100) + "..." : description);
            } else {
                log.warn("KOBIS plot 필드 없음: 영화={}, movieCd={}", movieList.getMovieNm(), movieCd);
                // plot 필드가 없으면 다른 필드들도 확인
                log.info("KOBIS movieInfo에서 사용 가능한 필드들: {}", movieInfo.fieldNames());
                
                // KOBIS에서 줄거리가 없으면 TMDB에서 가져오기 시도
                log.info("KOBIS에서 줄거리가 없으므로 TMDB에서 줄거리 가져오기 시도: {}", movieList.getMovieNm());
                try {
                    String tmdbOverview = getTmdbOverview(movieList.getMovieNm(), movieList.getOpenDt());
                    if (tmdbOverview != null && !tmdbOverview.trim().isEmpty()) {
                        description = tmdbOverview;
                        log.info("TMDB에서 줄거리 가져오기 성공: 영화={}, 줄거리 길이={}", 
                            movieList.getMovieNm(), description.length());
                    } else {
                        log.warn("TMDB에서도 줄거리를 찾을 수 없음: {}", movieList.getMovieNm());
                    }
                } catch (Exception e) {
                    log.warn("TMDB 줄거리 가져오기 실패: {} - {}", movieList.getMovieNm(), e.getMessage());
                }
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
            
            // 관람등급 파싱 (audits[0].watchGradeNm)
            String watchGradeNm = movieList.getWatchGradeNm();
            if (movieInfo.has("audits") && movieInfo.get("audits").isArray()) {
                JsonNode audits = movieInfo.get("audits");
                if (audits.size() > 0 && audits.get(0).has("watchGradeNm")) {
                    String kobisGrade = audits.get(0).get("watchGradeNm").asText();
                    if (kobisGrade != null && !kobisGrade.isBlank()) {
                        watchGradeNm = kobisGrade;
                    }
                }
            }
            // 청소년관람불가 영화는 저장하지 않음 (공식 KOBIS 문서 기준)
            if ("청소년관람불가".equals(watchGradeNm)) {
                log.info("청소년관람불가 영화 필터링: {} {}", movieCd, movieList.getMovieNm());
                return null;
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
            
            // 배우 정보 (Actor, Cast)
            List<Actor> actors = new ArrayList<>();
            if (movieInfo.has("actors") && movieInfo.get("actors").isArray()) {
                JsonNode actorsNode = movieInfo.get("actors");
                log.info("KOBIS에서 배우 정보 {}개 발견: 영화={}", actorsNode.size(), movieList.getMovieNm());
                
                for (int i = 0; i < actorsNode.size() && i < 10; i++) { // 최대 10명까지만
                    JsonNode actorNode = actorsNode.get(i);
                    String actorName = actorNode.get("peopleNm").asText();
                    String characterName = actorNode.has("cast") ? actorNode.get("cast").asText() : "";
                    
                    // Actor 저장 또는 조회
                    Actor actor = saveActorByName(actorName);
                    if (actor != null) {
                        actors.add(actor);
                        log.debug("배우 추가: {} - 캐릭터: {}", actorName, characterName);
                    }
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
                .watchGradeNm(watchGradeNm)
                .companyNm(companyNm)
                .totalAudience(0)
                .reservationRate(0.0)
                .averageRating(0.0)
//                .status(movieList.getStatus())
                .build();
            
            if (director != null) {
                movieDetail.setDirector(director);
            }
            
            // 저장
            MovieDetail savedMovieDetail = movieRepository.save(movieDetail);
            log.info("KOBIS MovieDetail 저장 완료: {} ({}) - 영문제목: {}, 장르: {}", 
                savedMovieDetail.getMovieNm(), movieCd, movieNmEn, genreNm);
            
            // description 저장 확인 로깅 추가
            log.info("KOBIS description 저장 확인: 영화={}, description 길이={}, description 내용={}", 
                savedMovieDetail.getMovieNm(), 
                savedMovieDetail.getDescription() != null ? savedMovieDetail.getDescription().length() : 0,
                savedMovieDetail.getDescription() != null && savedMovieDetail.getDescription().length() > 100 
                    ? savedMovieDetail.getDescription().substring(0, 100) + "..." 
                    : savedMovieDetail.getDescription());
            
            // Cast 정보 저장
            if (!actors.isEmpty()) {
                saveCastInfo(savedMovieDetail, actors, movieInfo);
            }
            
            return savedMovieDetail;
            
        } catch (Exception e) {
            log.error("KOBIS API로 MovieDetail 가져오기 실패: {} - {}", movieCd, e.getMessage());
            
            // 예외 발생 시에도 TMDB fallback 제거 - KOBIS 데이터만 사용
            log.warn("KOBIS API 호출 중 예외 발생: {} - TMDB fallback을 사용하지 않음", movieCd);
            return null;
        }
    }

    /**
     * TMDB에서 영화 정보 가져오기 (영어 제목, 장르)
     */
    private String getTmdbMovieInfo(String movieNm, LocalDate openDt) {
        try {
            // 1차 시도: 한글 제목으로 검색
            String movieInfo = searchMovieInfoFromTmdb(movieNm, openDt);
            if (movieInfo != null && !movieInfo.isEmpty()) {
                log.debug("TMDB 영화 정보 1차 시도 성공 (한글): {} -> {}", movieNm, movieInfo);
                return movieInfo;
            }
            
            // 2차 시도: 영문 제목으로 검색 (영문 제목이 있는 경우)
            if (movieNm != null && !movieNm.isEmpty()) {
                // 영문 제목 추출 시도 (괄호 안 영문 제목)
                String englishTitle = MovieTitleUtil.extractEnglishTitle(movieNm);
                if (englishTitle != null && !englishTitle.isEmpty()) {
                    movieInfo = searchMovieInfoFromTmdb(englishTitle, openDt);
                    if (movieInfo != null && !movieInfo.isEmpty()) {
                        log.debug("TMDB 영화 정보 2차 시도 성공 (영문): {} -> {} (원본: {})", englishTitle, movieInfo, movieNm);
                        return movieInfo;
                    }
                }
            }
            
            log.debug("TMDB 영화 정보 검색 실패: {} (한글/영문 모두 시도)", movieNm);
            
        } catch (Exception e) {
            log.warn("TMDB 영화 정보 조회 실패: {} - {}", movieNm, e.getMessage());
        }
        
        return null;
    }

    /**
     * TMDB에서 영화 정보 검색 (공통 로직)
     */
    private String searchMovieInfoFromTmdb(String title, LocalDate openDt) {
        try {
            String query = java.net.URLEncoder.encode(title, java.nio.charset.StandardCharsets.UTF_8);
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
            log.debug("TMDB 영화 정보 검색 실패: {} - {}", title, e.getMessage());
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
            
            // 현재 날짜부터 6개월 후까지의 개봉예정작 조회
            java.time.LocalDate today = java.time.LocalDate.now();
            java.time.LocalDate endDate = today.plusMonths(6);
            
            String startDateStr = today.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
            String endDateStr = endDate.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
            
            log.info("개봉예정작 조회 기간: {} ~ {}", startDateStr, endDateStr);
            
            // KOBIS 개봉예정작 API URL - 더 정확한 방법으로 수정
            String url = String.format("http://www.kobis.or.kr/kobisopenapi/webservice/rest/movie/searchMovieList.json?key=%s&openStartDt=%s&openEndDt=%s&itemPerPage=%d&repNationCd=K&movieTypeCd=220101", 
                kobisApiKey, startDateStr, endDateStr, limit);
            
            log.info("KOBIS API 호출 URL: {}", url);
            
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                log.error("KOBIS 개봉예정작 API 호출 실패: status={}", response.getStatusCode());
                return comingSoonMovies;
            }
            
            // API 응답 로깅 (디버깅용)
            log.debug("KOBIS API 응답: {}", response.getBody());
            
            JsonNode rootNode = objectMapper.readTree(response.getBody());
            
            // KOBIS API 응답 구조 확인
            if (rootNode.has("faultInfo")) {
                log.error("KOBIS API 오류: {}", rootNode.get("faultInfo").toPrettyString());
                return comingSoonMovies;
            }
            
            JsonNode movieListResult = rootNode.get("movieListResult");
            
            if (movieListResult == null) {
                log.warn("KOBIS API 응답에 movieListResult가 없음");
                log.warn("전체 응답 구조: {}", rootNode.toPrettyString());
                return comingSoonMovies;
            }
            
            JsonNode movieList = movieListResult.get("movieList");
            
            if (movieList == null) {
                log.warn("KOBIS API 응답에 movieList가 없음");
                log.warn("movieListResult 구조: {}", movieListResult.toPrettyString());
                return comingSoonMovies;
            }
            
            log.info("KOBIS에서 {}개의 영화 데이터를 받았습니다.", movieList.size());
            
            for (JsonNode movie : movieList) {
                try {
                    java.time.LocalDate openDt = parseKobisDate(movie.get("openDt").asText());
                    
                    // 개봉예정작 판별 로직 개선
                    if (openDt != null && isComingSoonMovie(openDt, today)) {
                        String movieCd = movie.get("movieCd").asText();
                        
                        // MovieList에 저장
                        MovieListDto movieDto = MovieListDto.builder()
                            .movieCd(movieCd)
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
                        log.info("개봉예정작 추가: {} (개봉일: {})", movie.get("movieNm").asText(), openDt);
                        
                        // MovieDetail도 함께 생성 (상세정보가 없는 경우)
                        try {
                            if (movieRepository.findByMovieCd(movieCd).isEmpty()) {
                                log.info("MovieDetail 생성 시작: {}", movieCd);
                                fetchAndSaveMovieDetail(movieCd);
                                log.info("MovieDetail 생성 완료: {}", movieCd);
                            }
                        } catch (Exception e) {
                            log.warn("MovieDetail 생성 실패: {} - {}", movieCd, e.getMessage());
                        }
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
    public Optional<MovieDetail> getMovieDetail(String movieCd) {
        return movieRepository.findByMovieCd(movieCd);
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

    /**
     * 영화명으로 KOBIS에서 검색하여 MovieDetail 가져오기
     */
    private MovieDetail searchMovieByTitleFromKobis(MovieList movieList) {
        try {
            log.info("KOBIS 영화명 검색 시작: {}", movieList.getMovieNm());
            
            // KOBIS 영화목록 API에서 영화명으로 검색
            String encodedTitle = java.net.URLEncoder.encode(movieList.getMovieNm(), java.nio.charset.StandardCharsets.UTF_8);
            String url = String.format("http://www.kobis.or.kr/kobisopenapi/webservice/rest/movie/searchMovieList.json?key=%s&movieNm=%s", 
                kobisApiKey, encodedTitle);
            
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                log.warn("KOBIS 영화명 검색 API 호출 실패: status={}", response.getStatusCode());
                return null;
            }
            
            JsonNode rootNode = objectMapper.readTree(response.getBody());
            JsonNode movieListResult = rootNode.get("movieListResult");
            
            if (movieListResult == null || movieListResult.get("movieList") == null) {
                log.warn("KOBIS 영화명 검색 결과가 없음: {}", movieList.getMovieNm());
                return null;
            }
            
            JsonNode movies = movieListResult.get("movieList");
            
            // 가장 정확한 매칭 찾기
            for (JsonNode movie : movies) {
                String foundMovieCd = movie.get("movieCd").asText();
                String foundMovieNm = movie.get("movieNm").asText();
                
                // 제목이 정확히 일치하거나 포함되는 경우
                if (foundMovieNm.equals(movieList.getMovieNm()) || 
                    foundMovieNm.contains(movieList.getMovieNm()) || 
                    movieList.getMovieNm().contains(foundMovieNm)) {
                    
                    log.info("KOBIS에서 매칭된 영화 발견: {} -> {} ({})", 
                        movieList.getMovieNm(), foundMovieNm, foundMovieCd);
                    
                    // 찾은 movieCd로 상세정보 직접 가져오기 (재귀 호출 방지)
                    return fetchMovieDetailByMovieCd(foundMovieCd, movieList);
                }
            }
            
            log.warn("KOBIS에서 매칭되는 영화를 찾을 수 없음: {}", movieList.getMovieNm());
            return null;
            
        } catch (Exception e) {
            log.warn("KOBIS 영화명 검색 실패: {} - {}", movieList.getMovieNm(), e.getMessage());
            return null;
        }
    }

    /**
     * movieCd로 KOBIS 상세정보 직접 가져오기 (재귀 호출 방지용)
     */
    private MovieDetail fetchMovieDetailByMovieCd(String movieCd, MovieList movieList) {
        try {
            log.info("KOBIS 상세정보 직접 가져오기: {} ({})", movieList.getMovieNm(), movieCd);
            
            // KOBIS API 호출
            String url = String.format("%s?key=%s&movieCd=%s", MOVIE_INFO_URL, kobisApiKey, movieCd);
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            
            if (!response.getStatusCode().is2xxSuccessful()) {
                log.error("KOBIS API 호출 실패: status={}, movieCd={}", response.getStatusCode(), movieCd);
                return null;
            }
            
            JsonNode rootNode = objectMapper.readTree(response.getBody());
            JsonNode movieInfoResult = rootNode.get("movieInfoResult");
            
            if (movieInfoResult == null) {
                log.warn("KOBIS API 응답에 movieInfoResult가 없음: movieCd={}", movieCd);
                return null;
            }
            
            JsonNode movieInfo = movieInfoResult.get("movieInfo");
            
            if (movieInfo == null) {
                log.warn("KOBIS API 응답에 movieInfo가 없음: movieCd={}", movieCd);
                return null;
            }
            
            // KOBIS API 응답 전체 로깅 (plot 필드 확인용)
            log.info("KOBIS movieInfo 전체 응답: {}", movieInfo.toString());
            
            // 상세 정보 추출
            String description = "";
            if (movieInfo.has("plot") && !movieInfo.get("plot").isNull()) {
                description = movieInfo.get("plot").asText();
                log.info("KOBIS plot 필드 발견: 영화={}, plot 길이={}, plot 내용={}", 
                    movieList.getMovieNm(), description.length(), 
                    description.length() > 100 ? description.substring(0, 100) + "..." : description);
            } else {
                log.warn("KOBIS plot 필드 없음: 영화={}, movieCd={}", movieList.getMovieNm(), movieCd);
                // plot 필드가 없으면 다른 필드들도 확인
                log.info("KOBIS movieInfo에서 사용 가능한 필드들: {}", movieInfo.fieldNames());
                
                // KOBIS에서 줄거리가 없으면 TMDB에서 가져오기 시도
                log.info("KOBIS에서 줄거리가 없으므로 TMDB에서 줄거리 가져오기 시도: {}", movieList.getMovieNm());
                try {
                    String tmdbOverview = getTmdbOverview(movieList.getMovieNm(), movieList.getOpenDt());
                    if (tmdbOverview != null && !tmdbOverview.trim().isEmpty()) {
                        description = tmdbOverview;
                        log.info("TMDB에서 줄거리 가져오기 성공: 영화={}, 줄거리 길이={}", 
                            movieList.getMovieNm(), description.length());
                    } else {
                        log.warn("TMDB에서도 줄거리를 찾을 수 없음: {}", movieList.getMovieNm());
                    }
                } catch (Exception e) {
                    log.warn("TMDB 줄거리 가져오기 실패: {} - {}", movieList.getMovieNm(), e.getMessage());
                }
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
            
            // 관람등급 파싱 (audits[0].watchGradeNm)
            String watchGradeNm = movieList.getWatchGradeNm();
            if (movieInfo.has("audits") && movieInfo.get("audits").isArray()) {
                JsonNode audits = movieInfo.get("audits");
                if (audits.size() > 0 && audits.get(0).has("watchGradeNm")) {
                    String kobisGrade = audits.get(0).get("watchGradeNm").asText();
                    if (kobisGrade != null && !kobisGrade.isBlank()) {
                        watchGradeNm = kobisGrade;
                    }
                }
            }
            // 청소년관람불가 영화는 저장하지 않음 (공식 KOBIS 문서 기준)
            if ("청소년관람불가".equals(watchGradeNm)) {
                log.info("청소년관람불가 영화 필터링: {} {}", movieCd, movieList.getMovieNm());
                return null;
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
            
            // 배우 정보 (Actor, Cast)
            List<Actor> actors = new ArrayList<>();
            if (movieInfo.has("actors") && movieInfo.get("actors").isArray()) {
                JsonNode actorsNode = movieInfo.get("actors");
                log.info("KOBIS에서 배우 정보 {}개 발견: 영화={}", actorsNode.size(), movieList.getMovieNm());
                
                for (int i = 0; i < actorsNode.size() && i < 10; i++) { // 최대 10명까지만
                    JsonNode actorNode = actorsNode.get(i);
                    String actorName = actorNode.get("peopleNm").asText();
                    String characterName = actorNode.has("cast") ? actorNode.get("cast").asText() : "";
                    
                    // Actor 저장 또는 조회
                    Actor actor = saveActorByName(actorName);
                    if (actor != null) {
                        actors.add(actor);
                        log.debug("배우 추가: {} - 캐릭터: {}", actorName, characterName);
                    }
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
                .watchGradeNm(watchGradeNm)
                .companyNm(companyNm)
                .totalAudience(0)
                .reservationRate(0.0)
                .averageRating(0.0)
//                .status(movieList.getStatus())
                .build();
            
            if (director != null) {
                movieDetail.setDirector(director);
            }
            
            // 저장
            MovieDetail savedMovieDetail = movieRepository.save(movieDetail);
            log.info("KOBIS MovieDetail 저장 완료: {} ({})", savedMovieDetail.getMovieNm(), movieCd);
            
            // description 저장 확인 로깅 추가
            log.info("KOBIS description 저장 확인: 영화={}, description 길이={}, description 내용={}", 
                savedMovieDetail.getMovieNm(), 
                savedMovieDetail.getDescription() != null ? savedMovieDetail.getDescription().length() : 0,
                savedMovieDetail.getDescription() != null && savedMovieDetail.getDescription().length() > 100 
                    ? savedMovieDetail.getDescription().substring(0, 100) + "..." 
                    : savedMovieDetail.getDescription());
            
            // Cast 정보 저장
            if (!actors.isEmpty()) {
                saveCastInfo(savedMovieDetail, actors, movieInfo);
            }
            
            return savedMovieDetail;
            
        } catch (Exception e) {
            log.error("KOBIS 상세정보 직접 가져오기 실패: {} - {}", movieCd, e.getMessage());
            return null;
        }
    }

    private void saveCastInfo(MovieDetail movieDetail, List<Actor> actors, JsonNode movieInfo) {
        JsonNode actorsNode = movieInfo.get("actors");
        
        for (int i = 0; i < actors.size() && i < actorsNode.size(); i++) {
            Actor actor = actors.get(i);
            JsonNode actorNode = actorsNode.get(i);
            String characterName = actorNode.has("cast") ? actorNode.get("cast").asText() : "";
            
            // 주연/조연 구분: 상위 3명은 주연, 나머지는 조연
            RoleType roleType = (i < 3) ? RoleType.LEAD : RoleType.SUPPORTING;
            
            Cast cast = Cast.builder()
                .movieDetail(movieDetail)
                .actor(actor)
                .characterName(characterName)
                .orderInCredits(i + 1)
                .roleType(roleType)
                .build();
            
            castRepository.save(cast);
            log.debug("Cast 저장: 영화={}, 배우={}, 역할={}, 캐릭터={}", 
                movieDetail.getMovieNm(), actor.getName(), roleType, characterName);
        }
        
        log.info("KOBIS Cast 정보 저장 완료: 영화={}, 배우 수={}", movieDetail.getMovieNm(), actors.size());
    }

    private Actor saveActorByName(String actorName) {
        // 기존 배우가 있는지 확인
        Optional<Actor> existingActor = actorRepository.findByName(actorName);
        
        if (existingActor.isPresent()) {
            return existingActor.get();
        }

        // 새 배우 생성 (TMDB에서 이미지 URL 조회)
        String photoUrl = fetchActorImageUrlFromTmdb(actorName);
        Actor actor = Actor.builder()
                .name(actorName)
                .photoUrl(photoUrl)
                .build();
        
        return actorRepository.save(actor);
    }

    private String fetchActorImageUrlFromTmdb(String actorName) {
        try {
            // 1차 시도: 원본 이름으로 검색
            String photoUrl = searchActorImageFromTmdb(actorName);
            if (photoUrl != null && !photoUrl.isEmpty()) {
                log.debug("TMDB 배우 이미지 1차 시도 성공 (원본): {} -> {}", actorName, photoUrl);
                return photoUrl;
            }
            
            // 2차 시도: 영문 이름으로 검색 (영문 이름이 있는 경우)
            if (actorName != null && !actorName.isEmpty()) {
                // 영문 이름 추출 시도 (괄호 안 영문 이름)
                String englishName = MovieTitleUtil.extractEnglishTitle(actorName);
                if (englishName != null && !englishName.isEmpty()) {
                    photoUrl = searchActorImageFromTmdb(englishName);
                    if (photoUrl != null && !photoUrl.isEmpty()) {
                        log.debug("TMDB 배우 이미지 2차 시도 성공 (영문): {} -> {} (원본: {})", englishName, photoUrl, actorName);
                        return photoUrl;
                    }
                }
            }
            
            log.debug("TMDB 배우 이미지 검색 실패: {} (원본/영문 모두 시도)", actorName);
            
        } catch (Exception e) {
            log.warn("TMDB 배우 이미지 URL 조회 실패: {} - {}", actorName, e.getMessage());
        }
        
        return null;
    }

    /**
     * TMDB에서 배우 이미지 검색 (공통 로직)
     */
    private String searchActorImageFromTmdb(String name) {
        try {
            String encodedName = java.net.URLEncoder.encode(name, java.nio.charset.StandardCharsets.UTF_8);
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
            log.debug("TMDB 배우 이미지 검색 실패: {} - {}", name, e.getMessage());
        }
        
        return null;
    }

    /**
     * TMDB에서 영화 줄거리(overview) 가져오기
     */
    private String getTmdbOverview(String movieNm, LocalDate openDt) {
        try {
            // 1차 시도: 한글 제목으로 검색
            String overview = searchOverviewFromTmdb(movieNm, openDt);
            if (overview != null && !overview.isEmpty()) {
                log.info("TMDB에서 overview 발견 (한글): 영화={}, overview 길이={}", movieNm, overview.length());
                return overview;
            }
            
            // 2차 시도: 영문 제목으로 검색 (영문 제목이 있는 경우)
            if (movieNm != null && !movieNm.isEmpty()) {
                // 영문 제목 추출 시도 (괄호 안 영문 제목)
                String englishTitle = MovieTitleUtil.extractEnglishTitle(movieNm);
                if (englishTitle != null && !englishTitle.isEmpty()) {
                    overview = searchOverviewFromTmdb(englishTitle, openDt);
                    if (overview != null && !overview.isEmpty()) {
                        log.info("TMDB에서 overview 발견 (영문): 영화={}, overview 길이={} (원본: {})", englishTitle, overview.length(), movieNm);
                        return overview;
                    }
                }
            }
            
            log.warn("TMDB에서 overview를 찾을 수 없음: 영화={} (한글/영문 모두 시도)", movieNm);
            
        } catch (Exception e) {
            log.warn("TMDB overview 조회 실패: {} - {}", movieNm, e.getMessage());
        }
        
        return null;
    }

    /**
     * TMDB에서 overview 검색 (공통 로직)
     */
    private String searchOverviewFromTmdb(String title, LocalDate openDt) {
        try {
            String query = java.net.URLEncoder.encode(title, java.nio.charset.StandardCharsets.UTF_8);
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
                    
                    if (!overview.isEmpty()) {
                        return overview;
                    }
                }
            }
        } catch (Exception e) {
            log.debug("TMDB overview 검색 실패: {} - {}", title, e.getMessage());
        }
        
        return null;
    }

    // extractEnglishTitle 메서드는 MovieTitleUtil로 이동됨

    // 제작국가명/관람등급명만 빠르게 가져오는 용도
    public static class NationAndGrade {
        public final String nationNm;
        public final String watchGradeNm;
        public NationAndGrade(String nationNm, String watchGradeNm) {
            this.nationNm = nationNm;
            this.watchGradeNm = watchGradeNm;
        }
    }

    public NationAndGrade fetchNationAndGrade(String movieCd) {
        try {
            String url = String.format("http://www.kobis.or.kr/kobisopenapi/webservice/rest/movie/searchMovieInfo.json?key=%s&movieCd=%s", kobisApiKey, movieCd);
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            JsonNode rootNode = objectMapper.readTree(response.getBody());
            JsonNode movieInfo = rootNode.path("movieInfoResult").path("movieInfo");
            String nationNm = "";
            if (movieInfo.has("nations") && movieInfo.get("nations").isArray() && movieInfo.get("nations").size() > 0) {
                nationNm = movieInfo.get("nations").get(0).path("nationNm").asText("");
            }
            String watchGradeNm = "";
            if (movieInfo.has("audits") && movieInfo.get("audits").isArray() && movieInfo.get("audits").size() > 0) {
                watchGradeNm = movieInfo.get("audits").get(0).path("watchGradeNm").asText("");
            }
            return new NationAndGrade(nationNm, watchGradeNm);
        } catch (Exception e) {
            log.warn("KOBIS 국가/등급 파싱 실패: movieCd={}, {}", movieCd, e.getMessage());
            return null;
        }
    }
} 

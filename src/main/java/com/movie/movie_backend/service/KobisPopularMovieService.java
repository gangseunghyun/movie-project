package com.movie.movie_backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.movie.movie_backend.dto.MovieListDto;
import com.movie.movie_backend.entity.MovieDetail;
import com.movie.movie_backend.entity.Director;
import com.movie.movie_backend.entity.Actor;
import com.movie.movie_backend.entity.Cast;
import com.movie.movie_backend.constant.MovieStatus;
import com.movie.movie_backend.constant.RoleType;
import com.movie.movie_backend.repository.PRDMovieRepository;
import com.movie.movie_backend.repository.PRDDirectorRepository;
import com.movie.movie_backend.repository.PRDActorRepository;
import com.movie.movie_backend.repository.CastRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class KobisPopularMovieService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final KobisApiService kobisApiService;
    private final TmdbPopularMovieService tmdbPopularMovieService;
    private final NaverMovieService naverMovieService;
    private final PRDMovieRepository movieRepository;
    private final PRDDirectorRepository directorRepository;
    private final PRDActorRepository actorRepository;
    private final CastRepository castRepository;

    @Value("${kobis.api.key}")
    private String kobisApiKey;

    private static final String BASE_URL = "http://www.kobis.or.kr/kobisopenapi/webservice/rest";
    private static final String DAILY_BOX_OFFICE_URL = BASE_URL + "/boxoffice/searchDailyBoxOfficeList.json";
    private static final String WEEKLY_BOX_OFFICE_URL = BASE_URL + "/boxoffice/searchWeeklyBoxOfficeList.json";

    /**
     * KOBIS 박스오피스 TOP-100 영화를 MovieListDto로 가져오기
     */
    public List<MovieListDto> getPopularMoviesFromBoxOffice(int limit) {
        List<MovieListDto> popularMovies = new ArrayList<>();
        
        try {
            // 최근 30주간의 박스오피스 데이터를 수집 (주간 TOP-10씩)
            // 중복 제거 후 100개를 확보하기 위해 더 많은 주간 데이터 수집
            for (int week = 0; week < 30 && popularMovies.size() < limit; week++) {
                LocalDate targetDate = LocalDate.now().minusWeeks(week);
                String dateStr = targetDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
                
                String url = String.format("%s?key=%s&targetDt=%s&weekGb=0", 
                    WEEKLY_BOX_OFFICE_URL, kobisApiKey, dateStr);
                
                log.info("KOBIS 주간 박스오피스 API 호출: week={}, date={}, 현재 수집된 영화: {}개", 
                    week, dateStr, popularMovies.size());
                String response = restTemplate.getForObject(url, String.class);
                JsonNode root = objectMapper.readTree(response);
                JsonNode boxOfficeResult = root.get("boxOfficeResult");
                
                if (boxOfficeResult != null && boxOfficeResult.get("weeklyBoxOfficeList") != null) {
                    JsonNode weeklyBoxOfficeList = boxOfficeResult.get("weeklyBoxOfficeList");
                    
                    for (JsonNode movie : weeklyBoxOfficeList) {
                        if (popularMovies.size() >= limit) break;
                        
                        try {
                            MovieListDto movieDto = convertBoxOfficeToMovieListDto(movie);
                            if (movieDto != null && !isDuplicateMovie(popularMovies, movieDto)) {
                                popularMovies.add(movieDto);
                                log.info("박스오피스 영화 추가: {} ({}) - 순위: {}, 총 {}개", 
                                    movieDto.getMovieNm(), movieDto.getMovieCd(), 
                                    movie.get("rank").asText(), popularMovies.size());
                            } else if (movieDto != null) {
                                log.debug("중복 영화 건너뛰기: {} ({})", movieDto.getMovieNm(), movieDto.getMovieCd());
                            }
                        } catch (Exception e) {
                            log.warn("박스오피스 영화 변환 실패: {}", movie.get("movieNm"), e);
                        }
                    }
                }
                
                // API 호출 제한 고려
                Thread.sleep(200);
            }
            
            log.info("KOBIS 박스오피스 영화 {}개 가져오기 완료 (목표: {}개)", popularMovies.size(), limit);
            
            // 만약 100개에 못 미치면 추가로 일일 박스오피스도 수집
            if (popularMovies.size() < limit) {
                log.info("주간 박스오피스로 {}개만 수집되어 일일 박스오피스 추가 수집 시작", popularMovies.size());
                int additionalNeeded = limit - popularMovies.size();
                
                // 최근 20일간의 일일 박스오피스 추가 수집
                for (int day = 0; day < 20 && popularMovies.size() < limit; day++) {
                    LocalDate targetDate = LocalDate.now().minusDays(day);
                    String dateStr = targetDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
                    
                    String url = String.format("%s?key=%s&targetDt=%s", 
                        DAILY_BOX_OFFICE_URL, kobisApiKey, dateStr);
                    
                    log.info("KOBIS 일일 박스오피스 API 호출: day={}, date={}, 현재 수집된 영화: {}개", 
                        day, dateStr, popularMovies.size());
                    String response = restTemplate.getForObject(url, String.class);
                    JsonNode root = objectMapper.readTree(response);
                    JsonNode boxOfficeResult = root.get("boxOfficeResult");
                    
                    if (boxOfficeResult != null && boxOfficeResult.get("dailyBoxOfficeList") != null) {
                        JsonNode dailyBoxOfficeList = boxOfficeResult.get("dailyBoxOfficeList");
                        
                        for (JsonNode movie : dailyBoxOfficeList) {
                            if (popularMovies.size() >= limit) break;
                            
                            try {
                                MovieListDto movieDto = convertBoxOfficeToMovieListDto(movie);
                                if (movieDto != null && !isDuplicateMovie(popularMovies, movieDto)) {
                                    popularMovies.add(movieDto);
                                    log.info("일일 박스오피스 영화 추가: {} ({}) - 순위: {}, 총 {}개", 
                                        movieDto.getMovieNm(), movieDto.getMovieCd(), 
                                        movie.get("rank").asText(), popularMovies.size());
                                }
                            } catch (Exception e) {
                                log.warn("일일 박스오피스 영화 변환 실패: {}", movie.get("movieNm"), e);
                            }
                        }
                    }
                    
                    Thread.sleep(200);
                }
            }
            
            log.info("최종 KOBIS 박스오피스 영화 {}개 가져오기 완료 (목표: {}개)", popularMovies.size(), limit);
            return popularMovies;
            
        } catch (Exception e) {
            log.error("KOBIS 박스오피스 영화 가져오기 실패", e);
            return new ArrayList<>();
        }
    }

    /**
     * 박스오피스 데이터를 MovieListDto로 변환
     */
    private MovieListDto convertBoxOfficeToMovieListDto(JsonNode movie) {
        try {
            String movieCd = movie.get("movieCd").asText();
            String movieNm = movie.get("movieNm").asText();
            String openDt = movie.has("openDt") ? movie.get("openDt").asText() : "";
            
            // 날짜 파싱 (KOBIS API는 yyyy-MM-dd 형식으로 반환)
            LocalDate openDate = null;
            if (!openDt.isEmpty()) {
                try {
                    // yyyy-MM-dd 형식으로 파싱 시도
                    openDate = LocalDate.parse(openDt, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                } catch (Exception e1) {
                    try {
                        // yyyyMMdd 형식으로 파싱 시도
                        openDate = LocalDate.parse(openDt, DateTimeFormatter.ofPattern("yyyyMMdd"));
                    } catch (Exception e2) {
                        log.warn("날짜 파싱 실패: {} - yyyy-MM-dd와 yyyyMMdd 모두 실패", openDt);
                    }
                }
            }
            
            return MovieListDto.builder()
                    .movieCd(movieCd)
                    .movieNm(movieNm)
                    .movieNmEn("") // KOBIS에는 영문 제목이 없음
                    .openDt(openDate)
                    .genreNm("") // 나중에 상세정보에서 가져옴
                    .nationNm("") // 나중에 상세정보에서 가져옴
                    .build();
                    
        } catch (Exception e) {
            log.warn("박스오피스 영화 변환 실패", e);
            return null;
        }
    }

    /**
     * 중복 영화 체크
     */
    private boolean isDuplicateMovie(List<MovieListDto> movies, MovieListDto newMovie) {
        return movies.stream()
                .anyMatch(movie -> movie.getMovieCd().equals(newMovie.getMovieCd()));
    }

    /**
     * 영화 상세정보 가져오기 (KOBIS 우선, 실패시 TMDB)
     */
    public MovieDetail getMovieDetailWithFallback(String movieCd, String movieNm) {
        try {
            // 1. KOBIS에서 상세정보 가져오기 시도 (Actor, Cast, Director 포함)
            log.info("KOBIS에서 상세정보 가져오기 시도: {} ({})", movieNm, movieCd);
            MovieDetail kobisDetail = kobisApiService.fetchAndSaveMovieDetail(movieCd);
            
            if (kobisDetail != null) {
                log.info("KOBIS 상세정보 가져오기 성공: {} ({}) - Actor, Cast, Director 포함", movieNm, movieCd);
                return kobisDetail;
            }
            
        } catch (Exception e) {
            log.warn("KOBIS 상세정보 가져오기 실패: {} ({}) - {}", movieNm, movieCd, e.getMessage());
        }
        
        // 2. KOBIS에서 실패하면 TMDB에서 시도 (Actor, Cast, Director 포함)
        try {
            log.info("TMDB에서 상세정보 가져오기 시도: {} ({})", movieNm, movieCd);
            
            // TMDB에서 영화 검색
            String searchUrl = String.format("https://api.themoviedb.org/3/search/movie?api_key=%s&query=%s&language=ko-KR",
                    tmdbPopularMovieService.getTmdbApiKey(), movieNm);
            
            String response = restTemplate.getForObject(searchUrl, String.class);
            JsonNode root = objectMapper.readTree(response);
            JsonNode results = root.get("results");
            
            if (results != null && results.isArray() && results.size() > 0) {
                // 첫 번째 검색 결과 사용
                JsonNode firstResult = results.get(0);
                String tmdbId = firstResult.get("id").asText();
                
                // TMDB 상세정보 가져오기 (credits 포함)
                var tmdbDetailDto = tmdbPopularMovieService.getMovieDetailFromTmdb(tmdbId);
                
                if (tmdbDetailDto != null) {
                    // TMDB 데이터를 MovieDetail 엔티티로 변환
                    MovieDetail tmdbDetail = MovieDetail.builder()
                            .movieCd(movieCd) // 원래 KOBIS movieCd 사용
                            .movieNm(tmdbDetailDto.getMovieNm())
                            .movieNmEn(tmdbDetailDto.getMovieNmEn())
                            .description(tmdbDetailDto.getDescription())
                            .openDt(tmdbDetailDto.getOpenDt())
                            .showTm(tmdbDetailDto.getShowTm())
                            .genreNm(tmdbDetailDto.getGenreNm())
                            .nationNm(tmdbDetailDto.getNationNm())
                            .watchGradeNm(tmdbDetailDto.getWatchGradeNm())
                            .companyNm(tmdbDetailDto.getCompanyNm())
                            .totalAudience(tmdbDetailDto.getTotalAudience())
                            .reservationRate(tmdbDetailDto.getReservationRate())
                            .averageRating(tmdbDetailDto.getAverageRating())
                            .status(MovieStatus.NOW_PLAYING)
                            .build();
                    
                    // TMDB에서 감독과 배우 정보 가져오기
                    MovieDetail savedMovieDetail = saveTmdbCreditsToMovieDetail(tmdbDetail, tmdbId);
                    
                    log.info("TMDB 상세정보 가져오기 성공: {} ({}) - Actor, Cast, Director 포함", movieNm, movieCd);
                    return savedMovieDetail;
                }
            }
            
        } catch (Exception e) {
            log.warn("TMDB 상세정보 가져오기 실패: {} ({}) - {}", movieNm, movieCd, e.getMessage());
        }
        
        log.warn("상세정보 가져오기 완전 실패: {} ({})", movieNm, movieCd);
        return null;
    }

    /**
     * TMDB credits API에서 감독과 배우 정보를 가져와서 MovieDetail에 저장
     */
    private MovieDetail saveTmdbCreditsToMovieDetail(MovieDetail movieDetail, String tmdbId) {
        try {
            // TMDB credits API 호출
            String creditsUrl = String.format("https://api.themoviedb.org/3/movie/%s/credits?api_key=%s",
                    tmdbId, tmdbPopularMovieService.getTmdbApiKey());
            
            String response = restTemplate.getForObject(creditsUrl, String.class);
            JsonNode root = objectMapper.readTree(response);
            
            // 감독 정보 저장
            JsonNode crew = root.get("crew");
            if (crew != null && crew.isArray()) {
                for (JsonNode person : crew) {
                    if ("Director".equals(person.get("job").asText())) {
                        String directorName = person.get("name").asText();
                        Director director = saveTmdbDirector(directorName);
                        movieDetail.setDirector(director);
                        break; // 첫 번째 감독만 저장
                    }
                }
            }
            
            // 영화 저장 (감독 정보 포함)
            MovieDetail savedMovieDetail = movieRepository.save(movieDetail);
            
            // 배우 정보 저장
            JsonNode cast = root.get("cast");
            if (cast != null && cast.isArray()) {
                saveTmdbActors(cast, savedMovieDetail);
            }
            
            return savedMovieDetail;
            
        } catch (Exception e) {
            log.warn("TMDB credits 저장 실패: {} - {}", movieDetail.getMovieNm(), e.getMessage());
            // credits 저장에 실패해도 기본 영화 정보는 저장
            return movieRepository.save(movieDetail);
        }
    }

    /**
     * TMDB 감독 정보 저장
     */
    private Director saveTmdbDirector(String directorName) {
        try {
            // 기존 감독이 있는지 확인
            java.util.Optional<Director> existingDirector = 
                directorRepository.findByName(directorName);
            
            if (existingDirector.isPresent()) {
                return existingDirector.get();
            }

            // 새 감독 생성 (TMDB에서 이미지 URL 조회)
            String photoUrl = fetchTmdbPersonImageUrl(directorName);
            Director director = Director.builder()
                    .name(directorName)
                    .photoUrl(photoUrl)
                    .build();
            
            return directorRepository.save(director);
            
        } catch (Exception e) {
            log.warn("TMDB 감독 저장 실패: {} - {}", directorName, e.getMessage());
            return null;
        }
    }

    /**
     * TMDB 배우 정보 저장 (KOBIS API에서 한국어 배역명 우선 사용)
     */
    @Transactional
    private void saveTmdbActors(JsonNode cast, MovieDetail movieDetail) {
        log.info("TMDB 배우 정보 저장 시작: 영화={}, 배우 수={}", movieDetail.getMovieNm(), cast.size());
        
        try {
            // KOBIS API에서 한국어 배역명 가져오기
            Map<String, String> kobisCharacterNames = getKobisCharacterNames(movieDetail.getMovieCd());
            
            // 네이버 API에서 한국어 배역명 가져오기 (KOBIS에 없을 경우)
            Map<String, String> naverCharacterNames = new HashMap<>();
            if (kobisCharacterNames.isEmpty()) {
                naverCharacterNames = naverMovieService.getNaverCharacterNames(movieDetail.getMovieNm());
            }
            
            for (int i = 0; i < cast.size() && i < 10; i++) { // 최대 10명까지만
                JsonNode person = cast.get(i);
                String actorName = person.get("name").asText();
                
                // KOBIS에서 한국어 배역명 우선 사용, 없으면 네이버, 마지막으로 TMDB 영어 캐릭터명 사용
                String characterName = "";
                if (kobisCharacterNames.containsKey(actorName)) {
                    characterName = kobisCharacterNames.get(actorName);
                    log.info("KOBIS 배역명 사용: {} - {}", actorName, characterName);
                } else if (naverCharacterNames.containsKey(actorName)) {
                    characterName = naverCharacterNames.get(actorName);
                    log.info("네이버 배역명 사용: {} - {}", actorName, characterName);
                } else {
                    characterName = person.has("character") ? person.get("character").asText() : "";
                    log.info("TMDB 캐릭터명 사용: {} - {}", actorName, characterName);
                }
                
                log.info("TMDB 배우 처리 중: {}/{} - 이름: {}, 캐릭터: {}", i+1, Math.min(cast.size(), 10), actorName, characterName);
                
                // 기존 배우가 있는지 확인
                java.util.Optional<Actor> existingActor = 
                    actorRepository.findByName(actorName);
                Actor actor;
                
                if (existingActor.isPresent()) {
                    actor = existingActor.get();
                    log.info("기존 배우 사용: {}", actor.getName());
                    // 배우 이미지가 없으면 TMDB에서 보완
                    if (actor.getPhotoUrl() == null || actor.getPhotoUrl().isEmpty()) {
                        String photoUrl = fetchTmdbPersonImageUrl(actorName);
                        if (photoUrl != null && !photoUrl.isEmpty()) {
                            actor.setPhotoUrl(photoUrl);
                            actorRepository.save(actor);
                            log.info("배우 이미지 업데이트: {}", actor.getName());
                        }
                    }
                } else {
                    // 새 배우 생성 (TMDB에서 이미지 URL 조회)
                    String photoUrl = fetchTmdbPersonImageUrl(actorName);
                    actor = Actor.builder()
                            .name(actorName)
                            .photoUrl(photoUrl)
                            .build();
                    actor = actorRepository.save(actor);
                    log.info("새 배우 생성: {} (ID: {})", actor.getName(), actor.getId());
                }

                // 주연/조연 구분: 상위 3명은 주연, 나머지는 조연
                RoleType roleType = (i < 3) ? 
                    RoleType.LEAD : RoleType.SUPPORTING;

                // 기존 Cast가 있으면 roleType만 업데이트, 없으면 새로 저장
                Cast existingCast = castRepository.findByMovieDetailAndActor(movieDetail, actor);
                if (existingCast != null) {
                    existingCast.setRoleType(roleType);
                    // 기존 캐릭터명이 없거나 영어이면 한국어로 업데이트
                    String currentCharacterName = existingCast.getCharacterName();
                    if (currentCharacterName == null || currentCharacterName.isEmpty() || 
                        isEnglishCharacterName(currentCharacterName)) {
                        existingCast.setCharacterName(characterName);
                        log.info("기존 Cast 캐릭터명 업데이트: 영화={}, 배우={}, 기존={}, 새={}", 
                            movieDetail.getMovieNm(), actor.getName(), currentCharacterName, characterName);
                    }
                    castRepository.save(existingCast);
                    log.info("기존 Cast 업데이트: 영화={}, 배우={}, 역할={}", movieDetail.getMovieNm(), actor.getName(), roleType);
                } else {
                    Cast castEntity = Cast.builder()
                            .movieDetail(movieDetail)
                            .actor(actor)
                            .characterName(characterName)
                            .orderInCredits(i + 1)
                            .roleType(roleType)
                            .build();
                    castEntity = castRepository.save(castEntity);
                    log.info("새 Cast 생성: 영화={}, 배우={}, 역할={}, 순서={}, 캐릭터={}", 
                        movieDetail.getMovieNm(), actor.getName(), roleType, i + 1, characterName);
                }
            }
            
            // 저장 후 Cast 개수 확인
            List<Cast> savedCasts = castRepository.findByMovieDetailMovieCdOrderByOrderInCreditsAsc(movieDetail.getMovieCd());
            log.info("TMDB 배우 정보 저장 완료: 영화={}, 저장된 Cast 수={}", movieDetail.getMovieNm(), savedCasts.size());
            
        } catch (Exception e) {
            log.error("TMDB 배우 저장 실패: {} - {}", movieDetail.getMovieNm(), e.getMessage(), e);
        }
    }

    /**
     * KOBIS API에서 영화의 배우별 한국어 배역명 가져오기
     */
    public Map<String, String> getKobisCharacterNames(String movieCd) {
        Map<String, String> characterNames = new java.util.HashMap<>();
        
        try {
            // KOBIS 영화 상세정보 API 호출
            String detailUrl = String.format("%s/movie/searchMovieInfo.json?key=%s&movieCd=%s",
                    BASE_URL, kobisApiKey, movieCd);
            
            String response = restTemplate.getForObject(detailUrl, String.class);
            JsonNode root = objectMapper.readTree(response);
            JsonNode movieInfo = root.get("movieInfoResult").get("movieInfo");
            
            if (movieInfo != null && movieInfo.has("actors")) {
                JsonNode actors = movieInfo.get("actors");
                if (actors.isArray()) {
                    for (JsonNode actor : actors) {
                        String actorName = actor.get("peopleNm").asText();
                        String characterName = actor.has("cast") ? actor.get("cast").asText() : "";
                        
                        if (characterName != null && !characterName.isEmpty()) {
                            characterNames.put(actorName, characterName);
                            log.debug("KOBIS 배역명: {} - {}", actorName, characterName);
                        }
                    }
                }
            }
            
            log.info("KOBIS 배역명 {}개 가져옴: 영화={}", characterNames.size(), movieCd);
            
        } catch (Exception e) {
            log.warn("KOBIS 배역명 가져오기 실패: {} - {}", movieCd, e.getMessage());
        }
        
        return characterNames;
    }

    /**
     * TMDB에서 인물 이미지 URL 조회
     */
    private String fetchTmdbPersonImageUrl(String personName) {
        try {
            String query = java.net.URLEncoder.encode(personName, java.nio.charset.StandardCharsets.UTF_8);
            String url = "https://api.themoviedb.org/3/search/person?api_key=" + 
                    tmdbPopularMovieService.getTmdbApiKey() + "&query=" + query;
            
            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);
            JsonNode results = root.get("results");
            
            if (results != null && results.size() > 0) {
                String profilePath = results.get(0).get("profile_path").asText();
                if (profilePath != null && !profilePath.isEmpty()) {
                    return "https://image.tmdb.org/t/p/w500" + profilePath;
                }
            }
        } catch (Exception e) {
            log.warn("TMDB 인물 이미지 검색 오류: {} - {}", personName, e.getMessage());
        }
        return null;
    }

    /**
     * 기존 영화들의 캐릭터명을 한국어로 업데이트
     */
    @Transactional
    public void updateExistingCharacterNamesToKorean() {
        log.info("기존 영화들의 캐릭터명을 한국어로 업데이트 시작");
        
        try {
            List<MovieDetail> allMovies = movieRepository.findAll();
            int updatedCount = 0;
            
            for (MovieDetail movie : allMovies) {
                try {
                    // KOBIS API에서 한국어 배역명 가져오기
                    Map<String, String> kobisCharacterNames = getKobisCharacterNames(movie.getMovieCd());
                    
                    if (!kobisCharacterNames.isEmpty()) {
                        // 해당 영화의 모든 Cast 조회
                        List<Cast> casts = castRepository.findByMovieDetailMovieCdOrderByOrderInCreditsAsc(movie.getMovieCd());
                        
                        for (Cast cast : casts) {
                            String actorName = cast.getActor().getName();
                            
                            // KOBIS에서 한국어 배역명이 있으면 업데이트
                            if (kobisCharacterNames.containsKey(actorName)) {
                                String koreanCharacterName = kobisCharacterNames.get(actorName);
                                
                                // 기존 캐릭터명이 영어이거나 없으면 한국어로 업데이트
                                String currentCharacterName = cast.getCharacterName();
                                if (currentCharacterName == null || currentCharacterName.isEmpty() || 
                                    isEnglishCharacterName(currentCharacterName)) {
                                    cast.setCharacterName(koreanCharacterName);
                                    castRepository.save(cast);
                                    updatedCount++;
                                    log.info("캐릭터명 업데이트: 영화={}, 배우={}, 배역={}", 
                                        movie.getMovieNm(), actorName, koreanCharacterName);
                                }
                            }
                        }
                    }
                    
                    // API 호출 간격 조절 (KOBIS API 제한 고려)
                    Thread.sleep(100);
                    
                } catch (Exception e) {
                    log.warn("영화 {} 캐릭터명 업데이트 실패: {}", movie.getMovieNm(), e.getMessage());
                }
            }
            
            log.info("캐릭터명 업데이트 완료: {}개 업데이트됨", updatedCount);
            
        } catch (Exception e) {
            log.error("캐릭터명 업데이트 실패: {}", e.getMessage(), e);
        }
    }

    /**
     * 캐릭터명이 영어인지 확인 (간단한 체크)
     */
    private boolean isEnglishCharacterName(String characterName) {
        if (characterName == null || characterName.isEmpty()) {
            return false;
        }
        
        // 영어 문자가 70% 이상이면 영어로 간주
        int englishCount = 0;
        int totalCount = 0;
        
        for (char c : characterName.toCharArray()) {
            if (Character.isLetter(c)) {
                totalCount++;
                if (c >= 'A' && c <= 'Z' || c >= 'a' && c <= 'z') {
                    englishCount++;
                }
            }
        }
        
        return totalCount > 0 && (double) englishCount / totalCount >= 0.7;
    }
} 

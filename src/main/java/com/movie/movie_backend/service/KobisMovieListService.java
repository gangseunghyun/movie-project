package com.movie.movie_backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.movie.movie_backend.dto.MovieListDto;
import com.movie.movie_backend.entity.MovieList;
import com.movie.movie_backend.constant.MovieStatus;
import com.movie.movie_backend.repository.PRDMovieListRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class KobisMovieListService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final PRDMovieListRepository movieListRepository;

    @Value("${kobis.api.key}")
    private String kobisApiKey;

    private static final String BASE_URL = "http://www.kobis.or.kr/kobisopenapi/webservice/rest";
    private static final String MOVIE_LIST_URL = BASE_URL + "/movie/searchMovieList.json";

    /**
     * KOBIS 영화목록 API에서 인기 영화들을 가져와서 MovieList에 저장
     */
    public List<MovieListDto> getPopularMoviesFromMovieList(int limit) {
        List<MovieListDto> popularMovies = new ArrayList<>();
        Set<String> addedMovieCds = new HashSet<>();
        
        try {
            log.info("=== KOBIS 영화목록 API에서 인기 영화 가져오기 시작 (목표: {}개) ===", limit);
            
            // 최근 2년간의 영화들을 수집 (인기순으로 정렬)
            LocalDate endDate = LocalDate.now();
            LocalDate startDate = endDate.minusYears(2);
            
            String startDateStr = String.valueOf(startDate.getYear());
            String endDateStr = String.valueOf(endDate.getYear());
            
            log.info("영화 조회 기간: {} ~ {} (연도만 사용)", startDateStr, endDateStr);
            
            // 여러 페이지를 가져와서 충분한 데이터 확보
            int page = 1;
            int maxPages = 10; // 최대 10페이지까지 조회 (페이지당 100개씩)
            
            while (popularMovies.size() < limit && page <= maxPages) {
                String url = String.format("%s?key=%s&openStartDt=%s&openEndDt=%s&itemPerPage=100&curPage=%d", 
                    MOVIE_LIST_URL, kobisApiKey, startDateStr, endDateStr, page);
                
                log.info("KOBIS 영화목록 API 호출: page={}, 현재 수집된 영화: {}개, URL: {}", page, popularMovies.size(), url);
                
                String response = restTemplate.getForObject(url, String.class);
                if (response == null) {
                    log.error("KOBIS API 응답이 null입니다. page={}", page);
                    break;
                }
                
                log.info("KOBIS API 응답 길이: {} 문자", response.length());
                
                JsonNode root = objectMapper.readTree(response);
                JsonNode movieListResult = root.get("movieListResult");
                
                if (movieListResult == null || movieListResult.get("movieList") == null) {
                    log.warn("KOBIS API 응답에 movieList가 없습니다. page={}, 응답: {}", page, response.substring(0, Math.min(200, response.length())));
                    break;
                }
                
                JsonNode movieList = movieListResult.get("movieList");
                log.info("KOBIS API 응답에서 영화 {}개 발견 (페이지 {})", movieList.size(), page);
                
                int pageMovieCount = 0;
                
                for (JsonNode movie : movieList) {
                    if (popularMovies.size() >= limit) break;
                    
                    try {
                        String movieCd = movie.get("movieCd").asText();
                        
                        // 중복 체크
                        if (addedMovieCds.contains(movieCd)) {
                            continue;
                        }
                        
                        String movieNm = movie.get("movieNm").asText();
                        String movieNmEn = movie.has("movieNmEn") ? movie.get("movieNmEn").asText() : "";
                        String openDt = movie.has("openDt") ? movie.get("openDt").asText() : "";
                        String genreNm = movie.has("genreNm") ? movie.get("genreNm").asText() : "";
                        String nationNm = movie.has("nationNm") ? movie.get("nationNm").asText() : "";
                        String watchGradeNm = movie.has("watchGradeNm") ? movie.get("watchGradeNm").asText() : "";
                        
                        // 개봉일 파싱
                        LocalDate openDate = null;
                        if (!openDt.isEmpty()) {
                            try {
                                // yyyyMMdd 형식으로 파싱
                                openDate = LocalDate.parse(openDt, DateTimeFormatter.ofPattern("yyyyMMdd"));
                            } catch (Exception e) {
                                log.warn("날짜 파싱 실패: {} - {}", openDt, e.getMessage());
                            }
                        }
                        
                        // 영화 상태 결정
                        MovieStatus status = determineMovieStatus(openDate);
                        
                        MovieListDto movieDto = MovieListDto.builder()
                            .movieCd(movieCd)
                            .movieNm(movieNm)
                            .movieNmEn(movieNmEn)
                            .openDt(openDate)
                            .genreNm(genreNm)
                            .nationNm(nationNm)
                            .watchGradeNm(watchGradeNm)
                            .posterUrl("") // 나중에 TMDB에서 가져올 수 있음
                            .status(status)
                            .build();
                        
                        popularMovies.add(movieDto);
                        addedMovieCds.add(movieCd);
                        pageMovieCount++;
                        
                        log.debug("영화 추가: {} ({}) - 개봉일: {}, 상태: {}", 
                            movieNm, movieCd, openDate, status);
                        
                    } catch (Exception e) {
                        log.warn("영화 파싱 실패: {}", e.getMessage());
                    }
                }
                
                log.info("페이지 {} 처리 완료: {}개 영화 추가", page, pageMovieCount);
                
                // 페이지에 영화가 없으면 더 이상 조회하지 않음
                if (pageMovieCount == 0) {
                    log.info("페이지 {}에 더 이상 영화가 없습니다. 조회를 중단합니다.", page);
                    break;
                }
                
                page++;
                
                // API 호출 제한을 위한 딜레이
                Thread.sleep(100);
                
            }
            
            log.info("=== KOBIS 영화목록 API에서 인기 영화 가져오기 완료: {}개 ===", popularMovies.size());
            
        } catch (Exception e) {
            log.error("KOBIS 영화목록 API 호출 실패", e);
        }
        
        return popularMovies;
    }
    
    /**
     * 영화 개봉일을 기준으로 상태 결정
     */
    private MovieStatus determineMovieStatus(LocalDate openDate) {
        if (openDate == null) {
            return MovieStatus.ENDED;
        }
        
        LocalDate today = LocalDate.now();
        LocalDate threeMonthsAgo = today.minusMonths(3);
        LocalDate sixMonthsLater = today.plusMonths(6);
        
        if (openDate.isAfter(today)) {
            return MovieStatus.COMING_SOON;
        } else if (openDate.isAfter(threeMonthsAgo)) {
            return MovieStatus.NOW_PLAYING;
        } else {
            return MovieStatus.ENDED;
        }
    }
    
    /**
     * MovieListDto를 MovieList 엔티티로 변환하여 저장
     */
    public int saveMovieListFromDto(List<MovieListDto> movieDtos) {
        int successCount = 0;
        int skippedCount = 0;
        
        for (MovieListDto movieDto : movieDtos) {
            try {
                // 기존 데이터가 있으면 건너뛰기
                if (movieListRepository.findById(movieDto.getMovieCd()).isPresent()) {
                    skippedCount++;
                    continue;
                }
                
                // MovieList 엔티티로 변환하여 저장
                MovieList movieList = MovieList.builder()
                    .movieCd(movieDto.getMovieCd())
                    .movieNm(movieDto.getMovieNm())
                    .movieNmEn(movieDto.getMovieNmEn())
                    .openDt(movieDto.getOpenDt())
                    .genreNm(movieDto.getGenreNm())
                    .nationNm(movieDto.getNationNm())
                    .watchGradeNm(movieDto.getWatchGradeNm())
                    .posterUrl(movieDto.getPosterUrl())
                    .status(movieDto.getStatus())
                    .build();
                
                movieListRepository.save(movieList);
                successCount++;
                
                log.info("MovieList 저장 완료: {} ({})", movieDto.getMovieNm(), movieDto.getMovieCd());
                
            } catch (Exception e) {
                log.warn("MovieList 저장 실패: {} - {}", movieDto.getMovieNm(), e.getMessage());
            }
        }
        
        log.info("MovieList 저장 완료: 성공={}, 건너뜀={}", successCount, skippedCount);
        return successCount;
    }
} 
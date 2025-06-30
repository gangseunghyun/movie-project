package com.movie.movie_backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.movie.movie_backend.dto.MovieDetailDto;
import com.movie.movie_backend.dto.MovieListDto;
import com.movie.movie_backend.entity.MovieDetail;
import com.movie.movie_backend.mapper.MovieDetailMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class TmdbPopularMovieService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final MovieDetailMapper movieDetailMapper;
    private final KobisApiService kobisApiService;

    @Value("${tmdb.api.key}")
    private String tmdbApiKey;

    @Value("${kobis.api.key}")
    private String kobisApiKey;

    private static final String TMDB_POPULAR_URL = "https://api.themoviedb.org/3/movie/popular";
    private static final String TMDB_SEARCH_URL = "https://api.themoviedb.org/3/search/movie";
    private static final String TMDB_MOVIE_DETAIL_URL = "https://api.themoviedb.org/3/movie";

    /**
     * TMDB에서 인기 영화 100개를 MovieListDto 형태로 가져오기
     */
    public List<MovieListDto> getPopularMoviesAsMovieList(int limit) {
        List<MovieListDto> popularMovies = new ArrayList<>();
        
        try {
            // TMDB Popular API 호출 (한 번에 최대 20개씩, 5페이지까지)
            int pages = (int) Math.ceil((double) limit / 20);
            
            for (int page = 1; page <= pages && popularMovies.size() < limit; page++) {
                String url = String.format("%s?api_key=%s&language=ko-KR&page=%d", 
                    TMDB_POPULAR_URL, tmdbApiKey, page);
                
                log.info("TMDB Popular API 호출: page={}", page);
                String response = restTemplate.getForObject(url, String.class);
                JsonNode root = objectMapper.readTree(response);
                JsonNode results = root.get("results");
                
                if (results != null && results.isArray()) {
                    for (JsonNode movie : results) {
                        if (popularMovies.size() >= limit) break;
                        
                        try {
                            MovieListDto movieDto = convertTmdbToMovieListDto(movie);
                            if (movieDto != null) {
                                popularMovies.add(movieDto);
                                log.info("인기 영화 추가: {} ({})", movieDto.getMovieNm(), movieDto.getMovieCd());
                            }
                        } catch (Exception e) {
                            log.warn("영화 변환 실패: {}", movie.get("title"), e);
                        }
                    }
                }
                
                // API 호출 제한 고려
                Thread.sleep(200);
            }
            
            log.info("인기 영화 {}개 가져오기 완료", popularMovies.size());
            return popularMovies;
            
        } catch (Exception e) {
            log.error("TMDB 인기 영화 가져오기 실패", e);
            return new ArrayList<>();
        }
    }

    /**
     * TMDB 영화 데이터를 MovieListDto로 변환
     */
    private MovieListDto convertTmdbToMovieListDto(JsonNode tmdbMovie) {
        try {
            String title = tmdbMovie.get("title").asText();
            String originalTitle = tmdbMovie.has("original_title") ? tmdbMovie.get("original_title").asText() : title;
            String releaseDate = tmdbMovie.has("release_date") ? tmdbMovie.get("release_date").asText() : "";
            
            // KOBIS에서 한국어 정보 가져오기 시도
            MovieDetail kobisMovie = findKobisMovieByTitle(title, originalTitle, releaseDate);
            
            // KOBIS에서 찾으면 KOBIS movieCd 사용, 없으면 TMDB ID 사용
            String movieCd = kobisMovie != null ? kobisMovie.getMovieCd() : "TMDB_" + tmdbMovie.get("id").asText();
            String movieNm = kobisMovie != null ? kobisMovie.getMovieNm() : title;
            String genreNm = kobisMovie != null ? kobisMovie.getGenreNm() : "";
            String nationNm = kobisMovie != null ? kobisMovie.getNationNm() : "";
            
            return MovieListDto.builder()
                    .movieCd(movieCd)
                    .movieNm(movieNm)
                    .movieNmEn(originalTitle)
                    .openDt(releaseDate.isEmpty() ? null : java.time.LocalDate.parse(releaseDate))
                    .genreNm(genreNm)
                    .nationNm(nationNm)
                    .build();
                    
        } catch (Exception e) {
            log.warn("TMDB 영화 변환 실패", e);
            return null;
        }
    }

    /**
     * TMDB에서 인기 영화 100개 가져오기
     */
    public List<MovieDetailDto> getPopularMovies(int limit) {
        List<MovieDetailDto> popularMovies = new ArrayList<>();
        
        try {
            // TMDB Popular API 호출 (한 번에 최대 20개씩, 5페이지까지)
            int pages = (int) Math.ceil((double) limit / 20);
            
            for (int page = 1; page <= pages && popularMovies.size() < limit; page++) {
                String url = String.format("%s?api_key=%s&language=ko-KR&page=%d", 
                    TMDB_POPULAR_URL, tmdbApiKey, page);
                
                log.info("TMDB Popular API 호출: page={}", page);
                String response = restTemplate.getForObject(url, String.class);
                JsonNode root = objectMapper.readTree(response);
                JsonNode results = root.get("results");
                
                if (results != null && results.isArray()) {
                    for (JsonNode movie : results) {
                        if (popularMovies.size() >= limit) break;
                        
                        try {
                            MovieDetailDto movieDto = convertTmdbToMovieDetailDto(movie);
                            if (movieDto != null) {
                                popularMovies.add(movieDto);
                                log.info("인기 영화 추가: {} ({})", movieDto.getMovieNm(), movieDto.getMovieNmEn());
                            }
                        } catch (Exception e) {
                            log.warn("영화 변환 실패: {}", movie.get("title"), e);
                        }
                    }
                }
                
                // API 호출 제한 고려
                Thread.sleep(200);
            }
            
            log.info("인기 영화 {}개 가져오기 완료", popularMovies.size());
            return popularMovies;
            
        } catch (Exception e) {
            log.error("TMDB 인기 영화 가져오기 실패", e);
            return new ArrayList<>();
        }
    }

    /**
     * TMDB 영화 데이터를 MovieDetailDto로 변환
     */
    private MovieDetailDto convertTmdbToMovieDetailDto(JsonNode tmdbMovie) {
        try {
            String title = tmdbMovie.get("title").asText();
            String originalTitle = tmdbMovie.has("original_title") ? tmdbMovie.get("original_title").asText() : title;
            String overview = tmdbMovie.has("overview") ? tmdbMovie.get("overview").asText() : "";
            String releaseDate = tmdbMovie.has("release_date") ? tmdbMovie.get("release_date").asText() : "";
            double popularity = tmdbMovie.has("popularity") ? tmdbMovie.get("popularity").asDouble() : 0.0;
            double voteAverage = tmdbMovie.has("vote_average") ? tmdbMovie.get("vote_average").asDouble() : 0.0;
            int voteCount = tmdbMovie.has("vote_count") ? tmdbMovie.get("vote_count").asInt() : 0;
            
            // TMDB overview 필드 로깅 추가
            if (!overview.isEmpty()) {
                log.info("TMDB Popular Movie overview 필드 발견: 영화={}, overview 길이={}, overview 내용={}", 
                    title, overview.length(), 
                    overview.length() > 100 ? overview.substring(0, 100) + "..." : overview);
            } else {
                log.warn("TMDB Popular Movie overview 필드 없음: 영화={}", title);
            }
            
            // 포스터 URL
            String posterPath = tmdbMovie.has("poster_path") ? tmdbMovie.get("poster_path").asText() : null;
            String posterUrl = null;
            if (posterPath != null && !posterPath.isEmpty()) {
                posterUrl = "https://image.tmdb.org/t/p/w500" + posterPath;
            }
            
            // KOBIS에서 한국어 정보 가져오기 시도
            MovieDetail kobisMovie = findKobisMovieByTitle(title, originalTitle, releaseDate);
            
            // 장르 정보 - 기존 장르에 추가
            String genreNm = "";
            if (tmdbMovie.has("genres") && tmdbMovie.get("genres").isArray()) {
                JsonNode genres = tmdbMovie.get("genres");
                List<String> genreNames = new ArrayList<>();
                for (JsonNode genre : genres) {
                    genreNames.add(genre.get("name").asText());
                }
                
                // 기존 장르가 있으면 추가, 없으면 새로 생성
                if (kobisMovie != null && kobisMovie.getGenreNm() != null && !kobisMovie.getGenreNm().isEmpty()) {
                    // 기존 장르와 새로운 장르를 합침
                    Set<String> allGenres = new HashSet<>();
                    
                    // 기존 장르 추가
                    String[] existingGenres = kobisMovie.getGenreNm().split(",");
                    for (String existing : existingGenres) {
                        allGenres.add(existing.trim());
                    }
                    
                    // 새로운 장르 추가
                    allGenres.addAll(genreNames);
                    
                    genreNm = String.join(",", allGenres);
                } else {
                    // 기존 장르가 없으면 새로운 장르만 사용
                    genreNm = String.join(",", genreNames);
                }
            } else if (kobisMovie != null && kobisMovie.getGenreNm() != null) {
                // TMDB에 장르가 없으면 기존 장르 사용
                genreNm = kobisMovie.getGenreNm();
            }
            
            return MovieDetailDto.builder()
                    .movieCd(kobisMovie != null ? kobisMovie.getMovieCd() : "TMDB_" + tmdbMovie.get("id").asText())
                    .movieNm(kobisMovie != null ? kobisMovie.getMovieNm() : title)
                    .movieNmEn(originalTitle)
                    .description(overview)
                    .openDt(kobisMovie != null ? kobisMovie.getOpenDt() : null)
                    .showTm(kobisMovie != null ? kobisMovie.getShowTm() : 0)
                    .genreNm(genreNm)
                    .nationNm(kobisMovie != null ? kobisMovie.getNationNm() : "")
                    .watchGradeNm(kobisMovie != null ? kobisMovie.getWatchGradeNm() : "")
                    .companyNm(kobisMovie != null ? kobisMovie.getCompanyNm() : "")
                    .posterUrl(posterUrl)
                    .directorName(kobisMovie != null && kobisMovie.getDirector() != null ? kobisMovie.getDirector().getName() : "")
                    .totalAudience(kobisMovie != null ? kobisMovie.getTotalAudience() : 0)
                    .reservationRate(kobisMovie != null ? kobisMovie.getReservationRate() : 0.0)
                    .averageRating(voteAverage)
                    .build();
                    
        } catch (Exception e) {
            log.warn("TMDB 영화 변환 실패", e);
            return null;
        }
    }

    /**
     * 영화 제목으로 KOBIS에서 한국어 정보 찾기
     */
    private MovieDetail findKobisMovieByTitle(String title, String originalTitle, String releaseDate) {
        try {
            // 1. 한국어 제목으로 검색
            MovieDetail movie = searchKobisMovieByTitle(title, releaseDate);
            if (movie != null) {
                return movie;
            }
            
            // 2. 영문 제목으로 검색
            if (!originalTitle.equals(title)) {
                movie = searchKobisMovieByTitle(originalTitle, releaseDate);
                if (movie != null) {
                    return movie;
                }
            }
            
        } catch (Exception e) {
            log.warn("KOBIS 영화 검색 실패: {} / {}", title, originalTitle, e);
        }
        
        return null;
    }

    /**
     * 제목으로 KOBIS 영화 검색
     */
    private MovieDetail searchKobisMovieByTitle(String title, String releaseDate) {
        try {
            // KOBIS 영화목록 API에서 검색
            String query = URLEncoder.encode(title, StandardCharsets.UTF_8);
            String url = String.format("http://www.kobis.or.kr/kobisopenapi/webservice/rest/movie/searchMovieList.json?key=%s&movieNm=%s", 
                kobisApiKey, query);
            
            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);
            JsonNode movieListResult = root.get("movieListResult");
            
            if (movieListResult != null && movieListResult.get("movieList") != null) {
                JsonNode movieList = movieListResult.get("movieList");
                
                for (JsonNode movie : movieList) {
                    String movieCd = movie.get("movieCd").asText();
                    String movieNm = movie.get("movieNm").asText();
                    String openDt = movie.has("openDt") ? movie.get("openDt").asText() : "";
                    
                    // 제목이 일치하고 개봉일이 비슷한 영화 찾기
                    if (movieNm.contains(title) || title.contains(movieNm)) {
                        if (releaseDate.isEmpty() || openDt.isEmpty() || 
                            releaseDate.substring(0, 4).equals(openDt.substring(0, 4))) {
                            
                            // MovieDetail 가져오기
                            return kobisApiService.findMovieDetailByCode(movieCd).orElse(null);
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            log.warn("KOBIS 영화 검색 실패: {}", title, e);
        }
        
        return null;
    }

    /**
     * TMDB에서 특정 영화의 상세정보 가져오기
     */
    public MovieDetailDto getMovieDetailFromTmdb(String tmdbId) {
        try {
            String url = String.format("%s/%s?api_key=%s&language=ko-KR&append_to_response=credits", 
                TMDB_MOVIE_DETAIL_URL, tmdbId, tmdbApiKey);
            
            log.info("TMDB 영화 상세정보 API 호출: movieId={}", tmdbId);
            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);
            
            return convertTmdbDetailToMovieDetailDto(root);
            
        } catch (Exception e) {
            log.warn("TMDB 영화 상세정보 가져오기 실패: movieId={}, error={}", tmdbId, e.getMessage());
            return null;
        }
    }

    /**
     * TMDB 상세정보를 MovieDetailDto로 변환
     */
    private MovieDetailDto convertTmdbDetailToMovieDetailDto(JsonNode tmdbMovie) {
        try {
            String title = tmdbMovie.get("title").asText();
            String originalTitle = tmdbMovie.has("original_title") ? tmdbMovie.get("original_title").asText() : title;
            String overview = tmdbMovie.has("overview") ? tmdbMovie.get("overview").asText() : "";
            String releaseDate = tmdbMovie.has("release_date") ? tmdbMovie.get("release_date").asText() : "";
            int runtime = tmdbMovie.has("runtime") ? tmdbMovie.get("runtime").asInt() : 0;
            double voteAverage = tmdbMovie.has("vote_average") ? tmdbMovie.get("vote_average").asDouble() : 0.0;
            int voteCount = tmdbMovie.has("vote_count") ? tmdbMovie.get("vote_count").asInt() : 0;
            
            // 포스터 URL
            String posterPath = tmdbMovie.has("poster_path") ? tmdbMovie.get("poster_path").asText() : null;
            String posterUrl = null;
            if (posterPath != null && !posterPath.isEmpty()) {
                posterUrl = "https://image.tmdb.org/t/p/w500" + posterPath;
            }
            
            // 장르 정보 - TMDB에서 모든 장르 가져오기
            String genreNm = "";
            if (tmdbMovie.has("genres") && tmdbMovie.get("genres").isArray()) {
                JsonNode genres = tmdbMovie.get("genres");
                List<String> genreNames = new ArrayList<>();
                for (JsonNode genre : genres) {
                    genreNames.add(genre.get("name").asText());
                }
                genreNm = String.join(",", genreNames); // 모든 장르를 쉼표로 구분
            }
            
            // 국가 정보
            String nationNm = "";
            if (tmdbMovie.has("production_countries") && tmdbMovie.get("production_countries").isArray()) {
                JsonNode countries = tmdbMovie.get("production_countries");
                List<String> countryNames = new ArrayList<>();
                for (JsonNode country : countries) {
                    countryNames.add(country.get("name").asText());
                }
                nationNm = String.join(",", countryNames);
            }
            
            // 제작사 정보
            String companyNm = "";
            if (tmdbMovie.has("production_companies") && tmdbMovie.get("production_companies").isArray()) {
                JsonNode companies = tmdbMovie.get("production_companies");
                if (companies.size() > 0) {
                    companyNm = companies.get(0).get("name").asText();
                }
            }
            
            // 감독 정보 (credits에서)
            String directorName = "";
            if (tmdbMovie.has("credits")) {
                JsonNode credits = tmdbMovie.get("credits");
                if (credits.has("crew") && credits.get("crew").isArray()) {
                    JsonNode crew = credits.get("crew");
                    for (JsonNode member : crew) {
                        String job = member.get("job").asText();
                        if ("Director".equals(job)) {
                            directorName = member.get("name").asText();
                            break;
                        }
                    }
                }
            }
            
            return MovieDetailDto.builder()
                    .movieCd("TMDB_" + tmdbMovie.get("id").asText())
                    .movieNm(title)
                    .movieNmEn(originalTitle)
                    .description(overview)
                    .openDt(releaseDate.isEmpty() ? null : java.time.LocalDate.parse(releaseDate))
                    .showTm(runtime)
                    .genreNm(genreNm)
                    .nationNm(nationNm)
                    .watchGradeNm("") // TMDB에는 관람등급 정보 없음
                    .companyNm(companyNm)
                    .posterUrl(posterUrl)
                    .directorName(directorName)
                    .totalAudience(0) // TMDB에는 관객수 정보 없음
                    .reservationRate(0.0) // TMDB에는 예매율 정보 없음
                    .averageRating(voteAverage)
                    .build();
                    
        } catch (Exception e) {
            log.warn("TMDB 상세정보 변환 실패", e);
            return null;
        }
    }

    public String getTmdbApiKey() {
        return tmdbApiKey;
    }
} 

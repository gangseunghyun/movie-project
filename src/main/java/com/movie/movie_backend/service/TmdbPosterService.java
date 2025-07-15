package com.movie.movie_backend.service;

import com.movie.movie_backend.entity.MovieDetail;
import com.movie.movie_backend.repository.PRDMovieRepository;
import com.movie.movie_backend.repository.PRDMovieListRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.List;
import com.movie.movie_backend.mapper.TopRatedMovieMapper;
import com.movie.movie_backend.dto.TopRatedMovieDto;

@Slf4j
@Service
@RequiredArgsConstructor
public class TmdbPosterService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final PRDMovieRepository movieRepository;
    private final PRDMovieListRepository movieListRepository;
    private final TopRatedMovieMapper topRatedMovieMapper;
    private final REVRatingService ratingService; // 사용자 평점 서비스 사용

    @Value("${tmdb.api.key}")
    private String tmdbApiKey;

    @Value("${tmdb.api.base-url}")
    private String tmdbBaseUrl;

    /**
     * 평균 별점이 높은 영화 TOP-N 조회 (사용자 평점만 사용)
     */
    @org.springframework.cache.annotation.Cacheable(value = "topRatedMovies", key = "#limit")
    public List<MovieDetail> getTopRatedMovies(int limit) {
        // 데이터베이스 레벨에서 직접 평점 높은 영화 조회 (매우 빠름)
        org.springframework.data.domain.PageRequest pageRequest = org.springframework.data.domain.PageRequest.of(0, limit * 2);
        // MovieList JOIN 없이 평점 높은 영화 조회 (더 많은 영화가 표시되도록)
        List<MovieDetail> topRatedMovies = movieRepository.findTopRatedMovies(4.0, pageRequest);
        
        // MovieList가 있는 영화만 필터링 (이미 JOIN으로 처리됨)
        topRatedMovies = topRatedMovies.stream()
                .limit(limit)
                .toList();
        
        // 포스터 URL이 없는 영화들은 자동으로 TMDB에서 가져오기
        for (MovieDetail movie : topRatedMovies) {
            try {
                var movieListOpt = movieListRepository.findById(movie.getMovieCd());
                if (movieListOpt.isPresent()) {
                    var movieList = movieListOpt.get();
                    if (movieList.getPosterUrl() == null || movieList.getPosterUrl().isEmpty()) {
                        // TMDB에서 포스터 URL 가져오기
                        String posterUrl = fetchPosterUrlFromTmdb(movie.getMovieNm(), movie.getOpenDt());
                        if (posterUrl != null) {
                            movieList.setPosterUrl(posterUrl);
                            movieListRepository.save(movieList);
                            log.info("Top Rated 영화 포스터 자동 가져오기: {} -> {}", movie.getMovieNm(), posterUrl);
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Top Rated 영화 포스터 자동 가져오기 실패: {}", movie.getMovieNm(), e);
            }
        }
        
        return topRatedMovies;
    }

    /**
     * TMDB에서 포스터 URL 가져오기
     */
    public String fetchPosterUrlFromTmdb(String movieName, LocalDate openDt) {
        try {
            String query = java.net.URLEncoder.encode(movieName, java.nio.charset.StandardCharsets.UTF_8);
            String year = (openDt != null) ? String.valueOf(openDt.getYear()) : null;
            String url = tmdbBaseUrl + "/search/movie" +
                    "?api_key=" + tmdbApiKey +
                    "&query=" + query +
                    (year != null ? "&year=" + year : "") +
                    "&language=ko-KR";

            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);
            JsonNode results = root.get("results");
            if (results != null && results.size() > 0) {
                String posterPath = results.get(0).get("poster_path").asText();
                if (posterPath != null && !posterPath.isEmpty()) {
                    return "https://image.tmdb.org/t/p/w500" + posterPath;
                }
            }
        } catch (Exception e) {
            log.warn("TMDB 포스터 검색 실패: {}", movieName, e);
        }
        return null;
    }

    /**
     * 평균 별점이 높은 영화 TOP-N 조회 (DTO)
     */
    public List<TopRatedMovieDto> getTopRatedMoviesAsDto(int limit) {
        List<MovieDetail> topRatedMovies = getTopRatedMovies(limit);
        return topRatedMovieMapper.toDtoList(topRatedMovies);
    }
} 

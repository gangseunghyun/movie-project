package com.movie.movie_backend.service;

import com.movie.movie_backend.entity.MovieList;
import com.movie.movie_backend.repository.PRDMovieListRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TmdbPosterBatchService {

    private final PRDMovieListRepository movieListRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${tmdb.api.key}")
    private String tmdbApiKey;

    private static final String TMDB_SEARCH_URL = "https://api.themoviedb.org/3/search/movie";

    @Transactional
    public void updatePosterUrlsForAllMovies() {
        List<MovieList> movies = new ArrayList<>();
        int page = 0, size = 1000;
        Page<MovieList> moviePage;
        do {
            moviePage = movieListRepository.findAll(PageRequest.of(page++, size));
            movies.addAll(moviePage.getContent());
        } while (moviePage.hasNext());

        int updated = 0, failed = 0;

        for (MovieList movie : movies) {
            if (movie.getPosterUrl() != null && !movie.getPosterUrl().isEmpty()) continue;

            String posterUrl = fetchPosterUrlFromTmdb(movie);
            if (posterUrl != null) {
                movie.setPosterUrl(posterUrl);
                movieListRepository.save(movie);
                updated++;
                log.info("포스터 매칭 성공: {} ({}) -> {}", movie.getMovieNm(), movie.getOpenDt(), posterUrl);
            } else {
                failed++;
                log.warn("포스터 매칭 실패: {} ({})", movie.getMovieNm(), movie.getOpenDt());
            }
        }
        log.info("TMDB 포스터 자동 매칭 완료: 성공 {}건, 실패 {}건", updated, failed);
    }

    public List<MovieList> getAllMovieListsPaged(int chunkSize) {
        List<MovieList> result = new ArrayList<>();
        int page = 0;
        Page<MovieList> moviePage;
        do {
            moviePage = movieListRepository.findAll(PageRequest.of(page, chunkSize));
            result.addAll(moviePage.getContent());
            page++;
        } while (!moviePage.isLast());
        return result;
    }

    private String fetchPosterUrlFromTmdb(MovieList movie) {
        try {
            String query = URLEncoder.encode(movie.getMovieNm(), StandardCharsets.UTF_8);
            String year = (movie.getOpenDt() != null) ? String.valueOf(movie.getOpenDt().getYear()) : null;
            String url = TMDB_SEARCH_URL +
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
            // 2차 시도: 영문 제목으로 검색
            if (movie.getMovieNmEn() != null && !movie.getMovieNmEn().isEmpty()) {
                query = URLEncoder.encode(movie.getMovieNmEn(), StandardCharsets.UTF_8);
                url = TMDB_SEARCH_URL +
                        "?api_key=" + tmdbApiKey +
                        "&query=" + query +
                        (year != null ? "&year=" + year : "") +
                        "&language=ko-KR";
                response = restTemplate.getForObject(url, String.class);
                root = objectMapper.readTree(response);
                results = root.get("results");
                if (results != null && results.size() > 0) {
                    String posterPath = results.get(0).get("poster_path").asText();
                    if (posterPath != null && !posterPath.isEmpty()) {
                        return "https://image.tmdb.org/t/p/w500" + posterPath;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("TMDB 포스터 검색 오류: {} ({}) - {}", movie.getMovieNm(), movie.getOpenDt(), e.getMessage());
        }
        return null;
    }
} 

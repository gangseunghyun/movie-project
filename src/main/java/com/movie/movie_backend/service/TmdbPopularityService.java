package com.movie.movie_backend.service;

import com.movie.movie_backend.entity.MovieList;
import com.movie.movie_backend.repository.PRDMovieListRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TmdbPopularityService {

    private final PRDMovieListRepository movieListRepository;
    private final RestTemplate restTemplate;

    @Value("${tmdb.api.key}")
    private String tmdbApiKey;

    /**
     * TMDB ID가 있는 모든 영화의 인기도를 업데이트
     */
    public void updateAllMoviePopularity() {
        log.info("=== TMDB 인기도 업데이트 시작 ===");
        
        List<MovieList> moviesWithTmdbId = new ArrayList<>();
        int page = 0, size = 1000;
        Page<MovieList> moviePage;
        do {
            moviePage = movieListRepository.findAll(PageRequest.of(page++, size));
            moviesWithTmdbId.addAll(moviePage.getContent().stream()
                .filter(m -> m.getTmdbId() != null)
                .toList());
        } while (moviePage.hasNext());
        
        log.info("TMDB ID가 있는 영화 수: {}", moviesWithTmdbId.size());
        
        int updated = 0;
        int failed = 0;
        
        for (MovieList movie : moviesWithTmdbId) {
            try {
                Double popularity = getMoviePopularity(movie.getTmdbId());
                if (popularity != null) {
                    movie.setTmdbPopularity(popularity);
                    movieListRepository.save(movie);
                    updated++;
                    log.info("인기도 업데이트 성공: {} ({}) -> popularity={}", 
                        movie.getMovieNm(), movie.getTmdbId(), popularity);
                } else {
                    failed++;
                    log.warn("인기도 조회 실패: {} ({})", movie.getMovieNm(), movie.getTmdbId());
                }
                
                // API 호출 제한을 위한 딜레이
                Thread.sleep(100);
                
            } catch (Exception e) {
                failed++;
                log.error("인기도 업데이트 중 오류: {} ({}) - {}", 
                    movie.getMovieNm(), movie.getTmdbId(), e.getMessage());
            }
        }
        
        log.info("=== TMDB 인기도 업데이트 완료: 성공 {}, 실패 {} ===", updated, failed);
    }

    /**
     * 특정 영화의 TMDB 인기도 조회
     */
    public Double getMoviePopularity(Integer tmdbId) {
        try {
            String url = String.format(
                "https://api.themoviedb.org/3/movie/%d?api_key=%s&language=ko-KR",
                tmdbId, tmdbApiKey
            );
            
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response != null && response.containsKey("popularity")) {
                return (Double) response.get("popularity");
            }
        } catch (Exception e) {
            log.error("TMDB 인기도 조회 실패: tmdbId={} - {}", tmdbId, e.getMessage());
        }
        return null;
    }

    /**
     * 특정 영화의 인기도 업데이트
     */
    public boolean updateMoviePopularity(String movieCd) {
        try {
            MovieList movie = movieListRepository.findById(movieCd).orElse(null);
            if (movie == null || movie.getTmdbId() == null) {
                log.warn("영화를 찾을 수 없거나 TMDB ID가 없음: {}", movieCd);
                return false;
            }
            
            Double popularity = getMoviePopularity(movie.getTmdbId());
            if (popularity != null) {
                movie.setTmdbPopularity(popularity);
                movieListRepository.save(movie);
                log.info("인기도 업데이트 성공: {} ({}) -> popularity={}", 
                    movie.getMovieNm(), movie.getTmdbId(), popularity);
                return true;
            }
        } catch (Exception e) {
            log.error("인기도 업데이트 실패: {} - {}", movieCd, e.getMessage());
        }
        return false;
    }

    /**
     * 인기도가 높은 영화 TOP-N 조회
     */
    public List<MovieList> getTopPopularMovies(int limit) {
        return movieListRepository.findAll().stream()
            .filter(movie -> movie.getTmdbPopularity() != null)
            .sorted((m1, m2) -> Double.compare(m2.getTmdbPopularity(), m1.getTmdbPopularity()))
            .limit(limit)
            .toList();
    }

    /**
     * 인기도 기준으로 영화 검색 (최소 인기도 이상)
     */
    public List<MovieList> getMoviesByMinPopularity(double minPopularity) {
        return movieListRepository.findAll().stream()
            .filter(movie -> movie.getTmdbPopularity() != null && movie.getTmdbPopularity() >= minPopularity)
            .sorted((m1, m2) -> Double.compare(m2.getTmdbPopularity(), m1.getTmdbPopularity()))
            .toList();
    }

    private List<MovieList> getAllMovieListsChunked() {
        List<MovieList> allMovieLists = new ArrayList<>();
        int page = 0, size = 1000;
        Page<MovieList> moviePage;
        do {
            moviePage = movieListRepository.findAll(PageRequest.of(page++, size));
            allMovieLists.addAll(moviePage.getContent());
        } while (moviePage.hasNext());
        return allMovieLists;
    }
} 
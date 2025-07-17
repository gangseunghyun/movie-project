package com.movie.movie_backend.controller;

import com.movie.movie_backend.entity.MovieList;
import com.movie.movie_backend.service.PRDMovieListService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tmdb")
@RequiredArgsConstructor
public class TmdbMappingController {

    private final PRDMovieListService movieListService;
    private final RestTemplate restTemplate;

    @Value("${tmdb.api.key}")
    private String tmdbApiKey;

    @GetMapping("/map-all")
    public ResponseEntity<String> mapAllMoviesToTmdb() {
        List<MovieList> movies = movieListService.getAllMovies().stream()
                .map(dto -> {
                    MovieList movie = new MovieList();
                    movie.setMovieCd(dto.getMovieCd());
                    movie.setMovieNm(dto.getMovieNm());
                    movie.setTmdbId(dto.getTmdbId());
                    return movie;
                })
                .toList();
        
        int mappedCount = 0;

        for (MovieList movie : movies) {
            if (movie.getTmdbId() == null) {
                Integer tmdbId = searchTmdbId(movie.getMovieNm());
                if (tmdbId != null) {
                    movie.setTmdbId(tmdbId);
                    // Note: Need to implement save method in service
                    mappedCount++;
                }
            }
        }

        return ResponseEntity.ok("Mapped " + mappedCount + " movies to TMDB IDs");
    }

    @GetMapping("/search/{movieTitle}")
    public ResponseEntity<Map<String, Object>> searchTmdbIdForMovie(@PathVariable String movieTitle) {
        Integer tmdbId = searchTmdbId(movieTitle);
        if (tmdbId != null) {
            return ResponseEntity.ok(Map.of("tmdbId", tmdbId, "movieTitle", movieTitle));
        } else {
            return ResponseEntity.ok(Map.of("tmdbId", null, "movieTitle", movieTitle));
        }
    }

    private Integer searchTmdbId(String movieTitle) {
        try {
            String url = String.format(
                "https://api.themoviedb.org/3/search/movie?api_key=%s&query=%s&language=ko-KR",
                tmdbApiKey, movieTitle
            );
            
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response != null && response.containsKey("results")) {
                List<Map<String, Object>> results = (List<Map<String, Object>>) response.get("results");
                if (!results.isEmpty()) {
                    return (Integer) results.get(0).get("id");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
} 
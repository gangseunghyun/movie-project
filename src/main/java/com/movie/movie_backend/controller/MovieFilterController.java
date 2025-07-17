package com.movie.movie_backend.controller;

import com.movie.movie_backend.entity.MovieList;
import com.movie.movie_backend.dto.MovieListDto;
import com.movie.movie_backend.repository.PRDMovieListRepository;
import com.movie.movie_backend.mapper.MovieListMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/movies")
@RequiredArgsConstructor
public class MovieFilterController {
    private final PRDMovieListRepository movieListRepository;
    private final MovieListMapper movieListMapper;

    /**
     * 영화 필터링 API
     * 
     * 사용법:
     * - 장르 필터링: /api/movies/filter?genres=액션,코미디&page=0&size=20
     * - 검색어 필터링: /api/movies/filter?search=아바타&page=0&size=20
     * - 정렬: /api/movies/filter?sort=date&page=0&size=20
     */
    @GetMapping("/filter")
    public ResponseEntity<Map<String, Object>> filterMovies(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String genres,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "tmdb_popularity") String sort) {
        try {
            log.info("영화 필터링 요청: page={}, size={}, genres={}, search={}, sort={}", 
                    page, size, genres, search, sort);

            List<MovieList> filteredMovies;

            // 1. 기본 데이터 조회
            if (search != null && !search.trim().isEmpty()) {
                filteredMovies = movieListRepository.findByMovieNmContainingIgnoreCase(search.trim());
                log.info("검색어 '{}'로 필터링된 영화: {}개", search, filteredMovies.size());
            } else {
                filteredMovies = movieListRepository.findAll();
                log.info("전체 영화 조회: {}개", filteredMovies.size());
            }

            // 포스터 URL이 없는 영화 제외
            filteredMovies = filteredMovies.stream()
                    .filter(movie -> movie.getPosterUrl() != null && 
                                   !movie.getPosterUrl().trim().isEmpty() && 
                                   !movie.getPosterUrl().equals("null"))
                    .collect(Collectors.toList());
            log.info("포스터 URL 필터링 후 영화: {}개", filteredMovies.size());

            // 2. 장르 필터링
            if (genres != null && !genres.trim().isEmpty()) {
                List<String> genreList = Arrays.stream(genres.split(","))
                        .map(String::trim)
                        .filter(g -> !g.isEmpty())
                        .collect(Collectors.toList());

                if (!genreList.isEmpty()) {
                    filteredMovies = filteredMovies.stream()
                            .filter(movie -> {
                                if (movie.getGenreNm() == null) return false;
                                // AND 조건: 선택된 모든 장르를 포함해야 함
                                return genreList.stream().allMatch(genre -> movie.getGenreNm().contains(genre));
                            })
                            .collect(Collectors.toList());
                    log.info("장르 '{}'로 필터링된 영화: {}개", genres, filteredMovies.size());
                }
            }

            // 3. 정렬
            switch (sort) {
                case "name":
                    filteredMovies.sort((m1, m2) -> {
                        String name1 = Optional.ofNullable(m1.getMovieNm()).orElse("");
                        String name2 = Optional.ofNullable(m2.getMovieNm()).orElse("");
                        return name1.compareToIgnoreCase(name2);
                    });
                    break;
                case "random":
                    Collections.shuffle(filteredMovies);
                    log.info("랜덤순 정렬 완료");
                    break;
                case "tmdb_popularity":
                    filteredMovies.sort((m1, m2) -> {
                        Double popularity1 = m1.getTmdbPopularity();
                        Double popularity2 = m2.getTmdbPopularity();
                        if (popularity1 == null) return 1;
                        if (popularity2 == null) return -1;
                        return Double.compare(popularity2, popularity1); // 내림차순 (높은 인기도가 먼저)
                    });
                    log.info("TMDB 인기도순 정렬 완료");
                    break;
                case "openDt":
                    filteredMovies.sort((m1, m2) -> {
                        if (m1.getOpenDt() == null) return 1;
                        if (m2.getOpenDt() == null) return -1;
                        return m2.getOpenDt().compareTo(m1.getOpenDt()); // 내림차순 (최신 영화가 먼저)
                    });
                    log.info("개봉일순 정렬 완료");
                    break;
                case "date":
                default:
                    filteredMovies.sort((m1, m2) -> {
                        if (m1.getOpenDt() == null) return 1;
                        if (m2.getOpenDt() == null) return -1;
                        return m2.getOpenDt().compareTo(m1.getOpenDt());
                    });
                    break;
            }

            // 4. 페이지네이션
            int total = filteredMovies.size();
            int start = page * size;
            int end = Math.min(start + size, total);

            if (start >= total) {
                return ResponseEntity.ok(Map.of(
                        "data", Collections.emptyList(),
                        "total", total,
                        "page", page,
                        "size", size,
                        "totalPages", (int) Math.ceil((double) total / size),
                        "filters", Map.of(
                                "genres", genres != null ? genres : "",
                                "search", search != null ? search : "",
                                "sort", sort
                        )
                ));
            }

            List<MovieListDto> dtoList = movieListMapper.toDtoList(filteredMovies.subList(start, end));
            log.info("필터링 완료: 총 {}개 중 {} (페이지: {})", total, dtoList.size(), page);

            return ResponseEntity.ok(Map.of(
                    "data", dtoList,
                    "total", total,
                    "page", page,
                    "size", size,
                    "totalPages", (int) Math.ceil((double) total / size),
                    "filters", Map.of(
                            "genres", genres != null ? genres : "",
                            "search", search != null ? search : "",
                            "sort", sort
                    )
            ));

        } catch (Exception e) {
            log.error("영화 필터링 실패", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}

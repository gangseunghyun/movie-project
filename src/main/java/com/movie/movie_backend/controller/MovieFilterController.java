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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

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

            Pageable pageable = PageRequest.of(page, size);
            Page<MovieList> moviePage;

            if (search != null && !search.trim().isEmpty()) {
                moviePage = movieListRepository.findByMovieNmContainingIgnoreCase(search.trim(), pageable);
                log.info("검색어 '{}'로 필터링된 영화: {}개", search, moviePage.getTotalElements());
            } else {
                List<MovieList> allMovies = new ArrayList<>();
                int pageIdx = 0, chunkSize = 1000;
                do {
                    moviePage = movieListRepository.findAll(PageRequest.of(pageIdx++, chunkSize));
                    allMovies.addAll(moviePage.getContent());
                } while (moviePage.hasNext());
                log.info("전체 영화 조회: {}개", allMovies.size());
            }

            List<MovieList> filteredMovies = moviePage.getContent();

            // 포스터 URL이 없는 영화 제외
            filteredMovies = filteredMovies.stream()
                    .filter(movie -> movie.getPosterUrl() != null && 
                                   !movie.getPosterUrl().trim().isEmpty() && 
                                   !movie.getPosterUrl().equals("null"))
                    .collect(Collectors.toList());
            log.info("포스터 URL 필터링 후 영화: {}개", filteredMovies.size());

            // 장르 필터링 (자바에서 처리)
            if (genres != null && !genres.trim().isEmpty()) {
                Set<String> genreSet = Arrays.stream(genres.split(","))
                        .map(String::trim)
                        .collect(Collectors.toSet());
                filteredMovies = filteredMovies.stream()
                        .filter(movie -> {
                            if (movie.getGenreNm() == null) return false;
                            Set<String> movieGenres = new HashSet<>(Arrays.asList(movie.getGenreNm().split(",")));
                            return !Collections.disjoint(genreSet, movieGenres);
                        })
                        .collect(Collectors.toList());
                log.info("장르 필터링 후 영화: {}개", filteredMovies.size());
            }

            // 정렬 분기 추가
            if (sort != null) {
                switch (sort) {
                    case "openDt":
                        filteredMovies.sort(Comparator.comparing(MovieList::getOpenDt, Comparator.nullsLast(Comparator.reverseOrder())));
                        break;
                    case "name":
                        filteredMovies.sort(Comparator.comparing(MovieList::getMovieNm, Comparator.nullsLast(Comparator.naturalOrder())));
                        break;
                    case "random":
                        Collections.shuffle(filteredMovies);
                        break;
                    case "tmdb_popularity":
                    default:
                        filteredMovies.sort(Comparator.comparing(MovieList::getTmdbPopularity, Comparator.nullsLast(Comparator.reverseOrder())));
                        break;
                }
            }

            // page/size에 맞게 슬라이싱
            int fromIndex = Math.min(page * size, filteredMovies.size());
            int toIndex = Math.min(fromIndex + size, filteredMovies.size());
            List<MovieList> pagedMovies = filteredMovies.subList(fromIndex, toIndex);

            int total = filteredMovies.size();
            int totalPages = (int) Math.ceil((double) total / size);

            List<MovieListDto> dtoList = movieListMapper.toDtoList(pagedMovies);
            log.info("필터링 완료: 총 {}개 중 {} (페이지: {})", total, dtoList.size(), page);

            return ResponseEntity.ok(Map.of(
                    "data", dtoList,
                    "total", total,
                    "page", page,
                    "size", size,
                    "totalPages", totalPages,
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

package com.movie.movie_backend.controller;

import com.movie.movie_backend.dto.McpToolRequestDto;
import com.movie.movie_backend.dto.McpToolResponseDto;
import com.movie.movie_backend.dto.MovieDetailDto;
import com.movie.movie_backend.entity.MovieDetail;
import com.movie.movie_backend.repository.PRDMovieRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/mcp/tools")
@CrossOrigin(origins = "*")
public class McpServerController {
    
    @Autowired
    private PRDMovieRepository movieRepository;
    
    @PostMapping("/searchMovies")
    public ResponseEntity<McpToolResponseDto> searchMovies(@RequestBody McpToolRequestDto request) {
        try {
            Map<String, Object> params = request.getParameters();
            String genre = (String) params.get("genre");
            String situation = (String) params.get("situation");
            String type = (String) params.get("type");
            
            List<MovieDetail> movies;
            
            if (genre != null) {
                movies = searchByGenre(genre);
            } else if (situation != null) {
                movies = searchBySituation(situation);
            } else if (type != null) {
                movies = searchByType(type);
            } else {
                movies = searchPopularMovies();
            }
            
            List<MovieDetailDto> movieDtos = convertToMovieDetailDto(movies);
            
            McpToolResponseDto.MovieSearchResult result = McpToolResponseDto.MovieSearchResult.builder()
                    .movies(movieDtos)
                    .genre(genre)
                    .count(movieDtos.size())
                    .build();
            
            return ResponseEntity.ok(McpToolResponseDto.builder()
                    .tool("searchMovies")
                    .result(result)
                    .success(true)
                    .build());
                    
        } catch (Exception e) {
            return ResponseEntity.ok(McpToolResponseDto.builder()
                    .tool("searchMovies")
                    .success(false)
                    .error(e.getMessage())
                    .build());
        }
    }
    
    @PostMapping("/getMovieInfo")
    public ResponseEntity<McpToolResponseDto> getMovieInfo(@RequestBody McpToolRequestDto request) {
        try {
            Map<String, Object> params = request.getParameters();
            String title = (String) params.get("title");
            
            if (title == null || title.trim().isEmpty()) {
                throw new IllegalArgumentException("영화 제목이 필요합니다.");
            }
            
            List<MovieDetail> movies = movieRepository.findByMovieNmContainingIgnoreCase(title);
            
            if (movies.isEmpty()) {
                return ResponseEntity.ok(McpToolResponseDto.builder()
                        .tool("getMovieInfo")
                        .success(false)
                        .error("해당 영화를 찾을 수 없습니다: " + title)
                        .build());
            }
            
            MovieDetail movie = movies.get(0);
            MovieDetailDto movieDto = convertToMovieDetailDto(List.of(movie)).get(0);
            
            McpToolResponseDto.MovieInfoResult result = McpToolResponseDto.MovieInfoResult.builder()
                    .movie(movieDto)
                    .title(movie.getMovieNm())
                    .description(movie.getDescription())
                    .build();
            
            return ResponseEntity.ok(McpToolResponseDto.builder()
                    .tool("getMovieInfo")
                    .result(result)
                    .success(true)
                    .build());
                    
        } catch (Exception e) {
            return ResponseEntity.ok(McpToolResponseDto.builder()
                    .tool("getMovieInfo")
                    .success(false)
                    .error(e.getMessage())
                    .build());
        }
    }
    
    @GetMapping("/available")
    public ResponseEntity<Map<String, Object>> getAvailableTools() {
        Map<String, Object> tools = Map.of(
            "tools", List.of(
                Map.of(
                    "name", "searchMovies",
                    "description", "장르, 상황, 타입별 영화 검색",
                    "parameters", Map.of(
                        "genre", "장르명 (로맨스, 액션, 코미디 등)",
                        "situation", "상황 (우울할 때, 친구와 함께 등)",
                        "type", "타입 (인기, 개봉예정 등)"
                    )
                ),
                Map.of(
                    "name", "getMovieInfo",
                    "description", "영화 상세 정보 조회",
                    "parameters", Map.of(
                        "title", "영화 제목"
                    )
                )
            )
        );
        
        return ResponseEntity.ok(tools);
    }
    
    private List<MovieDetail> searchByGenre(String genre) {
        List<MovieDetail> movies = movieRepository.findByGenreNmContaining(genre);
        return movies.stream()
                .filter(m -> m.getAverageRating() != null)
                .sorted((a, b) -> Double.compare(b.getAverageRating(), a.getAverageRating()))
                .limit(5)
                .collect(Collectors.toList());
    }
    
    private List<MovieDetail> searchBySituation(String situation) {
        String lowerSituation = situation.toLowerCase();
        
        if (lowerSituation.contains("우울") || lowerSituation.contains("기분")) {
            return searchByGenres(new String[]{"코미디", "드라마"});
        } else if (lowerSituation.contains("친구")) {
            return searchByGenres(new String[]{"액션", "코미디", "애니메이션"});
        } else if (lowerSituation.contains("연인")) {
            return searchByGenres(new String[]{"로맨스", "드라마"});
        } else if (lowerSituation.contains("가족")) {
            return searchByGenres(new String[]{"애니메이션", "코미디", "드라마"});
        } else if (lowerSituation.contains("힐링")) {
            return searchByGenres(new String[]{"드라마", "다큐멘터리"});
        }
        
        return searchPopularMovies();
    }
    
    private List<MovieDetail> searchByType(String type) {
        String lowerType = type.toLowerCase();
        
        if (lowerType.contains("인기") || lowerType.contains("인기있는")) {
            return searchPopularMovies();
        } else if (lowerType.contains("개봉예정") || lowerType.contains("곧")) {
            return movieRepository.findByStatus(com.movie.movie_backend.constant.MovieStatus.COMING_SOON)
                    .stream()
                    .limit(5)
                    .collect(Collectors.toList());
        }
        
        return searchPopularMovies();
    }
    
    private List<MovieDetail> searchByGenres(String[] genres) {
        List<MovieDetail> allMovies = new java.util.ArrayList<>();
        for (String genre : genres) {
            List<MovieDetail> movies = movieRepository.findByGenreNmContaining(genre);
            allMovies.addAll(movies.stream()
                    .filter(m -> m.getAverageRating() != null && m.getAverageRating() >= 4.0)
                    .limit(2)
                    .collect(Collectors.toList()));
        }
        return allMovies.stream().limit(5).collect(Collectors.toList());
    }
    
    private List<MovieDetail> searchPopularMovies() {
        List<MovieDetail> popularMovies = movieRepository.findTop20ByOrderByTotalAudienceDesc();
        return popularMovies.stream()
                .filter(m -> m.getAverageRating() != null && m.getAverageRating() >= 4.0)
                .limit(5)
                .collect(Collectors.toList());
    }
    
    private List<MovieDetailDto> convertToMovieDetailDto(List<MovieDetail> movies) {
        return movies.stream().map(movie -> MovieDetailDto.builder()
                .movieCd(movie.getMovieCd())
                .movieNm(movie.getMovieNm())
                .movieNmEn(movie.getMovieNmEn())
                .genreNm(movie.getGenreNm())
                .openDt(movie.getOpenDt())
                .showTm(movie.getShowTm())
                .nationNm(movie.getNationNm())
                .description(movie.getDescription())
                .averageRating(movie.getAverageRating())
                .totalAudience(movie.getTotalAudience())
                .build()).collect(Collectors.toList());
    }
} 
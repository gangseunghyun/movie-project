package com.movie.movie_backend.service;

import com.movie.movie_backend.dto.McpToolResponseDto;
import com.movie.movie_backend.dto.MovieDetailDto;
import com.movie.movie_backend.dto.MovieListDto;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import java.time.Duration;
import com.movie.movie_backend.entity.MovieDetail;
import com.movie.movie_backend.entity.MovieList;
import com.movie.movie_backend.repository.MovieDetailRepository;
import com.movie.movie_backend.repository.PRDMovieListRepository;
import com.movie.movie_backend.mapper.MovieDetailMapper;
import com.movie.movie_backend.mapper.MovieListMapper;
import com.movie.movie_backend.constant.MovieStatus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class McpChatbotService {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private MovieDetailRepository movieDetailRepository;
    @Autowired
    private PRDMovieListRepository movieListRepository;
    @Autowired
    private MovieDetailMapper movieDetailMapper;
    @Autowired
    private MovieListMapper movieListMapper;
    @Autowired
    private TmdbPopularMovieService tmdbPopularMovieService;
    @Autowired
    private KobisPopularMovieService kobisPopularMovieService;
    
    /**
     * MCP 도구 요청을 처리하는 메서드
     * AI가 MCP 도구를 사용할 때 호출됨
     */
    public McpToolResponseDto handleMcpToolRequest(String tool, Map<String, Object> parameters) {
        try {
            if ("searchMovies".equals(tool)) {
                return handleSearchMoviesRequest(parameters);
            } else if ("getMovieInfo".equals(tool)) {
                return handleGetMovieInfoRequest(parameters);
            } else {
                return McpToolResponseDto.builder()
                        .tool(tool)
                        .success(false)
                        .error("지원하지 않는 도구입니다: " + tool)
                        .build();
            }
        } catch (Exception e) {
            return McpToolResponseDto.builder()
                    .tool(tool)
                    .success(false)
                    .error("도구 실행 중 오류 발생: " + e.getMessage())
                    .build();
        }
    }
    
    /**
     * searchMovies 도구 요청 처리
     * 실제 DB에서 영화 검색 결과를 반환
     */
    private McpToolResponseDto handleSearchMoviesRequest(Map<String, Object> parameters) {
        List<MovieDetailDto> movies = new ArrayList<>();
        
        // 실제 DB에서 영화 검색
        if (parameters.containsKey("situation")) {
            String situation = (String) parameters.get("situation");
            if ("우울할 때".equals(situation)) {
                movies.addAll(getHealingMovies());
            }
        } else if (parameters.containsKey("genre")) {
            String genre = (String) parameters.get("genre");
            if ("로맨스".equals(genre)) {
                movies.addAll(getRomanceMovies());
            } else if ("액션".equals(genre)) {
                movies.addAll(getActionMovies());
            }
        } else {
            // 인기 영화나 일반 추천의 경우 실제 최신 인기 영화 가져오기
            movies.addAll(getRealPopularMovies());
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("movies", movies);
        result.put("totalCount", movies.size());
        
        return McpToolResponseDto.builder()
                .tool("searchMovies")
                .success(true)
                .result(result)
                .build();
    }
    
    /**
     * 실제 DB에서 최신 인기 영화 가져오기
     */
    private List<MovieDetailDto> getRealPopularMovies() {
        List<MovieDetailDto> movies = new ArrayList<>();
        try {
            List<MovieDetail> popularMovies = movieDetailRepository.findTop20ByOrderByTotalAudienceDesc();
            popularMovies = popularMovies.stream().distinct().limit(5).collect(Collectors.toList());
            for (MovieDetail detail : popularMovies) {
                MovieList movieList = movieListRepository.findById(detail.getMovieCd()).orElse(null);
                MovieDetailDto dto = movieDetailMapper.toDto(detail, 0, false);
                if (movieList != null) {
                    dto.setPosterUrl(movieList.getPosterUrl());
                    dto.setStatus(movieList.getStatus() != null ? movieList.getStatus().name() : null);
                }
                movies.add(dto);
            }
        } catch (Exception e) {
            System.err.println("Error fetching popular movies: " + e.getMessage());
        }
        return movies;
    }
    
    /**
     * 실제 DB에서 로맨스 영화 가져오기
     */
    private List<MovieDetailDto> getRomanceMovies() {
        List<MovieDetailDto> movies = new ArrayList<>();
        try {
            List<MovieDetail> romanceMovies = movieDetailRepository.findByGenreNmContaining("로맨스");
            romanceMovies = romanceMovies.stream().distinct().limit(5).collect(Collectors.toList());
            for (MovieDetail detail : romanceMovies) {
                MovieList movieList = movieListRepository.findById(detail.getMovieCd()).orElse(null);
                MovieDetailDto dto = movieDetailMapper.toDto(detail, 0, false);
                if (movieList != null) {
                    dto.setPosterUrl(movieList.getPosterUrl());
                    dto.setStatus(movieList.getStatus() != null ? movieList.getStatus().name() : null);
                }
                movies.add(dto);
            }
        } catch (Exception e) {
            System.err.println("Error fetching romance movies: " + e.getMessage());
        }
        return movies;
    }
    
    /**
     * 실제 DB에서 액션 영화 가져오기
     */
    private List<MovieDetailDto> getActionMovies() {
        List<MovieDetailDto> movies = new ArrayList<>();
        try {
            List<MovieDetail> actionMovies = movieDetailRepository.findByGenreNmContaining("액션");
            actionMovies = actionMovies.stream().distinct().limit(5).collect(Collectors.toList());
            for (MovieDetail detail : actionMovies) {
                MovieList movieList = movieListRepository.findById(detail.getMovieCd()).orElse(null);
                MovieDetailDto dto = movieDetailMapper.toDto(detail, 0, false);
                if (movieList != null) {
                    dto.setPosterUrl(movieList.getPosterUrl());
                    dto.setStatus(movieList.getStatus() != null ? movieList.getStatus().name() : null);
                }
                movies.add(dto);
            }
        } catch (Exception e) {
            System.err.println("Error fetching action movies: " + e.getMessage());
        }
        return movies;
    }
    
    /**
     * 실제 DB에서 힐링 영화 가져오기
     */
    private List<MovieDetailDto> getHealingMovies() {
        List<MovieDetailDto> movies = new ArrayList<>();
        try {
            List<MovieDetail> healingMovies = new ArrayList<>();
            healingMovies.addAll(movieDetailRepository.findByGenreNmContaining("로맨스"));
            healingMovies.addAll(movieDetailRepository.findByGenreNmContaining("코미디"));
            healingMovies.addAll(movieDetailRepository.findByGenreNmContaining("드라마"));
            healingMovies = healingMovies.stream().distinct().limit(5).collect(Collectors.toList());

            for (MovieDetail detail : healingMovies) {
                MovieList movieList = movieListRepository.findById(detail.getMovieCd()).orElse(null);
                MovieDetailDto dto = movieDetailMapper.toDto(detail, 0, false);
                if (movieList != null) {
                    dto.setPosterUrl(movieList.getPosterUrl());
                    dto.setStatus(movieList.getStatus() != null ? movieList.getStatus().name() : null);
                }
                movies.add(dto);
            }
        } catch (Exception e) {
            System.err.println("Error fetching healing movies: " + e.getMessage());
        }
        return movies;
    }
    
    /**
     * getMovieInfo 도구 요청 처리
     * MovieList에서 포스터 URL과 함께 영화 상세 정보를 반환
     */
    private McpToolResponseDto handleGetMovieInfoRequest(Map<String, Object> parameters) {
        String movieName = (String) parameters.get("movieCd"); // 실제로는 영화명
        String redisKey = "movie:info:" + movieName;

        // 1. 영화명으로 LIKE 검색
        List<MovieList> byName = movieListRepository.findByMovieNmContainingIgnoreCase(movieName);
        if (!byName.isEmpty()) {
            MovieList movieList = byName.get(0); // 가장 유사한 영화 1개
            String movieCd = movieList.getMovieCd();
            MovieListDto movieListDto = movieListMapper.toDto(movieList);
            MovieDetail movieDetail = movieDetailRepository.findByMovieCd(movieCd);
            MovieDetailDto movie;
            if (movieDetail != null) {
                movie = movieDetailMapper.toDto(movieDetail, 0, false);
                if (movieListDto.getPosterUrl() != null && !movieListDto.getPosterUrl().isEmpty()) {
                    movie.setPosterUrl(movieListDto.getPosterUrl());
                }
            } else {
                movie = createMovieDetailDtoFromMovieList(movieListDto);
            }
            redisTemplate.opsForValue().set(redisKey, movie, java.time.Duration.ofHours(12));
            return buildMovieInfoResponse(movie, true, null);
        }

        // 결과 없음
        return buildMovieInfoResponse(null, false, "해당 영화 정보를 찾을 수 없습니다.");
    }
    
    /**
     * MovieList 정보로 MovieDetailDto 생성
     */
    private MovieDetailDto createMovieDetailDtoFromMovieList(MovieListDto movieListDto) {
        return MovieDetailDto.builder()
                .movieCd(movieListDto.getMovieCd())
                .movieNm(movieListDto.getMovieNm())
                .movieNmEn(movieListDto.getMovieNmEn())
                .openDt(movieListDto.getOpenDt())
                .genreNm(movieListDto.getGenreNm())
                .nationNm(movieListDto.getNationNm())
                .watchGradeNm(movieListDto.getWatchGradeNm())
                .status(movieListDto.getStatus() != null ? movieListDto.getStatus().toString() : null)
                .posterUrl(movieListDto.getPosterUrl())
                .description("영화에 대한 상세한 정보를 제공합니다.")
                .directors(new ArrayList<>())
                .actors(new ArrayList<>())
                .build();
    }
    
    private McpToolResponseDto buildMovieInfoResponse(MovieDetailDto movie, boolean success, String error) {
        Map<String, Object> result = new HashMap<>();
        result.put("movie", movie);
        return McpToolResponseDto.builder()
                .tool("getMovieInfo")
                .success(success)
                .result(result)
                .error(error)
                .build();
    }
} 
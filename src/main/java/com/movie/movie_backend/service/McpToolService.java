package com.movie.movie_backend.service;

import com.movie.movie_backend.dto.MovieDetailDto;
import com.movie.movie_backend.dto.MovieListDto;
import com.movie.movie_backend.entity.MovieDetail;
import com.movie.movie_backend.entity.MovieList;
import com.movie.movie_backend.repository.PRDMovieRepository;
import com.movie.movie_backend.repository.PRDMovieListRepository;
import com.movie.movie_backend.mapper.MovieListMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class McpToolService {

    @Autowired
    private PRDMovieRepository movieRepository;
    
    @Autowired
    private PRDMovieListRepository movieListRepository;
    
    @Autowired
    private MovieListMapper movieListMapper;

    public Object searchMovies(Map<String, Object> parameters) {
        String query = (String) parameters.get("query");
        String genre = (String) parameters.get("genre");
        String situation = (String) parameters.get("situation");
        String type = (String) parameters.get("type");
        
        List<MovieDetail> movies;
        
        if (query != null && !query.trim().isEmpty()) {
            movies = movieRepository.findByMovieNmContainingIgnoreCase(query);
        } else if (genre != null) {
            movies = searchByGenre(genre);
        } else if (situation != null) {
            movies = searchBySituation(situation);
        } else if (type != null) {
            movies = searchByType(type);
        } else {
            movies = searchPopularMovies();
        }
        
        List<MovieDetailDto> movieDtos = convertToMovieDetailDto(movies);
        
        Map<String, Object> result = new HashMap<>();
        result.put("movies", movieDtos);
        result.put("count", movieDtos.size());
        result.put("query", query);
        result.put("genre", genre);
        result.put("situation", situation);
        result.put("type", type);
        
        return result;
    }

    public Object getMovieInfo(Map<String, Object> parameters) {
        String movieCd = (String) parameters.get("movieCd");
        String title = (String) parameters.get("title");
        
        MovieDetail movie = null;
        
        if (movieCd != null) {
            movie = movieRepository.findByMovieCd(movieCd).orElse(null);
        } else if (title != null) {
            List<MovieDetail> movies = movieRepository.findByMovieNmContainingIgnoreCase(title);
            if (!movies.isEmpty()) {
                movie = movies.get(0);
            }
        }
        
        if (movie == null) {
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", "영화를 찾을 수 없습니다");
            return errorResult;
        }
        
        MovieDetailDto movieDto = convertToMovieDetailDto(List.of(movie)).get(0);
        
        Map<String, Object> result = new HashMap<>();
        result.put("movie", movieDto);
        result.put("movieCd", movie.getMovieCd());
        result.put("title", movie.getMovieNm());
        result.put("description", movie.getDescription());
        result.put("genre", movie.getGenreNm());
        result.put("averageRating", movie.getAverageRating());
        
        return result;
    }

    private List<MovieDetail> searchByGenre(String genre) {
        List<MovieDetail> movies = movieRepository.findByGenreNmContaining(genre);
        return movies.stream()
                // .filter(m -> m.getAverageRating() != null && m.getAverageRating() >= 4.0) // 평점 조건 주석처리
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
            return movieListRepository.findByStatus(com.movie.movie_backend.constant.MovieStatus.COMING_SOON)
                    .stream()
                    .map(ml -> movieRepository.findByMovieCd(ml.getMovieCd()).orElse(null))
                    .filter(java.util.Objects::nonNull)
                    .limit(5)
                    .collect(java.util.stream.Collectors.toList());
        }
        
        return searchPopularMovies();
    }
    
    private List<MovieDetail> searchByGenres(String[] genres) {
        List<MovieDetail> allMovies = new java.util.ArrayList<>();
        for (String genre : genres) {
            List<MovieDetail> movies = movieRepository.findByGenreNmContaining(genre);
            allMovies.addAll(movies.stream()
                // .filter(m -> m.getAverageRating() != null && m.getAverageRating() >= 4.0) // 평점 조건 주석처리
                .limit(5)
                .collect(Collectors.toList()));
        }
        return allMovies.stream().distinct().limit(10).collect(Collectors.toList());
    }
    
    private List<MovieDetail> searchPopularMovies() {
        List<MovieDetail> popularMovies = movieRepository.findTop20ByOrderByTotalAudienceDesc();
        return popularMovies.stream()
                // .filter(m -> m.getAverageRating() != null && m.getAverageRating() >= 4.0) // 평점 조건 주석처리
                .limit(5)
                .collect(Collectors.toList());
    }
    
    private List<MovieDetailDto> convertToMovieDetailDto(List<MovieDetail> movies) {
        return movies.stream().map(movie -> {
            String posterUrl = null;
            String status = null;
            Optional<MovieList> movieListOpt = movieListRepository.findById(movie.getMovieCd());
            if (movieListOpt.isPresent()) {
                MovieList movieList = movieListOpt.get();
                posterUrl = movieList.getPosterUrl();
                status = movieList.getStatus() != null ? movieList.getStatus().name() : null;
            }
            return MovieDetailDto.builder()
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
                    .posterUrl(posterUrl)
                    .status(status)
                    .build();
        }).collect(Collectors.toList());
    }
} 
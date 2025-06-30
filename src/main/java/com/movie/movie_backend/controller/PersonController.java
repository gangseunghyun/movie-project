package com.movie.movie_backend.controller;

import com.movie.movie_backend.entity.Director;
import com.movie.movie_backend.entity.Actor;
import com.movie.movie_backend.entity.MovieDetail;
import com.movie.movie_backend.repository.PRDDirectorRepository;
import com.movie.movie_backend.repository.PRDActorRepository;
import com.movie.movie_backend.repository.CastRepository;
import com.movie.movie_backend.repository.PRDMovieRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/person")
@RequiredArgsConstructor
public class PersonController {
    private final PRDDirectorRepository directorRepository;
    private final PRDActorRepository actorRepository;
    private final PRDMovieRepository movieRepository;
    private final CastRepository castRepository;

    // 감독 상세 + 감독한 영화 리스트
    @GetMapping("/director/{id}")
    public ResponseEntity<?> getDirectorDetail(@PathVariable Long id) {
        Optional<Director> directorOpt = directorRepository.findById(id);
        if (directorOpt.isEmpty()) return ResponseEntity.notFound().build();
        Director director = directorOpt.get();
        
        List<MovieDetail> movies = movieRepository.findAll().stream()
            .filter(m -> m.getDirector() != null && m.getDirector().getId().equals(id))
            .collect(Collectors.toList());
        
        // DTO로 변환
        Map<String, Object> personData = new HashMap<>();
        personData.put("id", director.getId());
        personData.put("name", director.getName());
        personData.put("birthDate", director.getBirthDate());
        personData.put("nationality", director.getNationality());
        personData.put("biography", director.getBiography());
        personData.put("photoUrl", director.getPhotoUrl());
        
        List<Map<String, Object>> movieData = movies.stream()
            .map(movie -> {
                Map<String, Object> movieMap = new HashMap<>();
                movieMap.put("id", movie.getId());
                movieMap.put("movieCd", movie.getMovieCd());
                movieMap.put("movieNm", movie.getMovieNm());
                movieMap.put("movieNmEn", movie.getMovieNmEn());
                movieMap.put("openDt", movie.getOpenDt());
                movieMap.put("averageRating", movie.getAverageRating());
                movieMap.put("posterUrl", movie.getMovieList() != null ? movie.getMovieList().getPosterUrl() : null);
                return movieMap;
            })
            .collect(Collectors.toList());
        
        Map<String, Object> response = new HashMap<>();
        response.put("person", personData);
        response.put("movies", movieData);
        
        return ResponseEntity.ok(response);
    }

    // 배우 상세 + 출연 영화 리스트
    @GetMapping("/actor/{id}")
    public ResponseEntity<?> getActorDetail(@PathVariable Long id) {
        Optional<Actor> actorOpt = actorRepository.findById(id);
        if (actorOpt.isEmpty()) return ResponseEntity.notFound().build();
        Actor actor = actorOpt.get();
        
        List<MovieDetail> movies = castRepository.findByActorIdOrderByMovieDetailOpenDtDesc(id)
            .stream().map(cast -> cast.getMovieDetail()).collect(Collectors.toList());
        
        // DTO로 변환
        Map<String, Object> personData = new HashMap<>();
        personData.put("id", actor.getId());
        personData.put("name", actor.getName());
        personData.put("birthDate", actor.getBirthDate());
        personData.put("nationality", actor.getNationality());
        personData.put("biography", actor.getBiography());
        personData.put("photoUrl", actor.getPhotoUrl());
        
        List<Map<String, Object>> movieData = movies.stream()
            .map(movie -> {
                Map<String, Object> movieMap = new HashMap<>();
                movieMap.put("id", movie.getId());
                movieMap.put("movieCd", movie.getMovieCd());
                movieMap.put("movieNm", movie.getMovieNm());
                movieMap.put("movieNmEn", movie.getMovieNmEn());
                movieMap.put("openDt", movie.getOpenDt());
                movieMap.put("averageRating", movie.getAverageRating());
                movieMap.put("posterUrl", movie.getMovieList() != null ? movie.getMovieList().getPosterUrl() : null);
                
                // 배우의 역할 정보 추가 (기본값으로 "출연" 설정)
                movieMap.put("roleTypeKor", "출연");
                
                return movieMap;
            })
            .collect(Collectors.toList());
        
        Map<String, Object> response = new HashMap<>();
        response.put("person", personData);
        response.put("movies", movieData);
        
        return ResponseEntity.ok(response);
    }
} 
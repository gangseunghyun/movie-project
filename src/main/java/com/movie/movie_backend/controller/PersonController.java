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
import java.util.Random;
import com.movie.movie_backend.mapper.MovieDetailMapper;

@RestController
@RequestMapping("/api/person")
@RequiredArgsConstructor
public class PersonController {
    private final PRDDirectorRepository directorRepository;
    private final PRDActorRepository actorRepository;
    private final PRDMovieRepository movieRepository;
    private final CastRepository castRepository;
    private final MovieDetailMapper movieDetailMapper;
    // 현재 추천된 배우 정보 (캐시)
    private Actor currentRecommendedActor = null;
    private final Random random = new Random();
    // 19금(청소년관람불가) 등급 문자열 목록
    private static final List<String> FORBIDDEN_GRADES = List.of("청소년관람불가", "19", "청불", "Restricted", "R");
    /**
     * 19금 영화인지 판별하는 함수
     */
    private boolean isAdultMovie(MovieDetail movie) {
        String grade = movie.getWatchGradeNm();
        if (grade == null || grade.isBlank()) return false;
        return FORBIDDEN_GRADES.stream().anyMatch(grade::contains);
    }

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

    /**
     * 현재 추천된 배우 정보 조회
     */
    @GetMapping("/recommended-actor")
    public ResponseEntity<?> getRecommendedActor() {
        try {
            // 추천 배우가 없으면 선정
            if (currentRecommendedActor == null) {
                selectRandomActor();
            }
            if (currentRecommendedActor == null) {
                return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "추천 배우가 없습니다."
                ));
            }
            // 배우 정보와 대표 작품 3개 조회
            Map<String, Object> recommendationInfo = getActorRecommendationInfo();
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", recommendationInfo
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "배우 추천 정보 조회에 실패했습니다: " + e.getMessage()
            ));
        }
    }

    /**
     * 5분마다 영화 2개 이상 출연한 배우 중에서 무작위로 선정
     */
    @org.springframework.scheduling.annotation.Scheduled(fixedRate = 300000) // 5분 = 300초
    public void selectRandomActor() {
        try {
            // 19금이 아닌 영화 2개 이상 출연한 배우들 조회
            List<Actor> eligibleActors = getActorsWithMinNonAdultMovies(2);
            if (eligibleActors.isEmpty()) {
                return;
            }
            // 무작위로 배우 선정
            Actor selectedActor = eligibleActors.get(random.nextInt(eligibleActors.size()));
            currentRecommendedActor = selectedActor;
        } catch (Exception e) {
            // 로그는 나중에 추가
        }
    }

    /**
     * 19금이 아닌 영화 2개 이상 출연한 배우들 조회
     */
    private List<Actor> getActorsWithMinNonAdultMovies(int minMovieCount) {
        List<Actor> allActors = actorRepository.findAll();
        return allActors.stream()
            .filter(actor -> {
                List<MovieDetail> movies = castRepository.findByActorIdOrderByMovieDetailOpenDtDesc(actor.getId())
                    .stream()
                    .map(cast -> cast.getMovieDetail())
                    .filter(movie -> !isAdultMovie(movie))
                    .collect(Collectors.toList());
                return movies.size() >= minMovieCount;
            })
            .collect(Collectors.toList());
    }

    /**
     * 배우별 영화 개수 조회
     */
    private long getMovieCountByActor(Long actorId) {
        return castRepository.findByActorIdOrderByMovieDetailOpenDtDesc(actorId).size();
    }

    /**
     * 배우별 평균 평점 조회
     */
    private double getAverageRatingByActor(Long actorId) {
        List<MovieDetail> movies = castRepository.findByActorIdOrderByMovieDetailOpenDtDesc(actorId)
            .stream()
            .map(cast -> cast.getMovieDetail())
            .collect(Collectors.toList());
        if (movies.isEmpty()) return 0.0;
        double totalRating = movies.stream()
            .mapToDouble(movie -> movie.getAverageRating() != null ? movie.getAverageRating() : 0.0)
            .sum();
        return totalRating / movies.size();
    }

    /**
     * 배우 정보와 대표 작품 3개 조회
     */
    private Map<String, Object> getActorRecommendationInfo() {
        List<MovieDetail> allMovies = castRepository.findByActorIdOrderByMovieDetailOpenDtDesc(currentRecommendedActor.getId())
            .stream()
            .map(cast -> cast.getMovieDetail())
            .filter(movie -> !isAdultMovie(movie)) // 19금 영화 제외
            .collect(Collectors.toList());
        // 별점순으로 상위 3개 작품
        List<MovieDetail> topMovies = allMovies.stream()
            .sorted((m1, m2) -> {
                double rating1 = m1.getAverageRating() != null ? m1.getAverageRating() : 0.0;
                double rating2 = m2.getAverageRating() != null ? m2.getAverageRating() : 0.0;
                return Double.compare(rating2, rating1);
            })
            .limit(3)
            .collect(Collectors.toList());
        Map<String, Object> actorDto = new HashMap<>();
        actorDto.put("id", currentRecommendedActor.getId());
        actorDto.put("name", currentRecommendedActor.getName());
        actorDto.put("birthDate", currentRecommendedActor.getBirthDate());
        actorDto.put("nationality", currentRecommendedActor.getNationality());
        actorDto.put("biography", currentRecommendedActor.getBiography());
        actorDto.put("photoUrl", currentRecommendedActor.getPhotoUrl());
        Map<String, Object> result = new HashMap<>();
        result.put("actor", actorDto);
        result.put("movieCount", allMovies.size());
        result.put("averageRating", getAverageRatingByActor(currentRecommendedActor.getId()));
        result.put("topMovies", topMovies.stream()
            .map(movie -> movieDetailMapper.toDto(movie, 0, false))
            .collect(Collectors.toList()));
        return result;
    }

    /**
     * 수동으로 배우 추천 새로고침 (관리자용)
     */
    @PostMapping("/refresh-recommended-actor")
    public ResponseEntity<?> refreshRecommendedActor() {
        try {
            selectRandomActor();
            Map<String, Object> newRecommendation = getActorRecommendationInfo();
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "배우 추천이 새로고침되었습니다.",
                "data", newRecommendation
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "배우 추천 새로고침에 실패했습니다: " + e.getMessage()
            ));
        }
    }
} 
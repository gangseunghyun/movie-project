package com.movie.movie_backend.controller;

import com.movie.movie_backend.entity.Director;
import com.movie.movie_backend.entity.Actor;
import com.movie.movie_backend.entity.MovieDetail;
import com.movie.movie_backend.repository.PRDDirectorRepository;
import com.movie.movie_backend.repository.PRDActorRepository;
import com.movie.movie_backend.repository.CastRepository;
import com.movie.movie_backend.repository.PRDMovieRepository;
import com.movie.movie_backend.repository.PRDMovieListRepository;
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
import com.movie.movie_backend.service.PersonLikeService;
import com.movie.movie_backend.entity.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import com.movie.movie_backend.repository.USRUserRepository;
import com.movie.movie_backend.constant.Provider;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import com.movie.movie_backend.service.REVRatingService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.ArrayList;

@RestController
@RequestMapping("/api/person")
@RequiredArgsConstructor
public class PersonController {
    private final PRDDirectorRepository directorRepository;
    private final PRDActorRepository actorRepository;
    private final PRDMovieRepository movieRepository;
    private final PRDMovieListRepository movieListRepository;
    private final CastRepository castRepository;
    private final MovieDetailMapper movieDetailMapper;
    private final PersonLikeService personLikeService;
    private final USRUserRepository userRepository;
    private final REVRatingService ratingService;
    // 현재 추천된 배우 정보 (캐시)
    private Actor currentRecommendedActor = null;
    // 현재 추천된 감독 정보 (캐시)
    private Director currentRecommendedDirector = null;
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

    private <T> List<T> getAllChunked(JpaRepository<T, ?> repository) {
        List<T> all = new ArrayList<>();
        int page = 0, size = 1000;
        Page<T> pageResult;
        do {
            pageResult = repository.findAll(PageRequest.of(page++, size));
            all.addAll(pageResult.getContent());
        } while (pageResult.hasNext());
        return all;
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
            // 19금이 아닌 영화 6개 이상 출연한 배우들 조회
            List<Actor> eligibleActors = getActorsWithMinNonAdultMovies(6);
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
     * 19금이 아닌 영화 6개 이상 출연한 배우들 조회 (포스터가 있는 영화만 카운트)
     */
    private List<Actor> getActorsWithMinNonAdultMovies(int minMovieCount) {
        List<Actor> allActors = actorRepository.findAll();
        return allActors.stream()
            .filter(actor -> {
                List<MovieDetail> movies = castRepository.findByActorIdOrderByMovieDetailOpenDtDesc(actor.getId())
                    .stream()
                    .map(cast -> cast.getMovieDetail())
                    .filter(movie -> !isAdultMovie(movie))
                    .filter(movie -> hasPoster(movie)) // 포스터가 있는 영화만 카운트
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
     * 배우 정보와 모든 작품 조회
     */
    private Map<String, Object> getActorRecommendationInfo() {
        List<MovieDetail> allMovies = castRepository.findByActorIdOrderByMovieDetailOpenDtDesc(currentRecommendedActor.getId())
            .stream()
            .map(cast -> cast.getMovieDetail())
            .filter(movie -> !isAdultMovie(movie)) // 19금 영화 제외
            .distinct() // 중복 제거
            .collect(Collectors.toList());
        
        // 배치 평점 조회로 성능 최적화
        List<String> movieCds = allMovies.stream()
                .map(MovieDetail::getMovieCd)
                .collect(Collectors.toList());
        Map<String, Double> averageRatings = ratingService.getAverageRatingsForMovies(movieCds);
        
        // 평점 정보를 MovieDetail에 설정
        allMovies.forEach(movie -> {
            Double rating = averageRatings.get(movie.getMovieCd());
            movie.setAverageRating(rating);
        });
        
        // 개봉일순으로 정렬 (최신순)
        List<MovieDetail> sortedMovies = allMovies.stream()
            .sorted((m1, m2) -> {
                if (m1.getOpenDt() == null && m2.getOpenDt() == null) return 0;
                if (m1.getOpenDt() == null) return 1;
                if (m2.getOpenDt() == null) return -1;
                return m2.getOpenDt().compareTo(m1.getOpenDt()); // 최신순
            })
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
        result.put("allMovies", sortedMovies.stream()
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

    /**
     * 현재 추천된 감독 정보 조회
     */
    @GetMapping("/recommended-director")
    public ResponseEntity<?> getRecommendedDirector() {
        try {
            // 추천 감독이 없으면 선정
            if (currentRecommendedDirector == null) {
                selectRandomDirector();
            }
            if (currentRecommendedDirector == null) {
                return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "추천 감독이 없습니다."
                ));
            }
            // 감독 정보와 대표 작품 3개 조회
            Map<String, Object> recommendationInfo = getDirectorRecommendationInfo();
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", recommendationInfo
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "감독 추천 정보 조회에 실패했습니다: " + e.getMessage()
            ));
        }
    }

    /**
     * 5분마다 영화 6개 이상 감독한 감독 중에서 무작위로 선정
     */
    @org.springframework.scheduling.annotation.Scheduled(fixedRate = 300000) // 5분 = 300초
    public void selectRandomDirector() {
        try {
            // 19금이 아닌 영화 6개 이상 감독한 감독들 조회
            List<Director> eligibleDirectors = getDirectorsWithMinNonAdultMovies(6);
            if (eligibleDirectors.isEmpty()) {
                return;
            }
            // 무작위로 감독 선정
            Director selectedDirector = eligibleDirectors.get(random.nextInt(eligibleDirectors.size()));
            currentRecommendedDirector = selectedDirector;
        } catch (Exception e) {
            // 로그는 나중에 추가
        }
    }

    /**
     * 19금이 아닌 영화 6개 이상 감독한 감독들 조회 (포스터가 있는 영화만 카운트)
     */
    private List<Director> getDirectorsWithMinNonAdultMovies(int minMovieCount) {
        List<Director> allDirectors = directorRepository.findAll();
        return allDirectors.stream()
            .filter(director -> {
                List<MovieDetail> movies = movieRepository.findAll().stream()
                    .filter(movie -> movie.getDirector() != null && movie.getDirector().getId().equals(director.getId()))
                    .filter(movie -> !isAdultMovie(movie))
                    .filter(movie -> hasPoster(movie)) // 포스터가 있는 영화만 카운트
                    .collect(Collectors.toList());
                return movies.size() >= minMovieCount;
            })
            .collect(Collectors.toList());
    }

    /**
     * 영화에 포스터가 있는지 확인하는 메서드
     */
    private boolean hasPoster(MovieDetail movie) {
        try {
            // MovieList에서 포스터 URL 확인
            var movieListOpt = movieListRepository.findById(movie.getMovieCd());
            if (movieListOpt.isPresent()) {
                var movieList = movieListOpt.get();
                if (movieList.getPosterUrl() != null && 
                    !movieList.getPosterUrl().trim().isEmpty() && 
                    !movieList.getPosterUrl().equals("null")) {
                    return true;
                }
            }
            
            // 스틸컷이 있으면 포스터로 대체 가능
            if (movie.getStillcuts() != null && !movie.getStillcuts().isEmpty()) {
                return true;
            }
            
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 감독별 영화 개수 조회
     */
    private long getMovieCountByDirector(Long directorId) {
        return movieRepository.findAll().stream()
            .filter(movie -> movie.getDirector() != null && movie.getDirector().getId().equals(directorId))
            .count();
    }

    /**
     * 감독별 평균 평점 조회
     */
    private double getAverageRatingByDirector(Long directorId) {
        List<MovieDetail> movies = movieRepository.findAll().stream()
            .filter(movie -> movie.getDirector() != null && movie.getDirector().getId().equals(directorId))
            .collect(Collectors.toList());
        if (movies.isEmpty()) return 0.0;
        double totalRating = movies.stream()
            .mapToDouble(movie -> movie.getAverageRating() != null ? movie.getAverageRating() : 0.0)
            .sum();
        return totalRating / movies.size();
    }

    /**
     * 감독 정보와 모든 작품 조회
     */
    private Map<String, Object> getDirectorRecommendationInfo() {
        List<MovieDetail> allMovies = movieRepository.findAll().stream()
            .filter(movie -> movie.getDirector() != null && movie.getDirector().getId().equals(currentRecommendedDirector.getId()))
            .filter(movie -> !isAdultMovie(movie)) // 19금 영화 제외
            .distinct() // 중복 제거
            .collect(Collectors.toList());
        
        // 배치 평점 조회로 성능 최적화
        List<String> movieCds = allMovies.stream()
                .map(MovieDetail::getMovieCd)
                .collect(Collectors.toList());
        Map<String, Double> averageRatings = ratingService.getAverageRatingsForMovies(movieCds);
        
        // 평점 정보를 MovieDetail에 설정
        allMovies.forEach(movie -> {
            Double rating = averageRatings.get(movie.getMovieCd());
            movie.setAverageRating(rating);
        });
        
        // 개봉일순으로 정렬 (최신순)
        List<MovieDetail> sortedMovies = allMovies.stream()
            .sorted((m1, m2) -> {
                if (m1.getOpenDt() == null && m2.getOpenDt() == null) return 0;
                if (m1.getOpenDt() == null) return 1;
                if (m2.getOpenDt() == null) return -1;
                return m2.getOpenDt().compareTo(m1.getOpenDt()); // 최신순
            })
            .collect(Collectors.toList());
        Map<String, Object> directorDto = new HashMap<>();
        directorDto.put("id", currentRecommendedDirector.getId());
        directorDto.put("name", currentRecommendedDirector.getName());
        directorDto.put("birthDate", currentRecommendedDirector.getBirthDate());
        directorDto.put("nationality", currentRecommendedDirector.getNationality());
        directorDto.put("biography", currentRecommendedDirector.getBiography());
        directorDto.put("photoUrl", currentRecommendedDirector.getPhotoUrl());
        Map<String, Object> result = new HashMap<>();
        result.put("director", directorDto);
        result.put("movieCount", allMovies.size());
        result.put("averageRating", getAverageRatingByDirector(currentRecommendedDirector.getId()));
        result.put("allMovies", sortedMovies.stream()
            .map(movie -> movieDetailMapper.toDto(movie, 0, false))
            .collect(Collectors.toList()));
        return result;
    }

    /**
     * 수동으로 감독 추천 새로고침 (관리자용)
     */
    @PostMapping("/refresh-recommended-director")
    public ResponseEntity<?> refreshRecommendedDirector() {
        try {
            selectRandomDirector();
            Map<String, Object> newRecommendation = getDirectorRecommendationInfo();
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "감독 추천이 새로고침되었습니다.",
                "data", newRecommendation
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "감독 추천 새로고침에 실패했습니다: " + e.getMessage()
            ));
        }
    }

    // principal에서 User 엔티티 안전하게 추출하는 메서드
    private User extractCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) return null;
        Object principal = authentication.getPrincipal();
        if (principal instanceof User user) {
            return user;
        } else if (principal instanceof DefaultOAuth2User oAuth2User) {
            String email = oAuth2User.getAttribute("email");
            String provider = oAuth2User.getAttribute("provider");
            String providerId = oAuth2User.getAttribute("providerId");
            // 카카오의 경우 email이 kakao_account 안에 있을 수 있음
            if (email == null && "KAKAO".equals(provider)) {
                Object kakaoAccountObj = oAuth2User.getAttribute("kakao_account");
                if (kakaoAccountObj instanceof java.util.Map kakaoAccount) {
                    email = (String) kakaoAccount.get("email");
                }
            }
            if (provider != null && providerId != null) {
                try {
                    Provider providerEnum = Provider.valueOf(provider.toUpperCase());
                    return userRepository.findByProviderAndProviderId(providerEnum, providerId).orElse(null);
                } catch (Exception e) {
                    return null;
                }
            } else if (email != null) {
                return userRepository.findByEmail(email).orElse(null);
            }
        }
        return null;
    }

    // ===== 인물 좋아요 API =====

    /**
     * 배우 좋아요 추가
     */
    @PostMapping("/actor/{id}/like")
    public ResponseEntity<?> likeActor(@PathVariable Long id) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            User user = extractCurrentUser(authentication);
            if (user == null) {
                return ResponseEntity.status(401).body(Map.of("success", false, "message", "로그인이 필요합니다."));
            }
            personLikeService.likeActor(user.getId(), id);
            return ResponseEntity.ok(Map.of("success", true, "message", "배우를 좋아요했습니다."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * 배우 좋아요 취소
     */
    @DeleteMapping("/actor/{id}/like")
    public ResponseEntity<?> unlikeActor(@PathVariable Long id) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            User user = extractCurrentUser(authentication);
            if (user == null) {
                return ResponseEntity.status(401).body(Map.of("success", false, "message", "로그인이 필요합니다."));
            }
            personLikeService.unlikeActor(user.getId(), id);
            return ResponseEntity.ok(Map.of("success", true, "message", "배우 좋아요를 취소했습니다."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * 감독 좋아요 추가
     */
    @PostMapping("/director/{id}/like")
    public ResponseEntity<?> likeDirector(@PathVariable Long id) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            User user = extractCurrentUser(authentication);
            if (user == null) {
                return ResponseEntity.status(401).body(Map.of("success", false, "message", "로그인이 필요합니다."));
            }
            personLikeService.likeDirector(user.getId(), id);
            return ResponseEntity.ok(Map.of("success", true, "message", "감독을 좋아요했습니다."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * 감독 좋아요 취소
     */
    @DeleteMapping("/director/{id}/like")
    public ResponseEntity<?> unlikeDirector(@PathVariable Long id) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            User user = extractCurrentUser(authentication);
            if (user == null) {
                return ResponseEntity.status(401).body(Map.of("success", false, "message", "로그인이 필요합니다."));
            }
            personLikeService.unlikeDirector(user.getId(), id);
            return ResponseEntity.ok(Map.of("success", true, "message", "감독 좋아요를 취소했습니다."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * 배우 좋아요 상태 확인
     */
    @GetMapping("/actor/{id}/like-status")
    public ResponseEntity<?> getActorLikeStatus(@PathVariable Long id) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
                return ResponseEntity.ok(Map.of("likedByMe", false, "likeCount", personLikeService.getActorLikeCount(id)));
            }

            User user = null;
            Object principal = authentication.getPrincipal();
            if (principal instanceof User) {
                user = (User) principal;
            } else if (principal instanceof org.springframework.security.oauth2.core.user.DefaultOAuth2User oauth2User) {
                String provider = oauth2User.getAttribute("provider");
                String providerId = oauth2User.getAttribute("providerId");
                if (provider != null && providerId != null) {
                    user = userRepository.findByProviderAndProviderId(
                        com.movie.movie_backend.constant.Provider.valueOf(provider.toUpperCase()), providerId
                    ).orElse(null);
                }
            } else {
                String loginId = authentication.getName();
                user = userRepository.findByLoginId(loginId).orElse(null);
            }

            boolean likedByMe = user != null && personLikeService.hasUserLikedActor(user.getId(), id);
            long likeCount = personLikeService.getActorLikeCount(id);
            return ResponseEntity.ok(Map.of("likedByMe", likedByMe, "likeCount", likeCount));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * 감독 좋아요 상태 확인
     */
    @GetMapping("/director/{id}/like-status")
    public ResponseEntity<?> getDirectorLikeStatus(@PathVariable Long id) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
                return ResponseEntity.ok(Map.of("likedByMe", false, "likeCount", personLikeService.getDirectorLikeCount(id)));
            }

            User user = null;
            Object principal = authentication.getPrincipal();
            if (principal instanceof User) {
                user = (User) principal;
            } else if (principal instanceof org.springframework.security.oauth2.core.user.DefaultOAuth2User oauth2User) {
                String provider = oauth2User.getAttribute("provider");
                String providerId = oauth2User.getAttribute("providerId");
                if (provider != null && providerId != null) {
                    user = userRepository.findByProviderAndProviderId(
                        com.movie.movie_backend.constant.Provider.valueOf(provider.toUpperCase()), providerId
                    ).orElse(null);
                }
            } else {
                String loginId = authentication.getName();
                user = userRepository.findByLoginId(loginId).orElse(null);
            }

            boolean likedByMe = user != null && personLikeService.hasUserLikedDirector(user.getId(), id);
            long likeCount = personLikeService.getDirectorLikeCount(id);
            return ResponseEntity.ok(Map.of("likedByMe", likedByMe, "likeCount", likeCount));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }
} 
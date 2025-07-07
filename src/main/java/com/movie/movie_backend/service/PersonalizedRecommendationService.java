package com.movie.movie_backend.service;

import com.movie.movie_backend.dto.RecommendationDto;
import com.movie.movie_backend.entity.MovieDetail;
import com.movie.movie_backend.entity.RecommendationLog;
import com.movie.movie_backend.entity.Cast;
import com.movie.movie_backend.entity.User;
import com.movie.movie_backend.entity.Tag;
import com.movie.movie_backend.entity.Review;
import com.movie.movie_backend.repository.PersonLikeRepository;
import com.movie.movie_backend.repository.MovieDetailRepository;
import com.movie.movie_backend.repository.REVLikeRepository;
import com.movie.movie_backend.repository.RecommendationLogRepository;
import com.movie.movie_backend.repository.USRUserRepository;
import com.movie.movie_backend.repository.PRDMovieRepository;
import com.movie.movie_backend.repository.PRDTagRepository;
import com.movie.movie_backend.repository.REVReviewRepository;
import com.movie.movie_backend.repository.PRDMovieListRepository;
import com.movie.movie_backend.constant.RoleType;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PersonalizedRecommendationService {
    private final PersonLikeRepository personLikeRepository;
    private final MovieDetailRepository movieDetailRepository;
    private final REVLikeRepository likeRepository;
    private final RecommendationLogRepository recommendationLogRepository;
    private final USRUserRepository userRepository;
    private final PRDMovieRepository movieRepository;
    private final PRDTagRepository tagRepository;
    private final REVReviewRepository reviewRepository;
    private final PRDMovieListRepository movieListRepository;

    @Transactional
    // @Cacheable(value = "recommendations", key = "#userId + '_' + #page + '_' + #size")  // 임시로 캐시 비활성화
    public List<RecommendationDto> recommendByLikedPeople(Long userId, int page, int size) {
        System.out.println("=== [추천 디버깅] recommendByLikedPeople 진입: userId=" + userId + ", page=" + page + ", size=" + size);
        System.out.println("=== [테스트] 이 로그가 보이면 코드가 실행되고 있습니다!");
        // 1. 좋아요한 감독/배우 ID 조회
        List<Long> directorIds = personLikeRepository.findLikedDirectorIdsByUserId(userId);
        List<Long> actorIds = personLikeRepository.findLikedActorIdsByUserId(userId);

        // 2. 사용자 선호태그 조회
        User user = userRepository.findById(userId).orElse(null);
        List<Tag> preferredTags = user != null ? user.getPreferredTags() : List.of();
        System.out.println("[추천] userId=" + userId + ", 선호태그=" + preferredTags.stream().map(Tag::getName).collect(Collectors.toList()));

        // 3. 감독/배우가 참여한 영화 후보군 추출
        List<MovieDetail> directorMovies = directorIds.isEmpty() ? List.of() : movieDetailRepository.findMoviesByDirectorIds(directorIds);
        List<MovieDetail> actorMovies = actorIds.isEmpty() ? List.of() : movieDetailRepository.findMoviesByMainActorIds(actorIds, RoleType.LEAD);

        // 4. 선호태그 기반 영화 후보군 추출
        List<MovieDetail> tagMovies = preferredTags.isEmpty() ? List.of() : movieRepository.findMoviesByTags(preferredTags);

        // 5. 평점/찜한 영화와 연관된 영화 후보군 추출
        Set<Long> likedMovieIds = new HashSet<>(likeRepository.findMovieIdsByUserId(userId));
        Set<Long> ratedMovieIds = new HashSet<>(reviewRepository.findRatedMovieIdsByUserId(userId, Review.ReviewStatus.ACTIVE));
        Set<Long> reviewedMovieIds = new HashSet<>(reviewRepository.findMovieIdsByUserId(userId, Review.ReviewStatus.ACTIVE));
        
        // 평점/찜한 영화들의 감독/배우/장르 정보 수집
        Set<Long> relatedDirectorIds = new HashSet<>();
        Set<Long> relatedActorIds = new HashSet<>();
        Set<String> relatedGenres = new HashSet<>();
        
        // 찜한 영화들의 정보 수집
        for (Long movieId : likedMovieIds) {
            movieDetailRepository.findById(movieId).ifPresent(movie -> {
                if (movie.getDirector() != null) {
                    relatedDirectorIds.add(movie.getDirector().getId());
                }
                movie.getCasts().stream()
                    .filter(c -> RoleType.LEAD.equals(c.getRoleType()))
                    .forEach(c -> relatedActorIds.add(c.getActor().getId()));
                if (movie.getGenreNm() != null) {
                    Arrays.stream(movie.getGenreNm().split(","))
                        .map(String::trim)
                        .forEach(relatedGenres::add);
                }
            });
        }
        
        // 평점 4점 이상 준 영화들의 정보 수집 (감독/배우/장르 모두)
        Set<String> highRatedGenres = new HashSet<>();
        for (Long movieId : ratedMovieIds) {
            movieDetailRepository.findById(movieId).ifPresent(movie -> {
                // 평점 4점 이상인지 확인
                boolean isHighRated = movie.getRatings().stream()
                    .anyMatch(rating -> rating.getUser().getId().equals(userId) && rating.getScore() >= 4.0);
                
                if (isHighRated) {
                    // 평점 4점 이상인 영화의 감독/배우/장르 모두 수집
                    if (movie.getDirector() != null) {
                        relatedDirectorIds.add(movie.getDirector().getId());
                    }
                    movie.getCasts().stream()
                        .filter(c -> RoleType.LEAD.equals(c.getRoleType()))
                        .forEach(c -> relatedActorIds.add(c.getActor().getId()));
                    if (movie.getGenreNm() != null) {
                        Arrays.stream(movie.getGenreNm().split(","))
                            .map(String::trim)
                            .forEach(highRatedGenres::add);
                    }
                }
            });
        }
        relatedGenres.addAll(highRatedGenres);
        
        // 리뷰만 한 영화는 관련 감독/배우/장르에 포함하지 않음 (찜하거나 평점 4점 이상 준 영화만 포함)
        // for (Long movieId : reviewedMovieIds) {
        //     movieDetailRepository.findById(movieId).ifPresent(movie -> {
        //         if (movie.getDirector() != null) {
        //             relatedDirectorIds.add(movie.getDirector().getId());
        //         }
        //         movie.getCasts().stream()
        //             .filter(c -> RoleType.LEAD.equals(c.getRoleType()))
        //             .forEach(c -> relatedActorIds.add(c.getActor().getId()));
        //         // 리뷰만 한 영화는 관련 장르에 포함하지 않음
        //     });
        // }
        
        System.out.println("[추천] 평점/찜한 영화 연관 정보 - 감독: " + relatedDirectorIds.size() + "명, 배우: " + relatedActorIds.size() + "명, 관련 장르(평점4점이상): " + relatedGenres);
        System.out.println("[추천 디버깅] 관련 감독 ID: " + relatedDirectorIds);
        System.out.println("[추천 디버깅] 관련 배우 ID: " + relatedActorIds);
        System.out.println("[추천 디버깅] 찜한 영화 수: " + likedMovieIds.size() + ", 평점한 영화 수: " + ratedMovieIds.size() + ", 리뷰한 영화 수: " + reviewedMovieIds.size());
        if (!relatedGenres.isEmpty()) {
            System.out.println("[추천 디버깅] 관련 장르 상세: " + relatedGenres);
        }
        
        // 연관 영화 후보군 추출
        List<MovieDetail> relatedDirectorMovies = relatedDirectorIds.isEmpty() ? List.of() : movieDetailRepository.findMoviesByDirectorIds(new ArrayList<>(relatedDirectorIds));
        List<MovieDetail> relatedActorMovies = relatedActorIds.isEmpty() ? List.of() : movieDetailRepository.findMoviesByMainActorIds(new ArrayList<>(relatedActorIds), RoleType.LEAD);
        
        // 장르 기반 영화 추출 (선호태그와 동일한 방식)
        List<MovieDetail> relatedGenreMovies = new ArrayList<>();
        if (!relatedGenres.isEmpty()) {
            List<Tag> relatedGenreTags = tagRepository.findGenreTags().stream()
                .filter(tag -> relatedGenres.contains(tag.getName()))
                .collect(Collectors.toList());
            if (!relatedGenreTags.isEmpty()) {
                relatedGenreMovies = movieRepository.findMoviesByTags(relatedGenreTags);
            }
        }

        // 6. 후보군 합치기(중복 제거)
        Set<MovieDetail> candidates = new HashSet<>();
        candidates.addAll(directorMovies);
        candidates.addAll(actorMovies);
        candidates.addAll(tagMovies);
        candidates.addAll(relatedDirectorMovies);
        candidates.addAll(relatedActorMovies);
        candidates.addAll(relatedGenreMovies);

        // 후보군에서 movieCd가 null이거나 movie 자체가 null인 객체 제거
        candidates.removeIf(movie -> movie == null || movie.getMovieCd() == null);
        
        // 7. 이미 본 영화들을 후보군에서 제외
        candidates.removeIf(movie -> 
            likedMovieIds.contains(movie.getId()) || 
            ratedMovieIds.contains(movie.getId()) || 
            reviewedMovieIds.contains(movie.getId())
        );
        
        System.out.println("[추천] 후보군에서 제외된 영화 - 찜한 영화: " + likedMovieIds.size() + "개, 평점한 영화: " + ratedMovieIds.size() + "개, 리뷰한 영화: " + reviewedMovieIds.size() + "개");
        System.out.println("[추천] 최종 후보군 영화 수: " + candidates.size());

        // 7. 내가 이미 본/평점/찜한 영화 ID 조회 (중복 제거)
        // likedMovieIds, ratedMovieIds, reviewedMovieIds는 이미 위에서 정의됨
        System.out.println("[추천] userId=" + userId + ", 찜한 영화: " + likedMovieIds.size() + "개, 평점한 영화: " + ratedMovieIds.size() + "개, 리뷰한 영화: " + reviewedMovieIds.size() + "개");

        // 찜/평점/리뷰/관련 장르 로그는 기존 위치에 아래처럼 추가
        System.out.println("[추천 디버깅] 찜한 영화 ID: " + likedMovieIds);
        System.out.println("[추천 디버깅] 평점한 영화 ID: " + ratedMovieIds);
        System.out.println("[추천 디버깅] 리뷰한 영화 ID: " + reviewedMovieIds);
        System.out.println("[추천 디버깅] 관련 장르(평점4점이상/찜): " + relatedGenres);
        
        // 리뷰한 영화의 장르 정보 확인
        for (Long movieId : reviewedMovieIds) {
            movieDetailRepository.findById(movieId).ifPresent(movie -> {
                System.out.println("[추천 디버깅] 리뷰한 영화 '" + movie.getMovieNm() + "' (ID=" + movieId + ") 장르: " + movie.getGenreNm());
            });
        }

        // 5. 점수화 및 추천 근거 태깅
        List<RecommendationDto> result = new ArrayList<>();
        for (MovieDetail movie : candidates) {
            int score = 0;
            List<String> reasons = new ArrayList<>();
            List<RecommendationDto.ReasonDetail> reasonDetails = new ArrayList<>();

            // 로그: 좋아요한 배우 ID
            System.out.println("[추천] userId=" + userId + ", 좋아요한 배우 actorIds=" + actorIds);
            // 로그: 영화 cast 정보
            System.out.println("[추천] 영화 '" + movie.getMovieNm() + "' cast 목록:");
            movie.getCasts().forEach(c -> System.out.println("  - actorId=" + c.getActor().getId() + ", 이름=" + c.getActor().getName() + ", 역할=" + c.getRoleType() + ", roleType.name()=" + c.getRoleType().name()));
            
            // 디버깅: 주연 배우만 필터링해서 확인
            List<Cast> leadCasts = movie.getCasts().stream()
                .filter(c -> RoleType.LEAD.equals(c.getRoleType()))
                .collect(Collectors.toList());
            System.out.println("[추천 디버깅] 영화 '" + movie.getMovieNm() + "' 주연 배우 목록:");
            leadCasts.forEach(c -> System.out.println("  - actorId=" + c.getActor().getId() + ", 이름=" + c.getActor().getName() + ", 역할=" + c.getRoleType()));

            // 감독 매치
            if (movie.getDirector() != null && directorIds.contains(movie.getDirector().getId())) {
                score += 5;
                reasons.add("좋아하는 감독");
                reasonDetails.add(RecommendationDto.ReasonDetail.builder().reason("좋아하는 감독").score(5).build());
            }
            // 배우 매치(주연만)
            int mainActorMatchCount = (int) movie.getCasts().stream()
                .filter(c -> RoleType.LEAD.equals(c.getRoleType()) && actorIds.contains(c.getActor().getId()))
                .count();
            // 로그: 매칭된 주연 배우 수
            System.out.println("[추천] 영화 '" + movie.getMovieNm() + "' 주연 배우 매칭 수: " + mainActorMatchCount);
            if (mainActorMatchCount == 1) { score += 3; reasons.add("좋아하는 배우"); reasonDetails.add(RecommendationDto.ReasonDetail.builder().reason("좋아하는 배우").score(3).build()); }
            else if (mainActorMatchCount == 2) { score += 6; reasons.add("좋아하는 배우"); reasonDetails.add(RecommendationDto.ReasonDetail.builder().reason("좋아하는 배우").score(6).build()); }
            else if (mainActorMatchCount >= 3) { score += 9; reasons.add("좋아하는 배우"); reasonDetails.add(RecommendationDto.ReasonDetail.builder().reason("좋아하는 배우").score(9).build()); }

            // 선호태그 매치
            if (!preferredTags.isEmpty() && movie.getTags() != null) {
                int tagMatchCount = (int) movie.getTags().stream()
                    .filter(movieTag -> preferredTags.stream().anyMatch(prefTag -> prefTag.getName().equals(movieTag.getName())))
                    .count();
                System.out.println("[추천] 영화 '" + movie.getMovieNm() + "' 선호태그 매칭 수: " + tagMatchCount);
                if (tagMatchCount == 1) { score += 4; reasons.add("선호 장르"); reasonDetails.add(RecommendationDto.ReasonDetail.builder().reason("선호 장르").score(4).build()); }
                else if (tagMatchCount == 2) { score += 8; reasons.add("선호 장르"); reasonDetails.add(RecommendationDto.ReasonDetail.builder().reason("선호 장르").score(8).build()); }
                else if (tagMatchCount >= 3) { score += 12; reasons.add("선호 장르"); reasonDetails.add(RecommendationDto.ReasonDetail.builder().reason("선호 장르").score(12).build()); }
            }

            // 평점/찜한 영화와 연관된 영화 점수 계산
            // 감독 동일
            if (movie.getDirector() != null && relatedDirectorIds.contains(movie.getDirector().getId())) {
                score += 5;
                reasons.add("관련 감독");
                reasonDetails.add(RecommendationDto.ReasonDetail.builder().reason("관련 감독").score(5).build());
            }
            
            // 배우 동일 (주연만)
            int relatedActorMatchCount = (int) movie.getCasts().stream()
                .filter(c -> RoleType.LEAD.equals(c.getRoleType()) && relatedActorIds.contains(c.getActor().getId()))
                .count();
            System.out.println("[추천] 영화 '" + movie.getMovieNm() + "' 관련 배우 매칭 수: " + relatedActorMatchCount);
            if (relatedActorMatchCount == 1) { score += 2; reasons.add("관련 배우"); reasonDetails.add(RecommendationDto.ReasonDetail.builder().reason("관련 배우").score(2).build()); }
            else if (relatedActorMatchCount == 2) { score += 4; reasons.add("관련 배우"); reasonDetails.add(RecommendationDto.ReasonDetail.builder().reason("관련 배우").score(4).build()); }
            else if (relatedActorMatchCount >= 3) { score += 6; reasons.add("관련 배우"); reasonDetails.add(RecommendationDto.ReasonDetail.builder().reason("관련 배우").score(6).build()); }
            
            // 장르 동일
            if (movie.getGenreNm() != null && !relatedGenres.isEmpty()) {
                int relatedGenreMatchCount = (int) Arrays.stream(movie.getGenreNm().split(","))
                    .map(String::trim)
                    .filter(relatedGenres::contains)
                    .count();
                System.out.println("[추천] 영화 '" + movie.getMovieNm() + "' 관련 장르 매칭 수: " + relatedGenreMatchCount);
                if (relatedGenreMatchCount == 1) { score += 2; reasons.add("관련 장르"); reasonDetails.add(RecommendationDto.ReasonDetail.builder().reason("관련 장르").score(2).build()); }
                else if (relatedGenreMatchCount == 2) { score += 4; reasons.add("관련 장르"); reasonDetails.add(RecommendationDto.ReasonDetail.builder().reason("관련 장르").score(4).build()); }
                else if (relatedGenreMatchCount >= 3) { score += 6; reasons.add("관련 장르"); reasonDetails.add(RecommendationDto.ReasonDetail.builder().reason("관련 장르").score(6).build()); }
            }

            // 최신성
            if (movie.getOpenDt() != null) {
                int years = Period.between(movie.getOpenDt(), LocalDate.now()).getYears();
                if (years <= 2) { score += 2; reasons.add("최신작"); reasonDetails.add(RecommendationDto.ReasonDetail.builder().reason("최신작").score(2).build()); }
                else if (years <= 5) { score += 1; reasons.add("최신작"); reasonDetails.add(RecommendationDto.ReasonDetail.builder().reason("최신작").score(1).build()); }
            }
            // 평점
            if (movie.getAverageRating() != null) {
                if (movie.getAverageRating() >= 4.0) { score += 2; reasons.add("평점 우수"); reasonDetails.add(RecommendationDto.ReasonDetail.builder().reason("평점 우수").score(2).build()); }
                else if (movie.getAverageRating() >= 3.0) { score += 1; reasons.add("평점 우수"); reasonDetails.add(RecommendationDto.ReasonDetail.builder().reason("평점 우수").score(1).build()); }
            }
            // 로그: 최종 점수 및 reasons
            System.out.println("[추천] 영화 '" + movie.getMovieNm() + "' 최종 점수: " + score + ", reasons=" + reasons);

            // 상세내역 로그 추가
            System.out.println("[추천 상세내역] 영화: " + movie.getMovieNm());
            for (RecommendationDto.ReasonDetail detail : reasonDetails) {
                System.out.println("  - " + detail.getReason() + " : " + (detail.getScore() > 0 ? "+" : "") + detail.getScore());
            }
            System.out.println("  => 최종 점수: " + score);

            if (score >= 3) {
                // MovieList에서 포스터 URL 가져오기 (LAZY 로딩 문제 해결)
                String posterUrl = null;
                try {
                    var movieList = movieListRepository.findById(movie.getMovieCd()).orElse(null);
                    if (movieList != null && movieList.getPosterUrl() != null) {
                        posterUrl = movieList.getPosterUrl();
                    } else if (movie.getStillcuts() != null && !movie.getStillcuts().isEmpty()) {
                        posterUrl = movie.getStillcuts().get(0).getImageUrl();
                    }
                } catch (Exception e) {
                    // 에러 발생 시 스틸컷으로 fallback
                    if (movie.getStillcuts() != null && !movie.getStillcuts().isEmpty()) {
                        posterUrl = movie.getStillcuts().get(0).getImageUrl();
                    }
                }
                
                result.add(RecommendationDto.builder()
                    .movieId(movie.getId())
                    .movieCd(movie.getMovieCd())
                    .movieNm(movie.getMovieNm())
                    .posterUrl(posterUrl)
                    .genreNm(movie.getGenreNm())
                    .averageRating(movie.getAverageRating())
                    .score(score)
                    .reasons(reasons)
                    .reasonDetails(reasonDetails)
                    .build());
            }
        }
        // 6. 점수 내림차순 정렬
        result.sort(Comparator.comparing(RecommendationDto::getScore).reversed());
        // 7. Pagination 처리
        int fromIndex = Math.min(page * size, result.size());
        int toIndex = Math.min(fromIndex + size, result.size());
        List<RecommendationDto> pageResult = new ArrayList<>(result.subList(fromIndex, toIndex));

        // 추천 로그 저장
        for (RecommendationDto dto : pageResult) {
            RecommendationLog log = RecommendationLog.builder()
                    .userId(userId)
                    .movieId(dto.getMovieId())
                    .score(dto.getScore())
                    .reasons(String.join(",", dto.getReasons()))
                    .build();
            recommendationLogRepository.save(log);
        }
        return pageResult;
    }

    /**
     * 사용자의 추천 캐시를 무효화합니다.
     * 사용자가 평점을 주거나 찜을 할 때 호출됩니다.
     */
    @CacheEvict(value = "recommendations", allEntries = true)
    public void evictUserRecommendations(Long userId) {
        System.out.println("[캐시 무효화] userId=" + userId + "의 추천 캐시를 무효화합니다.");
        // 캐시 무효화만 수행
    }

    /**
     * 모든 추천 캐시를 무효화합니다. (디버깅용)
     */
    @CacheEvict(value = "recommendations", allEntries = true)
    public void evictAllRecommendations() {
        System.out.println("[캐시 무효화] 모든 추천 캐시를 무효화합니다.");
    }
} 
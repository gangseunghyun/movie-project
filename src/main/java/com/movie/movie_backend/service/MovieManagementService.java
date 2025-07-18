package com.movie.movie_backend.service;

import com.movie.movie_backend.dto.MovieDetailDto;
import com.movie.movie_backend.entity.*;
import com.movie.movie_backend.mapper.MovieDetailMapper;
import com.movie.movie_backend.repository.*;
import com.movie.movie_backend.repository.CastRepository;
import com.movie.movie_backend.constant.MovieStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MovieManagementService {

    private final PRDMovieRepository movieRepository;
    private final PRDMovieListRepository movieListRepository;
    private final PRDActorRepository actorRepository;
    private final PRDDirectorRepository directorRepository;
    private final PRDTagRepository tagRepository;
    private final REVLikeRepository likeRepository;
    private final REVRatingRepository ratingRepository;
    private final REVReviewRepository reviewRepository;
    private final MovieDetailMapper movieDetailMapper;
    private final USRUserRepository userRepository;
    private final FileUploadService fileUploadService;
    private final CastRepository castRepository;

    /**
     * 영화 등록
     */
    @Transactional
    public MovieDetail createMovie(MovieDetailDto movieDto) {
        log.info("영화 등록 시작: {}", movieDto.getMovieNm());
        
        // 데이터 검증
        validateMovieData(movieDto);
        
        // movieCd가 없으면 자동 생성 (현재 시간 기반)
        String movieCd = movieDto.getMovieCd();
        if (movieCd == null || movieCd.trim().isEmpty()) {
            movieCd = "M" + System.currentTimeMillis();
            log.info("자동 생성된 movieCd: {}", movieCd);
        }
        
        // MovieList가 이미 존재하는지 확인
        MovieList movieList = movieListRepository.findByMovieCd(movieCd).orElse(null);
        
        if (movieList == null) {
            // MovieList가 없으면 새로 생성
            movieList = MovieList.builder()
                    .movieCd(movieCd)
                    .movieNm(movieDto.getMovieNm())
                    .movieNmEn(movieDto.getMovieNmEn())
                    .openDt(movieDto.getOpenDt())
                    .genreNm(movieDto.getGenreNm())
                    .nationNm(movieDto.getNationNm())
                    .watchGradeNm(movieDto.getWatchGradeNm())
                    .status(MovieStatus.COMING_SOON)
                    .build();
            
            movieList = movieListRepository.save(movieList);
            log.info("새로운 MovieList 생성 완료: {}", movieList.getMovieCd());
        } else {
            log.info("기존 MovieList 사용: {}", movieList.getMovieCd());
        }
        
        // MovieDetail이 이미 존재하는지 확인
        if (movieRepository.existsByMovieCd(movieCd)) {
            throw new RuntimeException("이미 존재하는 영화입니다: " + movieCd);
        }
        
        // MovieDetail 엔티티 생성
        MovieDetail movieDetail = new MovieDetail();
        
        movieDetail.setMovieCd(movieCd);
        movieDetail.setMovieNm(movieDto.getMovieNm());
        movieDetail.setMovieNmEn(movieDto.getMovieNmEn());
        movieDetail.setShowTm(movieDto.getShowTm());
        movieDetail.setOpenDt(movieDto.getOpenDt());
        movieDetail.setGenreNm(movieDto.getGenreNm());
        movieDetail.setNationNm(movieDto.getNationNm());
        movieDetail.setCompanyNm(movieDto.getCompanyNm());
        movieDetail.setDescription(movieDto.getDescription());
        movieDetail.setPrdtYear(movieDto.getPrdtYear());
        movieDetail.setPrdtStatNm(movieDto.getPrdtStatNm());
        movieDetail.setTypeNm(movieDto.getTypeNm());
        movieDetail.setWatchGradeNm(movieDto.getWatchGradeNm());
        
        // 기본값 설정
        movieDetail.setReservationRank(0);
        movieDetail.setReservationRate(0.0);
        movieDetail.setDaysSinceRelease(0);
        movieDetail.setTotalAudience(movieDto.getTotalAudience());
        movieDetail.setAverageRating(movieDto.getAverageRating());
        
        log.info("영화 엔티티 생성 완료: movieCd={}, movieNm={}", 
                movieCd, movieDto.getMovieNm());
        
        // 감독 정보 저장
        if (movieDto.getDirectors() != null && !movieDto.getDirectors().isEmpty()) {
            MovieDetailDto.Director directorDto = movieDto.getDirectors().get(0);
            Director director = new Director();
            director.setName(directorDto.getPeopleNm());
            director = directorRepository.save(director);
            movieDetail.setDirector(director);
        } else if (movieDto.getDirectorName() != null && !movieDto.getDirectorName().trim().isEmpty()) {
            // 문자열로 전송된 감독명 처리
            Director director = new Director();
            director.setName(movieDto.getDirectorName().trim());
            director = directorRepository.save(director);
            movieDetail.setDirector(director);
            log.info("감독 정보 저장: {}", director.getName());
        }
        
        // 태그 정보 저장
        if (movieDto.getTags() != null && !movieDto.getTags().isEmpty()) {
            for (Tag tag : movieDto.getTags()) {
                // 태그가 이미 존재하는지 확인
                Optional<Tag> existingTag = tagRepository.findByName(tag.getName());
                if (existingTag.isPresent()) {
                    movieDetail.getTags().add(existingTag.get());
                } else {
                    // 새 태그 생성
                    Tag newTag = new Tag();
                    newTag.setName(tag.getName());
                    Tag savedTag = tagRepository.save(newTag);
                    movieDetail.getTags().add(savedTag);
                }
            }
        }
        
        // 먼저 MovieDetail을 저장
        MovieDetail savedMovie = movieRepository.save(movieDetail);
        
        // 배우 정보 저장 (MovieDetail이 저장된 후)
        if (movieDto.getActors() != null && !movieDto.getActors().isEmpty()) {
            for (int i = 0; i < movieDto.getActors().size(); i++) {
                MovieDetailDto.Actor actorDto = movieDto.getActors().get(i);
                Actor actor = new Actor();
                actor.setName(actorDto.getPeopleNm());
                actor = actorRepository.save(actor);
                
                Cast cast = new Cast();
                cast.setMovieDetail(savedMovie);
                cast.setActor(actor);
                cast.setCharacterName(actorDto.getCast());
                cast.setOrderInCredits(i + 1);
                castRepository.save(cast);
                
                log.info("배우 정보 저장: {} (순서: {})", actor.getName(), i + 1);
            }
        } else if (movieDto.getActorNames() != null && !movieDto.getActorNames().trim().isEmpty()) {
            // 문자열로 전송된 배우명 처리
            String[] actorNames = movieDto.getActorNames().split(",");
            for (int i = 0; i < actorNames.length; i++) {
                String actorName = actorNames[i].trim();
                if (!actorName.isEmpty()) {
                    Actor actor = new Actor();
                    actor.setName(actorName);
                    actor = actorRepository.save(actor);
                    
                    Cast cast = new Cast();
                    cast.setMovieDetail(savedMovie);
                    cast.setActor(actor);
                    cast.setCharacterName(actorName);
                    cast.setOrderInCredits(i + 1);
                    castRepository.save(cast);
                    
                    log.info("배우 정보 저장: {} (순서: {})", actor.getName(), i + 1);
                }
            }
        }
        log.info("영화 등록 완료: {} (ID: {})", savedMovie.getMovieNm(), savedMovie.getMovieCd());
        
        // Cast 정보 저장 확인
        List<Cast> savedCasts = castRepository.findByMovieDetailMovieCdOrderByOrderInCreditsAsc(savedMovie.getMovieCd());
        log.info("저장된 Cast 개수: {}", savedCasts.size());
        for (Cast cast : savedCasts) {
            log.info("Cast 정보: 배우={}, 순서={}, 캐릭터={}", 
                cast.getActor().getName(), cast.getOrderInCredits(), cast.getCharacterName());
        }
        
        // DB 저장 확인을 위한 추가 로깅
        log.info("=== 영화 등록 상세 정보 ===");
        log.info("저장된 영화 코드: {}", savedMovie.getMovieCd());
        log.info("저장된 영화명: {}", savedMovie.getMovieNm());
        log.info("저장된 영문명: {}", savedMovie.getMovieNmEn());
        log.info("저장된 장르: {}", savedMovie.getGenreNm());
        log.info("저장된 국가: {}", savedMovie.getNationNm());
        log.info("저장된 상영시간: {}", savedMovie.getShowTm());
        log.info("저장된 개봉일: {}", savedMovie.getOpenDt());
        log.info("저장된 감독: {}", savedMovie.getDirector() != null ? savedMovie.getDirector().getName() : "없음");
        log.info("저장된 태그: {}", savedMovie.getTags().stream().map(Tag::getName).collect(java.util.stream.Collectors.joining(", ")));
        log.info("=== 영화 등록 완료 ===");
        
        return savedMovie;
    }

    /**
     * 영화 수정
     */
    @Transactional
    public MovieDetail updateMovie(String movieCd, MovieDetailDto movieDto) {
        log.info("영화 수정 시작: {}", movieCd);
        
        MovieDetail existingMovie = movieRepository.findByMovieCd(movieCd)
                .orElseThrow(() -> new RuntimeException("영화를 찾을 수 없습니다: " + movieCd));
        
        // 기본 정보 업데이트
        existingMovie.setMovieNm(movieDto.getMovieNm());
        existingMovie.setMovieNmEn(movieDto.getMovieNmEn());
        existingMovie.setShowTm(movieDto.getShowTm());
        existingMovie.setOpenDt(movieDto.getOpenDt());
        existingMovie.setGenreNm(movieDto.getGenreNm());
        existingMovie.setNationNm(movieDto.getNationNm());
        existingMovie.setCompanyNm(movieDto.getCompanyNm());
        existingMovie.setDescription(movieDto.getDescription());
        existingMovie.setPrdtYear(movieDto.getPrdtYear());
        existingMovie.setPrdtStatNm(movieDto.getPrdtStatNm());
        existingMovie.setTypeNm(movieDto.getTypeNm());
        existingMovie.setWatchGradeNm(movieDto.getWatchGradeNm());
        
        MovieDetail updatedMovie = movieRepository.save(existingMovie);
        log.info("영화 수정 완료: {} (ID: {})", updatedMovie.getMovieNm(), updatedMovie.getMovieCd());
        
        return updatedMovie;
    }

    /**
     * 영화 삭제
     */
    @Transactional
    public void deleteMovie(String movieCd) {
        log.info("영화 삭제 시작: {}", movieCd);
        
        // MovieDetail 조회
        MovieDetail movie = movieRepository.findByMovieCd(movieCd)
                .orElseThrow(() -> new RuntimeException("영화를 찾을 수 없습니다: " + movieCd));
        
        // MovieList 조회 및 포스터 파일 삭제
        MovieList movieList = movieListRepository.findById(movieCd).orElse(null);
        if (movieList != null && movieList.getPosterUrl() != null) {
            fileUploadService.deleteImage(movieList.getPosterUrl(), "posters");
        }
        
        // 관련 데이터 삭제 (찜, 평점, 댓글 등)
        List<Like> likes = likeRepository.findAll().stream()
                .filter(like -> like.getMovieDetail().getMovieCd().equals(movieCd))
                .toList();
        likeRepository.deleteAll(likes);
        
        // 평점 데이터 삭제
        List<Rating> ratings = ratingRepository.findAll().stream()
                .filter(rating -> rating.getMovieDetail().getMovieCd().equals(movieCd))
                .toList();
        ratingRepository.deleteAll(ratings);
        
        // 리뷰 데이터 삭제
        List<Review> reviews = reviewRepository.findAll().stream()
                .filter(review -> review.getMovieDetail().getMovieCd().equals(movieCd))
                .toList();
        reviewRepository.deleteAll(reviews);
        
        // Cast 데이터 삭제
        List<Cast> casts = castRepository.findByMovieDetailMovieCdOrderByOrderInCreditsAsc(movieCd);
        castRepository.deleteAll(casts);
        
        // MovieDetail 삭제
        movieRepository.delete(movie);
        
        // MovieList 삭제
        if (movieList != null) {
            movieListRepository.delete(movieList);
        }
        
        log.info("영화 삭제 완료: {}", movieCd);
    }

    /**
     * 영화 찜
     */
    @Transactional
    public void likeMovie(String movieCd, Long userId) {
        log.info("영화 찜: {} - 사용자: {}", movieCd, userId);
        
        MovieDetail movie = movieRepository.findByMovieCd(movieCd)
                .orElseThrow(() -> new RuntimeException("영화를 찾을 수 없습니다: " + movieCd));
        
        // 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + userId));
        
        // 이미 찜을 눌렀는지 확인 (null 체크 추가)
        List<Like> existingLikes = likeRepository.findAll().stream()
                .filter(like -> like.getMovieDetail().getMovieCd().equals(movieCd) && 
                               like.getUser() != null && like.getUser().getId().equals(userId))
                .toList();
        
        if (!existingLikes.isEmpty()) {
            log.info("이미 찜을 눌렀습니다: {} - 사용자: {}", movieCd, userId);
            return;
        }
        
        // 찜 추가
        Like like = new Like();
        like.setMovieDetail(movie);
        like.setUser(user);
        like.setCreatedAt(LocalDateTime.now());
        
        likeRepository.save(like);
        log.info("찜 추가 완료: {} - 사용자: {}", movieCd, userId);
        likeRepository.flush();
    }

    /**
     * 영화 찜 취소
     */
    @Transactional
    public void unlikeMovie(String movieCd, Long userId) {
        log.info("영화 찜 취소: {} - 사용자: {}", movieCd, userId);
        
        MovieDetail movie = movieRepository.findByMovieCd(movieCd)
                .orElseThrow(() -> new RuntimeException("영화를 찾을 수 없습니다: " + movieCd));
        
        // 찜 찾기 (null 체크 추가)
        List<Like> existingLikes = likeRepository.findAll().stream()
                .filter(like -> like.getMovieDetail().getMovieCd().equals(movieCd) && 
                               like.getUser() != null && like.getUser().getId().equals(userId))
                .toList();
        
        if (existingLikes.isEmpty()) {
            log.info("찜을 누르지 않았습니다: {} - 사용자: {}", movieCd, userId);
            return;
        }
        
        // 찜 삭제
        likeRepository.deleteAll(existingLikes);
        log.info("찜 취소 완료: {} - 사용자: {}", movieCd, userId);
        likeRepository.flush();
    }

    /**
     * 영화 상세 정보 조회
     */
    public MovieDetailDto getMovieDetail(String movieCd, User currentUser) {
        log.info("영화 상세 정보 조회: {}", movieCd);
        MovieDetail movie = movieRepository.findByMovieCd(movieCd)
                .orElseThrow(() -> new RuntimeException("영화를 찾을 수 없습니다: " + movieCd));
        int likeCount = likeRepository.countByMovieDetail(movie);
        boolean likedByMe = currentUser != null && likeRepository.existsByMovieDetailAndUser(movie, currentUser);
        return movieDetailMapper.toDto(movie, likeCount, likedByMe);
    }

    public List<MovieDetailDto> getMovieDetailDtos(User currentUser) {
        List<MovieDetail> movies = movieRepository.findAll();
        return movies.stream()
            .map(movie -> {
                int likeCount = likeRepository.countByMovieDetail(movie);
                boolean likedByMe = currentUser != null && likeRepository.existsByMovieDetailAndUser(movie, currentUser);
                return movieDetailMapper.toDto(movie, likeCount, likedByMe);
            })
            .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 영화 데이터 검증
     */
    private void validateMovieData(MovieDetailDto movieDto) {
        List<String> errors = new ArrayList<>();
        
        // 필수 필드 검증
        if (movieDto.getMovieNm() == null || movieDto.getMovieNm().trim().isEmpty()) {
            errors.add("영화 제목(한글)은 필수입니다.");
        }
        
        // 숫자 필드 검증
        if (movieDto.getShowTm() < 0) {
            errors.add("상영시간은 0 이상이어야 합니다.");
        }
        
        if (movieDto.getTotalAudience() < 0) {
            errors.add("누적 관객수는 0 이상이어야 합니다.");
        }
        
        if (movieDto.getReservationRate() < 0 || movieDto.getReservationRate() > 100) {
            errors.add("예매율은 0~100 사이여야 합니다.");
        }
        
        if (movieDto.getAverageRating() < 0 || movieDto.getAverageRating() > 10) {
            errors.add("평균 평점은 0~10 사이여야 합니다.");
        }
        
        // 날짜 형식 검증
        if (movieDto.getOpenDt() != null) {
            try {
                // LocalDate는 이미 검증됨
                if (movieDto.getOpenDt().isAfter(java.time.LocalDate.now().plusYears(10))) {
                    errors.add("개봉일은 현재로부터 10년 이내여야 합니다.");
                }
            } catch (Exception e) {
                errors.add("개봉일 형식이 올바르지 않습니다. (YYYY-MM-DD)");
            }
        }
        
        // 제작연도 검증
        if (movieDto.getPrdtYear() != null && !movieDto.getPrdtYear().trim().isEmpty()) {
            try {
                int year = Integer.parseInt(movieDto.getPrdtYear());
                if (year < 1900 || year > java.time.LocalDate.now().getYear() + 10) {
                    errors.add("제작연도는 1900년부터 현재로부터 10년 이내여야 합니다.");
                }
            } catch (NumberFormatException e) {
                errors.add("제작연도는 숫자여야 합니다.");
            }
        }
        
        // 문자열 길이 검증
        if (movieDto.getMovieNm() != null && movieDto.getMovieNm().length() > 200) {
            errors.add("영화 제목(한글)은 200자 이하여야 합니다.");
        }
        
        if (movieDto.getMovieNmEn() != null && movieDto.getMovieNmEn().length() > 200) {
            errors.add("영화 제목(영문)은 200자 이하여야 합니다.");
        }
        
        if (movieDto.getDescription() != null && movieDto.getDescription().length() > 4000) {
            errors.add("영화 설명은 4000자 이하여야 합니다.");
        }
        
        if (movieDto.getGenreNm() != null && movieDto.getGenreNm().length() > 100) {
            errors.add("장르는 100자 이하여야 합니다.");
        }
        
        if (movieDto.getNationNm() != null && movieDto.getNationNm().length() > 50) {
            errors.add("제작국가는 50자 이하여야 합니다.");
        }
        
        if (movieDto.getCompanyNm() != null && movieDto.getCompanyNm().length() > 100) {
            errors.add("배급사는 100자 이하여야 합니다.");
        }
        
        // 에러가 있으면 예외 발생
        if (!errors.isEmpty()) {
            String errorMessage = "데이터 검증 실패:\n" + String.join("\n", errors);
            log.error("영화 등록 데이터 검증 실패: {}", errorMessage);
            throw new IllegalArgumentException(errorMessage);
        }
    }
} 

package com.movie.movie_backend.service;

import com.movie.movie_backend.dto.MovieDetailDto;
import com.movie.movie_backend.entity.*;
import com.movie.movie_backend.mapper.MovieDetailMapper;
import com.movie.movie_backend.repository.*;
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
    private final MovieDetailMapper movieDetailMapper;

    /**
     * 영화 등록
     */
    @Transactional
    public MovieDetail createMovie(MovieDetailDto movieDto) {
        log.info("영화 등록 시작: {}", movieDto.getMovieNm());
        
        // 데이터 검증
        validateMovieData(movieDto);
        
        // MovieDetail 엔티티 생성
        MovieDetail movieDetail = new MovieDetail();
        
        // movieCd가 없으면 자동 생성 (현재 시간 기반)
        String movieCd = movieDto.getMovieCd();
        if (movieCd == null || movieCd.trim().isEmpty()) {
            movieCd = "M" + System.currentTimeMillis();
            log.info("자동 생성된 movieCd: {}", movieCd);
        }
        
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
        movieDetail.setStatus(MovieStatus.COMING_SOON); // 상영예정으로 기본 설정
        movieDetail.setReservationRank(0);
        movieDetail.setReservationRate(0.0);
        movieDetail.setDaysSinceRelease(0);
        movieDetail.setTotalAudience(movieDto.getTotalAudience());
        movieDetail.setAverageRating(movieDto.getAverageRating());
        
        log.info("영화 엔티티 생성 완료: movieCd={}, movieNm={}, status={}", 
                movieCd, movieDto.getMovieNm(), movieDetail.getStatus());
        
        // 감독 정보 저장
        if (movieDto.getDirectors() != null && !movieDto.getDirectors().isEmpty()) {
            MovieDetailDto.Director directorDto = movieDto.getDirectors().get(0);
            Director director = new Director();
            director.setName(directorDto.getPeopleNm());
            director = directorRepository.save(director);
            movieDetail.setDirector(director);
        }
        
        // 배우 정보 저장
        if (movieDto.getActors() != null && !movieDto.getActors().isEmpty()) {
            for (MovieDetailDto.Actor actorDto : movieDto.getActors()) {
                Actor actor = new Actor();
                actor.setName(actorDto.getPeopleNm());
                actor = actorRepository.save(actor);
                
                Cast cast = new Cast();
                cast.setMovieDetail(movieDetail);
                cast.setActor(actor);
                cast.setCharacterName(actorDto.getCast());
            }
        }
        
        MovieDetail savedMovie = movieRepository.save(movieDetail);
        log.info("영화 등록 완료: {} (ID: {})", savedMovie.getMovieNm(), savedMovie.getMovieCd());
        
        // DB 저장 확인을 위한 추가 로깅
        log.info("=== 영화 등록 상세 정보 ===");
        log.info("저장된 영화 코드: {}", savedMovie.getMovieCd());
        log.info("저장된 영화명: {}", savedMovie.getMovieNm());
        log.info("저장된 영문명: {}", savedMovie.getMovieNmEn());
        log.info("저장된 장르: {}", savedMovie.getGenreNm());
        log.info("저장된 국가: {}", savedMovie.getNationNm());
        log.info("저장된 상태: {}", savedMovie.getStatus());
        log.info("저장된 상영시간: {}", savedMovie.getShowTm());
        log.info("저장된 개봉일: {}", savedMovie.getOpenDt());
        log.info("저장된 감독: {}", savedMovie.getDirector() != null ? savedMovie.getDirector().getName() : "없음");
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
        
        MovieDetail movie = movieRepository.findByMovieCd(movieCd)
                .orElseThrow(() -> new RuntimeException("영화를 찾을 수 없습니다: " + movieCd));
        
        // 관련 데이터 삭제 (좋아요, 댓글 등)
        List<Like> likes = likeRepository.findAll().stream()
                .filter(like -> like.getMovieDetail().getMovieCd().equals(movieCd))
                .toList();
        likeRepository.deleteAll(likes);
        
        // 영화 삭제
        movieRepository.delete(movie);
        log.info("영화 삭제 완료: {}", movieCd);
    }

    /**
     * 영화 좋아요
     */
    @Transactional
    public void likeMovie(String movieCd, Long userId) {
        log.info("영화 좋아요: {} - 사용자: {}", movieCd, userId);
        
        MovieDetail movie = movieRepository.findByMovieCd(movieCd)
                .orElseThrow(() -> new RuntimeException("영화를 찾을 수 없습니다: " + movieCd));
        
        // 이미 좋아요를 눌렀는지 확인
        List<Like> existingLikes = likeRepository.findAll().stream()
                .filter(like -> like.getMovieDetail().getMovieCd().equals(movieCd) && 
                               like.getUser().getId().equals(userId))
                .toList();
        
        if (!existingLikes.isEmpty()) {
            log.info("이미 좋아요를 눌렀습니다: {} - 사용자: {}", movieCd, userId);
            return;
        }
        
        // 좋아요 추가
        Like like = new Like();
        like.setMovieDetail(movie);
        like.setCreatedAt(LocalDateTime.now());
        
        likeRepository.save(like);
        log.info("좋아요 추가 완료: {} - 사용자: {}", movieCd, userId);
    }

    /**
     * 영화 상세 정보 조회
     */
    public MovieDetailDto getMovieDetail(String movieCd) {
        log.info("영화 상세 정보 조회: {}", movieCd);
        
        MovieDetail movie = movieRepository.findByMovieCd(movieCd)
                .orElseThrow(() -> new RuntimeException("영화를 찾을 수 없습니다: " + movieCd));
        
        return movieDetailMapper.toDto(movie);
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

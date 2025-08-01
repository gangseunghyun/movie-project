package com.movie.movie_backend.mapper;

import com.movie.movie_backend.entity.MovieDetail;
import com.movie.movie_backend.entity.MovieList;
import com.movie.movie_backend.dto.MovieDetailDto;
import com.movie.movie_backend.repository.PRDMovieListRepository;
import com.movie.movie_backend.repository.BoxOfficeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class MovieDetailMapper {

    private final PRDMovieListRepository movieListRepository;
    private final BoxOfficeRepository boxOfficeRepository;
    private final ObjectMapper objectMapper;

    public MovieDetailDto toDto(MovieDetail movieDetail, int likeCount, boolean likedByMe) {
        MovieDetailDto dto = new MovieDetailDto();
        dto.setMovieDetailId(movieDetail.getId());
        dto.setMovieCd(movieDetail.getMovieCd());
        dto.setMovieNm(movieDetail.getMovieNm());
        dto.setMovieNmEn(movieDetail.getMovieNmEn());
        dto.setPrdtYear(movieDetail.getPrdtYear());
        dto.setShowTm(movieDetail.getShowTm());
        dto.setOpenDt(movieDetail.getOpenDt());
        dto.setPrdtStatNm(movieDetail.getPrdtStatNm());
        dto.setTypeNm(movieDetail.getTypeNm());
        dto.setGenreNm(movieDetail.getGenreNm());
        dto.setNationNm(movieDetail.getNationNm());
        dto.setWatchGradeNm(movieDetail.getWatchGradeNm());
        dto.setCompanyNm(movieDetail.getCompanyNm());
        dto.setDescription(movieDetail.getDescription());
        dto.setReservationRank(getReservationRank(movieDetail.getMovieCd()));
        dto.setReservationRate(calculateReservationRate(getReservationRank(movieDetail.getMovieCd())));
        dto.setDaysSinceRelease(calculateDaysSinceRelease(movieDetail.getOpenDt()));
        dto.setTotalAudience(getTotalAudience(movieDetail.getMovieCd()));
        
        // MovieList에서 포스터 URL과 TMDB ID 가져오기
        String posterUrl = null;
        String directorName = null;
        String status = null;
        Integer tmdbId = null;
        Double tmdbPopularity = null;
        
        try {
            MovieList movieList = movieListRepository.findById(movieDetail.getMovieCd()).orElse(null);
            if (movieList != null) {
                posterUrl = movieList.getPosterUrl();
                status = movieList.getStatus() != null ? movieList.getStatus().name() : null;
                tmdbId = movieList.getTmdbId();
                tmdbPopularity = movieList.getTmdbPopularity();
            }
            
            if (movieDetail.getDirector() != null) {
                directorName = movieDetail.getDirector().getName();
            }
        } catch (Exception e) {
            // 로그는 남기되 에러는 발생시키지 않음
        }
        
        // 포스터 URL이 null이거나 "null" 문자열이면 빈 문자열로 설정
        if (posterUrl == null || "null".equals(posterUrl) || posterUrl.trim().isEmpty()) {
            posterUrl = "";
        }
        
        dto.setPosterUrl(posterUrl);
        dto.setDirectorName(directorName);
        dto.setAverageRating(movieDetail.getAverageRating() != null ? movieDetail.getAverageRating() : 0.0);
        dto.setStatus(status);
        dto.setTmdbId(tmdbId);
        dto.setTmdbPopularity(tmdbPopularity);
        
        // 감독 정보 매핑
        if (movieDetail.getDirector() != null) {
            List<MovieDetailDto.Director> directors = List.of(
                MovieDetailDto.Director.builder()
                    .id(movieDetail.getDirector().getId())
                    .peopleNm(movieDetail.getDirector().getName())
                    .peopleNmEn(movieDetail.getDirector().getName()) // 영문명이 없으면 한글명 사용
                    .photoUrl(movieDetail.getDirector().getPhotoUrl())
                    .roleType("감독")
                    .build()
            );
            dto.setDirectors(directors);
        }
        
        // 배우 정보 매핑
        if (movieDetail.getCasts() != null && !movieDetail.getCasts().isEmpty()) {
            List<MovieDetailDto.Actor> actors = movieDetail.getCasts().stream()
                .map(cast -> MovieDetailDto.Actor.builder()
                    .id(cast.getActor().getId())
                    .peopleNm(cast.getActor().getName())
                    .peopleNmEn(cast.getActor().getName()) // 영문명이 없으면 한글명 사용
                    .cast(cast.getCharacterName())
                    .castEn(cast.getCharacterName()) // 영문 배역명이 없으면 한글 배역명 사용
                    .photoUrl(cast.getActor().getPhotoUrl())
                    .roleType(cast.getRoleType() != null ? cast.getRoleType().name() : "")
                    .build())
                .collect(Collectors.toList());
            dto.setActors(actors);
        }
        
        // 스틸컷 정보 매핑 (정렬된 순서로)
        if (movieDetail.getStillcuts() != null && !movieDetail.getStillcuts().isEmpty()) {
            List<MovieDetailDto.Stillcut> stillcutDtos = movieDetail.getStillcuts().stream()
                .sorted((s1, s2) -> Integer.compare(s1.getOrderInMovie(), s2.getOrderInMovie()))
                .map(stillcut -> MovieDetailDto.Stillcut.builder()
                    .id(stillcut.getId())
                    .imageUrl(stillcut.getImageUrl())
                    .orderInMovie(stillcut.getOrderInMovie())
                    .build())
                .collect(Collectors.toList());
            dto.setStillcuts(stillcutDtos);
        } else {
            dto.setStillcuts(null);
        }
        
        // 태그 정보 매핑
        dto.setTags(movieDetail.getTags() != null ? movieDetail.getTags() : List.of());
        
        dto.setLikeCount(likeCount);
        dto.setLikedByMe(likedByMe);

        // audits(JSON) → List<Audit> 매핑
        List<MovieDetailDto.Audit> auditsList = List.of();
        if (movieDetail.getAudits() != null && !movieDetail.getAudits().isBlank()) {
            try {
                auditsList = objectMapper.readValue(
                    movieDetail.getAudits(),
                    new TypeReference<List<MovieDetailDto.Audit>>() {}
                );
            } catch (Exception e) {
                // 변환 실패 시 빈 리스트 유지
            }
        }
        dto.setAudits(auditsList);
        return dto;
    }

    // public List<MovieDetailDto> toDtoList(List<MovieDetail> movieDetails) {
    //     return movieDetails.stream()
    //             .map(this::toDto)
    //             .collect(Collectors.toList());
    // }

    // ===== 왓챠피디아 스타일 정보 계산 메서드들 =====

    /**
     * 개봉일수 계산
     */
    private int calculateDaysSinceRelease(LocalDate openDt) {
        if (openDt == null) return 0;
        LocalDate today = LocalDate.now();
        return (int) ChronoUnit.DAYS.between(openDt, today);
    }

    /**
     * 박스오피스에서 예매 순위 가져오기
     */
    private int getReservationRank(String movieCd) {
        try {
            // 최신 박스오피스에서 해당 영화의 순위 찾기
            var latestBoxOffice = boxOfficeRepository.findLatestBoxOfficeTop10("DAILY");
            for (var boxOffice : latestBoxOffice) {
                if (boxOffice.getMovieCd().equals(movieCd)) {
                    return boxOffice.getRank();
                }
            }
        } catch (Exception e) {
            // 에러 발생 시 기본값 반환
        }
        return 0; // 박스오피스에 없으면 0
    }

    /**
     * 예매율 계산 (순위 기반 추정)
     */
    private double calculateReservationRate(int rank) {
        if (rank <= 0) return 0.0;
        if (rank <= 3) {
            return 25.0 - (rank - 1) * 4.0;
        } else if (rank <= 10) {
            return 12.0 - (rank - 4) * 1.2;
        } else {
            return 1.0;
        }
    }

    /**
     * 박스오피스에서 누적 관객수 가져오기
     */
    private int getTotalAudience(String movieCd) {
        try {
            // 최신 박스오피스에서 해당 영화의 누적 관객수 찾기
            var latestBoxOffice = boxOfficeRepository.findLatestBoxOfficeTop10("DAILY");
            for (var boxOffice : latestBoxOffice) {
                if (boxOffice.getMovieCd().equals(movieCd)) {
                    return (int) boxOffice.getAudiAcc();
                }
            }
        } catch (Exception e) {
            // 에러 발생 시 기본값 반환
        }
        return 0; // 박스오피스에 없으면 0
    }
} 

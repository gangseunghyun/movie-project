package com.movie.mapper;

import com.movie.entity.MovieDetail;
import com.movie.entity.MovieList;
import com.movie.dto.TopRatedMovieDto;
import com.movie.repository.PRDMovieListRepository;
import com.movie.repository.BoxOfficeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class TopRatedMovieMapper {

    private final PRDMovieListRepository movieListRepository;
    private final BoxOfficeRepository boxOfficeRepository;

    public TopRatedMovieDto toDto(MovieDetail movieDetail) {
        // MovieList에서 포스터 URL과 기본 정보 가져오기
        String posterUrl = null;
        String movieNmEn = null;
        String genreNm = null;
        String nationNm = null;
        String watchGradeNm = null;
        
        try {
            MovieList movieList = movieListRepository.findById(movieDetail.getMovieCd()).orElse(null);
            if (movieList != null) {
                posterUrl = movieList.getPosterUrl();
                movieNmEn = movieList.getMovieNmEn();
                genreNm = movieList.getGenreNm();
                nationNm = movieList.getNationNm();
                watchGradeNm = movieList.getWatchGradeNm();
            }
        } catch (Exception e) {
            // 로그는 남기되 에러는 발생시키지 않음
        }

        // 왓챠피디아 스타일 정보 계산
        int calculatedDaysSinceRelease = calculateDaysSinceRelease(movieDetail.getOpenDt());
        int reservationRank = getReservationRank(movieDetail.getMovieCd());
        double reservationRate = calculateReservationRate(reservationRank);
        int totalAudience = getTotalAudience(movieDetail.getMovieCd());

        return TopRatedMovieDto.builder()
                .movieCd(movieDetail.getMovieCd())
                .movieNm(movieDetail.getMovieNm())
                .movieNmEn(movieNmEn != null ? movieNmEn : movieDetail.getMovieNmEn())
                .prdtYear(movieDetail.getPrdtYear())
                .showTm(movieDetail.getShowTm())
                .openDt(movieDetail.getOpenDt())
                .genreNm(genreNm != null ? genreNm : movieDetail.getGenreNm())
                .nationNm(nationNm != null ? nationNm : movieDetail.getNationNm())
                .watchGradeNm(watchGradeNm != null ? watchGradeNm : movieDetail.getWatchGradeNm())
                .companyNm(movieDetail.getCompanyNm())
                .description(movieDetail.getDescription())
                .posterUrl(posterUrl)
                .directorName(movieDetail.getDirector() != null ? movieDetail.getDirector().getName() : null)
                .averageRating(movieDetail.getAverageRating() != null ? movieDetail.getAverageRating() : 0.0)
                .ratingCount(movieDetail.getRatingCount() != null ? movieDetail.getRatingCount() : 0)
                .reservationRank(reservationRank)
                .reservationRate(reservationRate)
                .totalAudience(totalAudience)
                .build();
    }

    public List<TopRatedMovieDto> toDtoList(List<MovieDetail> movieDetails) {
        return movieDetails.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

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
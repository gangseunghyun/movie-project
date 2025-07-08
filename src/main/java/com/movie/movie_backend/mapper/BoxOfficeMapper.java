package com.movie.movie_backend.mapper;

import com.movie.movie_backend.dto.BoxOfficeDto;
import com.movie.movie_backend.entity.BoxOffice;
import com.movie.movie_backend.entity.MovieDetail;
import com.movie.movie_backend.entity.Director;
import com.movie.movie_backend.entity.Tag;
import com.movie.movie_backend.constant.MovieStatus;
import com.movie.movie_backend.entity.MovieList;
import com.movie.movie_backend.repository.PRDMovieListRepository;
import com.movie.movie_backend.service.TmdbPosterService;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;
import java.text.NumberFormat;
import java.util.Locale;

@Component
public class BoxOfficeMapper {

    private final PRDMovieListRepository movieListRepository;
    private final TmdbPosterService tmdbPosterService;

    public BoxOfficeMapper(PRDMovieListRepository movieListRepository, TmdbPosterService tmdbPosterService) {
        this.movieListRepository = movieListRepository;
        this.tmdbPosterService = tmdbPosterService;
    }

    /**
     * BoxOffice 엔티티를 BoxOfficeDto로 변환 (왓챠피디아 스타일)
     */
    public BoxOfficeDto toDto(BoxOffice boxOffice) {
        if (boxOffice == null) return null;

        MovieDetail movieDetail = boxOffice.getMovieDetail();
        
        // 디버깅 로그 추가
        System.out.println("BoxOfficeMapper - BoxOffice ID: " + boxOffice.getId());
        System.out.println("BoxOfficeMapper - movieCd: " + boxOffice.getMovieCd());
        System.out.println("BoxOfficeMapper - movieDetail: " + (movieDetail != null ? movieDetail.getMovieCd() : "null"));
        
        // MovieList에서 포스터 URL 가져오기
        String posterUrl = null;
        if (boxOffice.getMovieCd() != null) {
            MovieList movieList = movieListRepository.findById(boxOffice.getMovieCd()).orElse(null);
            if (movieList != null) {
                posterUrl = movieList.getPosterUrl();
            }
        }

        // 평점 정보 가져오기 (캐시된 데이터 사용)
        Double averageRating = null;
        Integer ratingCount = null;
        if (movieDetail != null) {
            averageRating = movieDetail.getAverageRating();
            ratingCount = movieDetail.getRatingCount();
        }

        return BoxOfficeDto.builder()
                .id(boxOffice.getId())
                .movieCd(boxOffice.getMovieCd())
                .movieNm(boxOffice.getMovieNm())
                .rank(boxOffice.getRank())
                .salesAmt(boxOffice.getSalesAmt())
                .audiCnt(boxOffice.getAudiCnt())
                .audiAcc(boxOffice.getAudiAcc())
                .targetDate(boxOffice.getTargetDate())
                .rankType(boxOffice.getRankType())
                
                // 영화 상세 정보
                .movieNmEn(movieDetail != null ? movieDetail.getMovieNmEn() : "")
                .genreNm(movieDetail != null ? movieDetail.getGenreNm() : "")
                .nationNm(movieDetail != null ? movieDetail.getNationNm() : "")
                .watchGradeNm(movieDetail != null ? movieDetail.getWatchGradeNm() : "")
                .posterUrl(posterUrl)
                .description(movieDetail != null ? movieDetail.getDescription() : "")
                .showTm(movieDetail != null ? movieDetail.getShowTm() : 0)
                .openDt(movieDetail != null ? movieDetail.getOpenDt() : null)
                
                // 박스오피스 통계 정보 (왓챠피디아 스타일 포맷팅)
                .reservationRate(formatReservationRate(boxOffice.getRank()))
                .audienceCount(formatAudienceCount(boxOffice.getAudiCnt()))
                .salesAmount(formatSalesAmount(boxOffice.getSalesAmt()))
                .rankChange("-") // 순위 변동은 별도 계산 필요
                .rankTypeDisplay(getRankTypeDisplay(boxOffice.getRankType()))
                
                // 영화 상태 정보
                .movieStatus(getMovieStatusDisplay(movieDetail != null ? movieDetail.getStatus() : null))
                .daysSinceRelease(calculateDaysSinceRelease(movieDetail != null ? movieDetail.getOpenDt() : null))
                
                // 감독 정보
                .directorName(movieDetail != null && movieDetail.getDirector() != null ? 
                    movieDetail.getDirector().getName() : "")
                .directorPhotoUrl(movieDetail != null && movieDetail.getDirector() != null ? 
                    movieDetail.getDirector().getPhotoUrl() : "")
                
                // 태그 정보
                .tags(movieDetail != null && movieDetail.getTags() != null ? 
                    movieDetail.getTags().stream()
                        .map(Tag::getName)
                        .toArray(String[]::new) : new String[0])
                .averageRating(averageRating)
                .ratingCount(ratingCount)
                .formattedSalesAmt(formatSalesAmount(boxOffice.getSalesAmt()))
                .formattedAudiCnt(formatAudienceCount(boxOffice.getAudiCnt()))
                .formattedAudiAcc(formatAccumulatedAudience(boxOffice.getAudiAcc()))
                .companyNm(movieDetail != null ? movieDetail.getCompanyNm() : "")
                .build();
    }

    /**
     * BoxOffice 엔티티 리스트를 BoxOfficeDto 리스트로 변환
     */
    public List<BoxOfficeDto> toDtoList(List<BoxOffice> boxOfficeList) {
        if (boxOfficeList == null) return List.of();
        return boxOfficeList.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    // ===== 헬퍼 메서드 =====

    /**
     * 예매율 포맷팅 (왓챠피디아 스타일)
     * 박스오피스 순위를 기반으로 한 예매율 추정
     */
    private String formatReservationRate(int rank) {
        // 박스오피스 순위를 기반으로 한 예매율 추정
        if (rank <= 3) {
            return String.format("%.1f%%", 25.0 - (rank - 1) * 4.0);
        } else if (rank <= 10) {
            return String.format("%.1f%%", 12.0 - (rank - 4) * 1.2);
        } else {
            return "1.0%";
        }
    }

    /**
     * 관객수 포맷팅 (왓챠피디아 스타일)
     */
    private String formatAudienceCount(long audiCnt) {
        if (audiCnt >= 10000) {
            return String.format("%.1f만명", audiCnt / 10000.0);
        } else {
            return String.format("%,d명", audiCnt);
        }
    }

    /**
     * 누적 관객수 포맷팅 (왓챠피디아 스타일)
     */
    private String formatAccumulatedAudience(long audiAcc) {
        if (audiAcc >= 10000) {
            return String.format("%.1f만명", audiAcc / 10000.0);
        } else {
            return String.format("%,d명", audiAcc);
        }
    }

    /**
     * 매출액 포맷팅 (왓챠피디아 스타일)
     */
    private String formatSalesAmount(long salesAmt) {
        if (salesAmt >= 100000000) { // 1억 이상
            long billion = salesAmt / 100000000;
            long million = (salesAmt % 100000000) / 10000;
            if (million > 0) {
                return String.format("%d억 %,d만원", billion, million);
            } else {
                return String.format("%d억원", billion);
            }
        } else if (salesAmt >= 10000) {
            return String.format("%.1f만원", salesAmt / 10000.0);
        } else {
            return String.format("%,d원", salesAmt);
        }
    }

    /**
     * 순위 타입 표시
     */
    private String getRankTypeDisplay(String rankType) {
        return switch (rankType) {
            case "DAILY" -> "일일";
            case "WEEKLY" -> "주간";
            case "WEEKEND" -> "주말";
            default -> rankType;
        };
    }

    /**
     * 영화 상태 표시
     */
    private String getMovieStatusDisplay(MovieStatus status) {
        if (status == null) return "상영중";
        return switch (status) {
            case COMING_SOON -> "상영예정";
            case NOW_PLAYING -> "상영중";
            case ENDED -> "상영종료";
        };
    }

    /**
     * 개봉일수 계산
     */
    private int calculateDaysSinceRelease(LocalDate openDt) {
        if (openDt == null) return 0;
        LocalDate today = LocalDate.now();
        return (int) java.time.temporal.ChronoUnit.DAYS.between(openDt, today);
    }
} 

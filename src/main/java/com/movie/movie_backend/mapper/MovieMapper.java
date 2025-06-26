package com.movie.mapper;

import com.movie.dto.MovieDetailDto;
import com.movie.entity.MovieDetail;
import com.movie.entity.Director;
import com.movie.entity.Actor;
import com.movie.entity.Cast;
import com.movie.constant.MovieStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class MovieMapper {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * MovieDetailDto를 MovieDetail 엔티티로 변환
     */
    public MovieDetail toMovieDetailEntity(MovieDetailDto dto) {
        if (dto == null) return null;

        return MovieDetail.builder()
                .movieCd(dto.getMovieCd())
                .movieNm(dto.getMovieNm())
                .movieNmEn(dto.getMovieNmEn())
                .prdtYear(dto.getPrdtYear())
                .showTm(dto.getShowTm())
                .openDt(dto.getOpenDt())
                .prdtStatNm(dto.getPrdtStatNm())
                .typeNm(dto.getTypeNm())
                .genreNm(dto.getGenreNm())
                .nationNm(dto.getNationNm())
                .watchGradeNm(dto.getWatchGradeNm())
                .companyNm(dto.getCompanyNm())
                .description(dto.getDescription() != null ? dto.getDescription() : "")
                .status(determineMovieStatus(dto.getOpenDt()))
                .reservationRank(dto.getReservationRank())
                .reservationRate(dto.getReservationRate())
                .daysSinceRelease(dto.getDaysSinceRelease())
                .totalAudience(dto.getTotalAudience())
                .build();
    }

    /**
     * MovieDetail 엔티티를 MovieDetailDto로 변환
     */
    public MovieDetailDto toDto(MovieDetail entity) {
        if (entity == null) return null;

        return MovieDetailDto.builder()
                .movieCd(entity.getMovieCd())
                .movieNm(entity.getMovieNm())
                .movieNmEn(entity.getMovieNmEn())
                .prdtYear(entity.getPrdtYear())
                .showTm(entity.getShowTm())
                .openDt(entity.getOpenDt())
                .prdtStatNm(entity.getPrdtStatNm())
                .typeNm(entity.getTypeNm())
                .genreNm(entity.getGenreNm())
                .nationNm(entity.getNationNm())
                .watchGradeNm(entity.getWatchGradeNm())
                .companyNm(entity.getCompanyNm())
                .description(entity.getDescription())
                .status(entity.getStatus() != null ? entity.getStatus().name() : null)
                .reservationRank(entity.getReservationRank())
                .reservationRate(entity.getReservationRate())
                .daysSinceRelease(entity.getDaysSinceRelease())
                .totalAudience(entity.getTotalAudience())
                .posterUrl("") // MovieList에서 가져와야 함
                .directorName(entity.getDirector() != null ? entity.getDirector().getName() : null)
                .nations(createNationList(entity.getNationNm()))
                .genres(createGenreList(entity.getGenreNm()))
                .directors(createDirectorList(entity.getDirector()))
                .actors(createActorList(entity.getCasts()))
                .showTypes(List.of()) // Entity에 없으므로 빈 리스트
                .audits(createAuditList(entity.getWatchGradeNm()))
                .companys(createCompanyList(entity.getCompanyNm()))
                .staffs(List.of()) // Entity에 없으므로 빈 리스트
                .build();
    }

    // Helper methods
    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return null;
        try {
            return LocalDate.parse(dateStr, DATE_FORMATTER);
        } catch (Exception e) {
            return null;
        }
    }

    private String formatDate(LocalDate date) {
        if (date == null) return "";
        return date.format(DATE_FORMATTER);
    }

    private MovieStatus determineMovieStatus(LocalDate openDt) {
        if (openDt == null) return MovieStatus.COMING_SOON;
        
        LocalDate now = LocalDate.now();
        
        if (openDt.isAfter(now)) return MovieStatus.COMING_SOON;
        if (openDt.plusDays(30).isAfter(now)) return MovieStatus.NOW_PLAYING;
        return MovieStatus.ENDED;
    }

    private List<MovieDetailDto.Nation> createNationList(String nationNm) {
        if (nationNm == null || nationNm.isEmpty()) return List.of();
        return List.of(MovieDetailDto.Nation.builder().nationNm(nationNm).build());
    }

    private List<MovieDetailDto.Genre> createGenreList(String genreNm) {
        if (genreNm == null || genreNm.isEmpty()) return List.of();
        return List.of(MovieDetailDto.Genre.builder().genreNm(genreNm).build());
    }

    private List<MovieDetailDto.Director> createDirectorList(Director director) {
        if (director == null) return List.of();
        return List.of(MovieDetailDto.Director.builder()
                .peopleNm(director.getName())
                .peopleNmEn("") // Director 엔티티에 영문명 필드가 없으므로 빈 문자열
                .build());
    }

    private List<MovieDetailDto.Actor> createActorList(List<Cast> casts) {
        if (casts == null || casts.isEmpty()) return List.of();
        return casts.stream()
                .map(cast -> MovieDetailDto.Actor.builder()
                        .peopleNm(cast.getActor().getName())
                        .peopleNmEn("") // Actor 엔티티에 영문명 필드가 없으므로 빈 문자열
                        .cast((cast.getCharacterName() == null || cast.getCharacterName().isBlank()) ? null : cast.getCharacterName()) // characterName이 null 또는 빈 문자열이면 null 반환
                        .castEn("") // 영문 배역명 필드가 없으므로 빈 문자열
                        .build())
                .collect(Collectors.toList());
    }

    private List<MovieDetailDto.Audit> createAuditList(String watchGradeNm) {
        if (watchGradeNm == null || watchGradeNm.isEmpty()) return List.of();
        return List.of(MovieDetailDto.Audit.builder()
                .auditNo("")
                .watchGradeNm(watchGradeNm)
                .build());
    }

    private List<MovieDetailDto.Company> createCompanyList(String companyNm) {
        if (companyNm == null || companyNm.isEmpty()) return List.of();
        return List.of(MovieDetailDto.Company.builder()
                .companyCd("")
                .companyNm(companyNm)
                .companyNmEn("")
                .companyPartNm("")
                .build());
    }
} 
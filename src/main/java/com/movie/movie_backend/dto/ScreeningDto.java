package com.movie.movie_backend.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ScreeningDto {
    private Long id;
    private String startTime;
    private String endTime;
    private int price;
    private String status;
    private Long theaterId;
    private String theaterName;
    private Long movieDetailId;
    private String movieCd;
    private String movieNm;
    private String posterUrl;

    public static ScreeningDto fromEntity(com.movie.movie_backend.entity.Screening s) {
        String posterUrl = null;
        if (s.getMovieDetail() != null && s.getMovieDetail().getMovieList() != null) {
            posterUrl = s.getMovieDetail().getMovieList().getPosterUrl();
        }
        return ScreeningDto.builder()
            .id(s.getId())
            .startTime(s.getStartTime() != null ? s.getStartTime().toString() : null)
            .endTime(s.getEndTime() != null ? s.getEndTime().toString() : null)
            .price(s.getPrice() != null ? s.getPrice().intValue() : 0)
            .status(s.getStatus() != null ? s.getStatus().name() : null)
            .theaterId(s.getTheater() != null ? s.getTheater().getId() : null)
            .theaterName(s.getTheater() != null ? s.getTheater().getName() : null)
            .movieDetailId(s.getMovieDetail() != null ? s.getMovieDetail().getId() : null)
            .movieCd(s.getMovieDetail() != null ? s.getMovieDetail().getMovieCd() : null)
            .movieNm(s.getMovieDetail() != null ? s.getMovieDetail().getMovieNm() : null)
            .posterUrl(posterUrl)
            .build();
    }
} 
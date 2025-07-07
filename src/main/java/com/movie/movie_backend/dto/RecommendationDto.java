package com.movie.movie_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecommendationDto {
    private Long movieId;
    private String movieCd;
    private String movieNm;
    private String posterUrl;
    private String genreNm;
    private Double averageRating;
    private int score;
    private List<String> reasons;
    private List<ReasonDetail> reasonDetails;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ReasonDetail {
        private String reason;
        private int score;
    }
} 
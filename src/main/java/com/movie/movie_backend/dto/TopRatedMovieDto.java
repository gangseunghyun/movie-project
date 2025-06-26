package com.movie.movie_backend.dto;

import lombok.*;
import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TopRatedMovieDto {
    private String movieCd;           // 영화코드
    private String movieNm;           // 영화명 (국문)
    private String movieNmEn;         // 영화명 (영문)
    private String prdtYear;          // 제작연도
    private int showTm;               // 상영시간 (분)
    private LocalDate openDt;         // 개봉일
    private String genreNm;           // 장르명
    private String nationNm;          // 제작국가명
    private String watchGradeNm;      // 관람등급명
    private String companyNm;         // 영화사명
    private String description;       // 영화설명
    private String posterUrl;         // 포스터 URL (MovieList에서 가져옴)
    private String directorName;      // 감독이름
    private double averageRating;     // 평균 평점
    private int ratingCount;          // 평점 개수
    private int reservationRank;      // 예매순위
    private double reservationRate;   // 예매율
    private int totalAudience;        // 누적관객수
} 

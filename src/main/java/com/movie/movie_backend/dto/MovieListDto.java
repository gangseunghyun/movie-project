package com.movie.movie_backend.dto;

import lombok.*;
import java.time.LocalDate;
import com.movie.movie_backend.constant.MovieStatus;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MovieListDto {
    private String movieCd;           // 영화코드 (PK, 문자열)
    private String movieNm;           // 영화 제목 (한글)
    private String movieNmEn;         // 영화 원제 (영어)
    private LocalDate openDt;         // 개봉일
    private String genreNm;           // 장르
    private String nationNm;          // 제작국가
    private String watchGradeNm;
    public String getWatchGradeNm() {
        return watchGradeNm;
    }
    public void setWatchGradeNm(String watchGradeNm) {
        this.watchGradeNm = watchGradeNm;
    }
    private String posterUrl;         // 포스터 이미지 URL
    private MovieStatus status;       // 영화 상태
    private String kmdbId;              // KMDb 영화 ID (nullable)
} 

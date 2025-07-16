package com.movie.movie_backend.dto;

import com.movie.movie_backend.constant.MovieStatus;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminMovieDto {
    
    // 영화 기본 정보
    private String movieCd;
    private String movieNm;
    private String movieNmEn;
    private String prdtYear;
    private int showTm;
    private LocalDate openDt;
    private String prdtStatNm;
    private String typeNm;
    private String genreNm;
    private String nationNm;
    private String watchGradeNm;
    private String companyNm;
    private String description;
    private MovieStatus status;
    
    // 감독 정보
    private String directorName;
    
    // 배우 정보
    private String actorNames;
    
    // 태그 정보
    private List<String> tagNames;
    
    // 스틸컷 정보
    private List<String> stillcutUrls;
    
    // 관리용 정보
    private LocalDate createdAt;
    private LocalDate updatedAt;
} 

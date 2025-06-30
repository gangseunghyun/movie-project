package com.movie.movie_backend.entity;

import lombok.*;
import jakarta.persistence.*;
import java.time.LocalDate;
import com.movie.movie_backend.constant.MovieStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Table(name = "movie_list")
public class MovieList {
    @Id
    @Column(name = "movie_cd")
    private String movieCd; // 영화코드 (PK, 문자열)

    private String movieNm; // 영화 제목 (한글)
    private String movieNmEn; // 영화 원제 (영어)
    private LocalDate openDt; // 개봉일
    private String genreNm; // 장르
    private String nationNm; // 제작국가
    private String watchGradeNm; // 관람등급
    @Column(nullable = true)
    private String posterUrl; // 포스터 이미지 URL

    @Enumerated(EnumType.STRING)
    private MovieStatus status; // 영화 상태

    @OneToOne(mappedBy = "movieList", fetch = FetchType.LAZY)
    @JsonIgnore
    private MovieDetail movieDetail; // 영화 상세정보 (1:1)
} 

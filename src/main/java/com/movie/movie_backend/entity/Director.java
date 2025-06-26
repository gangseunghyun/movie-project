package com.movie.movie_backend.entity;

import lombok.*;
import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDate;
import java.util.List;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Director {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 감독 고유 ID

    private String name; // 감독 이름
    private LocalDate birthDate; // 감독 생년월일
    private String nationality; // 국적
    private String biography; // 감독 소개
    private String photoUrl; // 감독 사진 URL

    @OneToMany(mappedBy = "director")
    @JsonIgnoreProperties("director")
    private List<MovieDetail> movieDetails; // 감독한 영화 상세정보 목록 (1:N)
} 

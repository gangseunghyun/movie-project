package com.movie.movie_backend.entity;

import lombok.*;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.List;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Actor {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 배우 고유 ID

    private String name; // 배우 이름
    private LocalDate birthDate; // 배우 생년월일
    private String nationality; // 국적
    private String biography; // 배우 소개
    private String photoUrl; // 배우 사진 URL

    @OneToMany(mappedBy = "actor", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Cast> casts; // 출연한 영화 목록 (역할 정보 포함)
} 

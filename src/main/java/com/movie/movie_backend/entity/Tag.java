package com.movie.movie_backend.entity;

import lombok.*;
import jakarta.persistence.*;
import java.util.List;

@Entity
@Getter @Setter @NoArgsConstructor
public class Tag {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 태그 고유 ID

    private String name; // 태그명

    @ManyToMany(mappedBy = "tags")
    private List<MovieDetail> movieDetails; // 태그가 달린 영화 상세정보 목록 (N:M)
} 

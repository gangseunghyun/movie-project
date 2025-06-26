package com.movie.entity;

import lombok.*;
import jakarta.persistence.*;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Stillcut {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 스틸컷 고유 ID

    private String imageUrl; // 스틸컷 이미지 URL
    private int orderInMovie; // 영화 내 스틸컷 순서(선택)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_detail_id")
    private MovieDetail movieDetail; // 소속 영화 상세정보
} 
package com.movie.movie_backend.entity;

import lombok.*;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Getter @Setter @NoArgsConstructor
public class Rating {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 평점 고유 ID

    private int score; // 평점 점수 (예: 1~5)
    private LocalDateTime createdAt; // 평점 작성 시간

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user; // 평점을 남긴 사용자

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_detail_id")
    private MovieDetail movieDetail; // 평점이 달린 영화 상세정보
} 

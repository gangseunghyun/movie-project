package com.movie.movie_backend.entity;

import lombok.*;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Getter @Setter @NoArgsConstructor
@Table(name = "likes")
public class Like {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 찜 고유 ID

    private LocalDateTime createdAt; // 찜 누른 시간

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user; // 찜을 누른 사용자

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_detail_id")
    private MovieDetail movieDetail; // 찜이 달린 영화 상세정보
} 

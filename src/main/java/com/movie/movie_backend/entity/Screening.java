package com.movie.entity;

import lombok.*;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;
import java.math.BigDecimal;
import com.movie.constant.ScreeningStatus;

@Entity
@Getter @Setter @NoArgsConstructor
@Table(name = "screenings")
public class Screening {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 상영 고유 ID

    private LocalDateTime startTime; // 상영 시작 시간
    private LocalDateTime endTime; // 상영 종료 시간
    private BigDecimal price; // 상영 가격

    @Enumerated(EnumType.STRING)
    private ScreeningStatus status; // 상영 상태 (예매가능, 예매마감, 상영완료 등)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_detail_id")
    private MovieDetail movieDetail; // 상영할 영화 상세정보

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "theater_id")
    private Theater theater; // 상영할 상영관

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cinema_id")
    private Cinema cinema; // 상영할 영화관

    @OneToMany(mappedBy = "screening")
    private List<Reservation> reservations; // 이 상영의 예매 목록
} 
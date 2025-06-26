package com.movie.movie_backend.entity;

import lombok.*;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;
import java.math.BigDecimal;
import com.movie.movie_backend.constant.ReservationStatus;

@Entity
@Getter @Setter @NoArgsConstructor
public class Reservation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 예매 고유 ID

    private LocalDateTime reservedAt; // 예매 시간
    private BigDecimal totalAmount; // 총 결제 금액

    @Enumerated(EnumType.STRING)
    private ReservationStatus status; // 예매 상태 (예매완료, 취소됨, 사용완료 등)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user; // 예매한 사용자

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "screening_id")
    private Screening screening; // 예매한 상영

    @ManyToMany
    @JoinTable(name = "reservation_seat",
        joinColumns = @JoinColumn(name = "reservation_id"),
        inverseJoinColumns = @JoinColumn(name = "seat_id"))
    private List<Seat> seats; // 예매한 좌석 목록

    @OneToMany(mappedBy = "reservation")
    private List<Payment> payments; // 이 예매의 결제 목록
} 

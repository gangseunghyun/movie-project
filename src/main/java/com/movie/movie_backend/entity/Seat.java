package com.movie.entity;

import lombok.*;
import jakarta.persistence.*;
import com.movie.constant.SeatType;

@Entity
@Getter @Setter @NoArgsConstructor
@Table(name = "seats")
public class Seat {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 좌석 고유 ID

    private String seatNumber; // 좌석 번호 (예: A1, A2, B1, B2...)
    
    @Column(name = "seat_row")
    private int rowNumber; // 행 번호
    
    @Column(name = "seat_column")
    private int columnNumber; // 열 번호

    @Enumerated(EnumType.STRING)
    private SeatType seatType; // 좌석 타입 (일반, 커플, 장애인, VIP 등)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "theater_id")
    private Theater theater; // 소속 상영관
} 
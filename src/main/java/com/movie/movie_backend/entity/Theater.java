package com.movie.movie_backend.entity;

import lombok.*;
import jakarta.persistence.*;
import java.util.List;

@Entity
@Getter @Setter @NoArgsConstructor
@Table(name = "theaters")
public class Theater {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 상영관 고유 ID

    private String name; // 상영관 이름 (예: 1관, 2관, IMAX관 등)
    private int totalSeats; // 총 좌석 수

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cinema_id")
    private Cinema cinema; // 소속 영화관

    @OneToMany(mappedBy = "theater", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Seat> seats; // 상영관 내 좌석 목록

    @OneToMany(mappedBy = "theater", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Screening> screenings; // 이 상영관의 상영 정보 목록
} 

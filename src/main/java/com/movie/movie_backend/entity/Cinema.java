package com.movie.entity;

import lombok.*;
import jakarta.persistence.*;
import java.util.List;

@Entity
@Getter @Setter @NoArgsConstructor
@Table(name = "cinemas")
public class Cinema {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 영화관 고유 ID

    private String name; // 영화관 이름
    private String address; // 영화관 주소
    private String phoneNumber; // 영화관 전화번호

    @OneToMany(mappedBy = "cinema", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Theater> theaters; // 영화관 내 상영관 목록

    @OneToMany(mappedBy = "cinema")
    private List<Screening> screenings; // 이 영화관의 상영 정보 목록
} 
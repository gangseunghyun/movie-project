package com.movie.movie_backend.entity;

import lombok.*;
import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDate;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Table(name = "box_office")
public class BoxOffice {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String movieCd;           // 영화 코드
    private String movieNm;           // 영화명
    @Column(name = "`rank`")
    private int rank;                 // 순위
    private long salesAmt;            // 매출액
    private long audiCnt;             // 관객수
    private long audiAcc;             // 누적 관객수
    private LocalDate targetDate;     // 기준일
    private String rankType;          // 순위 타입 (일일/주간/주말)
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_detail_id")
    @JsonIgnoreProperties({"casts", "tags", "ratings", "comments", "likes", "screenings", "stillcuts"})
    private MovieDetail movieDetail;  // 영화 상세정보
} 

package com.movie.movie_backend.entity;

import lombok.*;
import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import com.movie.movie_backend.constant.MovieStatus;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Table(name = "movie_detail")
public class MovieDetail {
    @Id
    private String movieCd; // 영화 코드 (KOBIS API의 movieCd)

    private String movieNm; // 영화명
    private String movieNmEn; // 영화 영문명
    private String prdtYear; // 제작년도
    private int showTm; // 상영 시간 (분)
    private LocalDate openDt; // 개봉일
    private String prdtStatNm; // 제작상태
    private String typeNm; // 영화유형
    private String genreNm; // 장르
    private String nationNm; // 제작국가
    private String watchGradeNm; // 관람등급
    private String companyNm; // 배급사
    
    @Column(columnDefinition = "TEXT")
    private String description; // 영화 설명 (줄거리)
    
    @Enumerated(EnumType.STRING)
    private MovieStatus status; // 영화 상태 (상영중, 상영예정, 상영종료)
    
    // 예매 순위 관련
    private int reservationRank; // 예매 순위
    private double reservationRate; // 예매율 (%)
    private int daysSinceRelease; // 개봉일수
    private int totalAudience; // 누적 관객수

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "director_id", nullable = true)
    @JsonIgnore
    private Director director; // 감독 (N:1)

    @OneToMany(mappedBy = "movieDetail", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Cast> casts; // 출연 배우 목록 (역할 정보 포함)

    @ManyToMany
    @JoinTable(name = "movie_detail_tag",
            joinColumns = @JoinColumn(name = "movie_detail_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"))
    @JsonIgnore
    private List<Tag> tags; // 영화 태그 목록 (N:M)

    @OneToMany(mappedBy = "movieDetail")
    @JsonIgnore
    private List<Rating> ratings; // 영화에 달린 평점 목록

    @OneToMany(mappedBy = "movieDetail")
    @JsonIgnore
    private List<Review> reviews; // 영화에 달린 리뷰 목록

    @OneToMany(mappedBy = "movieDetail")
    @JsonIgnore
    private List<Like> likes; // 영화에 달린 좋아요 목록

    @OneToMany(mappedBy = "movieDetail")
    @JsonIgnore
    private List<Screening> screenings; // 영화의 상영 정보 목록

    @OneToMany(mappedBy = "movieDetail", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Stillcut> stillcuts; // 영화의 스틸컷 이미지 목록

    // 평점 캐시 필드 (성능 최적화용)
    private Double averageRating;     // 평균 평점
    private Integer ratingCount;      // 평점 개수
    private LocalDateTime ratingUpdatedAt; // 평점 업데이트 시간
}

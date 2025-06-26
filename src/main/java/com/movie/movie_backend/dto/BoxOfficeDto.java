package com.movie.movie_backend.dto;

import lombok.*;
import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BoxOfficeDto {
    
    // 박스오피스 기본 정보
    private Long id;
    private String movieCd;
    private String movieNm;
    private int rank;
    private long salesAmt;
    private long audiCnt;
    private long audiAcc;
    private LocalDate targetDate;
    private String rankType;
    
    // 영화 상세 정보 (왓챠피디아 스타일)
    private String movieNmEn;
    private String genreNm;
    private String nationNm;
    private String watchGradeNm;
    private String posterUrl;
    private String description;
    private int showTm;
    private LocalDate openDt;
    
    // 박스오피스 통계 정보 (왓챠피디아 스타일)
    private String reservationRate; // 예매율 (예: "38%", "8.1%")
    private String audienceCount; // 관객수 포맷팅 (예: "1,828명", "130.8만명")
    private String salesAmount; // 매출액 포맷팅 (예: "1억 2,000만원")
    private String rankChange; // 순위 변동 (예: "▲1", "▼2", "-")
    private String rankTypeDisplay; // 순위 타입 표시 (예: "일일", "주간", "주말")
    
    // 영화 상태 정보
    private String movieStatus; // "상영중", "상영예정", "상영종료"
    private int daysSinceRelease; // 개봉일수
    
    // 감독 정보
    private String directorName;
    private String directorPhotoUrl;
    
    // 태그 정보
    private String[] tags;
    
    // 추가된 필드
    private String formattedSalesAmt;
    private String formattedAudiCnt;
    private String formattedAudiAcc;
    private String companyNm;
    private Double averageRating;
    private Integer ratingCount;
} 

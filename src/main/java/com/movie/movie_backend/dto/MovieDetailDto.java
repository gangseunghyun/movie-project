package com.movie.movie_backend.dto;

import lombok.*;
import java.util.List;
import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MovieDetailDto {
    private String id;                // 영화 ID (movieCd와 동일)
    private String movieCd;           // 영화코드 (PK, 문자열)
    private String movieNm;           // 영화명 (국문)
    private String movieNmEn;         // 영화명 (영문)
    private String prdtYear;          // 제작연도
    private int showTm;               // 상영시간 (분)
    private LocalDate openDt;          // 개봉일
    private String prdtStatNm;        // 제작상태명
    private String typeNm;            // 영화유형명
    private String genreNm;           // 장르명
    private String nationNm;          // 제작국가명
    private String watchGradeNm;      // 관람등급명
    private String companyNm;         // 영화사명
    private String description;       // 영화설명
    private String status;            // 상태
    private int reservationRank;      // 예매순위
    private double reservationRate;   // 예매율
    private int daysSinceRelease;     // 개봉후일수
    private int totalAudience;        // 누적관객수
    private String posterUrl;         // 포스터 URL
    private String directorName;      // 감독이름
    private double averageRating;     // 평균 평점
    private List<Nation> nations;     // 제작국가 목록
    private List<Genre> genres;       // 장르 목록
    private List<Director> directors; // 감독 목록
    private List<Actor> actors;       // 배우 목록
    private List<ShowType> showTypes; // 상영형태 목록
    private List<Audit> audits;       // 심의정보 목록
    private List<Company> companys;   // 참여 영화사 목록
    private List<Staff> staffs;       // 스텝 목록
    private List<Stillcut> stillcuts; // 스틸컷 목록
    private int likeCount;      // 좋아요 수
    private boolean likedByMe;  // 내가 좋아요를 눌렀는지

    // 내부 DTO들 (필요하면 더 세분화 가능)

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Nation {
        private String nationNm;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Genre {
        private String genreNm;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Director {
        private Long id;
        private String peopleNm;
        private String peopleNmEn;
        private String photoUrl; // 감독 사진
        private String roleType; // '감독'
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Actor {
        private Long id;
        private String peopleNm;
        private String peopleNmEn;
        private String cast;    // 배역명
        private String castEn;  // 배역명(영문)
        private String photoUrl; // 배우 사진
        private String roleType; // 주연/조연/특별출연 등
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ShowType {
        private String showTypeGroupNm;
        private String showTypeNm;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Audit {
        private String auditNo;
        private String watchGradeNm;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Company {
        private String companyCd;
        private String companyNm;
        private String companyNmEn;
        private String companyPartNm;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Staff {
        private String peopleNm;
        private String peopleNmEn;
        private String staffRoleNm;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Stillcut {
        private Long id;
        private String imageUrl;
        private int orderInMovie;
    }
}

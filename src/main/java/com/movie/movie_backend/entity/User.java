package com.movie.movie_backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "temp_user") // 나중에 DB 관리자가 테이블명을 정해주면 이 부분만 바꾸면 됩니다.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // DB가 알아서 ID를 1씩 증가시킵니다.
    private Long id;

    @Column(name = "temp_login_id", nullable = false, unique = true) // nullable=false: 비어있으면 안됨, unique=true: 중복되면 안됨
    private String loginId;

    @Column(name = "temp_password", nullable = true) // 소셜 로그인을 위해 nullable=true로 변경
    private String password;

    @Column(name = "temp_email", nullable = false, unique = true)
    private String email;

    @Column(name = "temp_role")
    private String role; // 예: "USER", "ADMIN"

    private String provider; // 예: "local", "google", "kakao"
    private String providerId; // 소셜 로그인 공급자가 제공하는 고유 ID
} 
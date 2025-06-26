package com.movie.movie_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "password_reset_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordResetToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private LocalDateTime expiryDate;

    @Column(nullable = false)
    private boolean used = false;

    // 생성 시 만료시간 기본값 30분 후로 설정
    public static PasswordResetToken create(String token, String email, int minutes) {
        return PasswordResetToken.builder()
                .token(token)
                .email(email)
                .expiryDate(LocalDateTime.now().plusMinutes(minutes))
                .used(false)
                .build();
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiryDate);
    }
} 

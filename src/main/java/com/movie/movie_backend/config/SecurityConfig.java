package com.movie.movie_backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration // 이 클래스가 Spring의 설정 파일임을 나타냅니다.
public class SecurityConfig {

    @Bean // 이 메소드가 반환하는 객체(PasswordEncoder)를 Spring 컨테이너의 Bean으로 등록합니다.
    public PasswordEncoder passwordEncoder() {
        // BCrypt 알고리즘을 사용하는 PasswordEncoder를 반환합니다.
        // BCrypt는 비밀번호 해싱에 가장 널리 사용되는 안전한 방법 중 하나입니다.
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // CSRF(Cross-Site Request Forgery) 보호 기능을 비활성화합니다.
                // API 서버로 개발할 때는 보통 비활성화하며, 나중에 프론트엔드와 연동 시 다른 방식으로 보안을 처리합니다.
                .csrf(csrf -> csrf.disable())

                // HTTP 요청에 대한 접근 권한을 설정합니다.
                .authorizeHttpRequests(authz -> authz
                        // "/join.html", "/api/users/join" 등 지정된 경로는 인증 없이 누구나 접근을 허용합니다.
                        .requestMatchers(
                                "/join.html",
                                "/api/users/join",
                                "/api/users/check-login-id",
                                "/api/mail/send-code",
                                "/api/mail/verify-code"
                        ).permitAll()
                        // 그 외의 모든 요청은 반드시 인증된 사용자만 접근할 수 있습니다.
                        .anyRequest().authenticated()
                );

        return http.build();
    }
} 
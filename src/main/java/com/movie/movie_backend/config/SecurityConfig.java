package com.movie.movie_backend.config;

import com.movie.movie_backend.service.UserDetailServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserDetailServiceImpl userDetailService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        AuthenticationManagerBuilder authBuilder = http.getSharedObject(AuthenticationManagerBuilder.class);
        authBuilder.userDetailsService(userDetailService).passwordEncoder(passwordEncoder());
        AuthenticationManager authenticationManager = authBuilder.build();

        http
            .csrf(csrf -> csrf.disable())
            .authenticationManager(authenticationManager)
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/", "/join", "/login", "/css/**", "/js/**", "/images/**").permitAll()
                .requestMatchers("/api/users/**").permitAll() // /api/users 경로 전체 허용으로 간소화
                .requestMatchers("/api/mail/**").permitAll()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login") // 로그인 처리 URL 명시
                .defaultSuccessUrl("/", true) // 로그인 성공 시 항상 홈으로 리디렉션
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout") // 로그아웃 URL 명시
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            );

        return http.build();
    }
} 
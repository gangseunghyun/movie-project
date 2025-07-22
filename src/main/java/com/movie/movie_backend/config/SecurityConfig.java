package com.movie.movie_backend.config;

import com.movie.movie_backend.service.USRUserDetailServiceImpl;
import com.movie.movie_backend.service.CustomOAuth2UserService;
import com.movie.movie_backend.handler.CustomAuthenticationSuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${app.frontend.url:http://filmer-movie.duckdns.org}")
    private String frontendUrl;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CsrfTokenRepository csrfTokenRepository() {
        CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repository.setCookiePath("/");
        return repository;
    }

    @Bean
    public AuthenticationFailureHandler customAuthenticationFailureHandler() {
        return new SimpleUrlAuthenticationFailureHandler() {
            @Override
            public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, org.springframework.security.core.AuthenticationException exception) throws IOException, ServletException {
                String errorMessage = exception.getMessage();
                String provider = null;
                if (errorMessage != null && errorMessage.startsWith("PROVIDER:")) {
                    int sep = errorMessage.indexOf('|');
                    if (sep > 0) {
                        provider = errorMessage.substring(9, sep);
                        errorMessage = errorMessage.substring(sep + 1);
                    }
                }
                errorMessage = java.net.URLEncoder.encode(errorMessage, "UTF-8");
                if (provider == null || provider.equals("local") || provider.isBlank()) {
                    getRedirectStrategy().sendRedirect(request, response, frontendUrl + "/login?error=true&message=" + errorMessage);
                } else {
                    getRedirectStrategy().sendRedirect(request, response, frontendUrl + "/login?error=true&message=" + errorMessage + "&social=" + provider);
                }
            }
        };
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(
            "http://localhost:3000",
            "http://localhost",
            "http://13.222.249.145",
            "https://13.222.249.145",
            "http://filmer-movie.duckdns.org",
            "https://filmer-movie.duckdns.org"
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setExposedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With", "Accept", "Origin", "Access-Control-Request-Method", "Access-Control-Request-Headers"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler() {
        return new CustomAuthenticationSuccessHandler();
    }

    @Bean
    public AuthenticationManager authenticationManager(
        HttpSecurity http,
        USRUserDetailServiceImpl userDetailService
    ) throws Exception {
        AuthenticationManagerBuilder authBuilder = http.getSharedObject(AuthenticationManagerBuilder.class);
        authBuilder.userDetailsService(userDetailService).passwordEncoder(passwordEncoder());
        return authBuilder.build();
    }

    @Bean
    public SecurityFilterChain filterChain(
        HttpSecurity http,
        USRUserDetailServiceImpl userDetailService,
        CustomOAuth2UserService customOAuth2UserService
    ) throws Exception {
        AuthenticationManagerBuilder authBuilder = http.getSharedObject(AuthenticationManagerBuilder.class);
        authBuilder.userDetailsService(userDetailService).passwordEncoder(passwordEncoder());
        AuthenticationManager authenticationManager = authBuilder.build();

        http
            .csrf(csrf -> csrf.disable())
            .headers(headers -> headers
                .frameOptions().disable()
                .xssProtection().disable()
                .contentTypeOptions().disable()
            )
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            .sessionManagement(session -> session
                .sessionCreationPolicy(org.springframework.security.config.http.SessionCreationPolicy.ALWAYS)
                .maximumSessions(1)
                .maxSessionsPreventsLogin(false)
                .and()
                .sessionFixation().migrateSession()
                .invalidSessionUrl("http://localhost:3000/login")
            )
            .authenticationManager(authenticationManager)
            .authorizeHttpRequests()
                .requestMatchers(
                    "/oauth2/authorization/**",
                    "/login/oauth2/code/**",
                    "/api/box-office-dto",
                    "/api/box-office",
                    "/api/movie-detail-dto",
                    "/api/movie-detail",
                    "/api/movie-list-dto",
                    "/api/movie-list",
                    "/api/ratings/top-rated",
                    "/api/movies/coming-soon",
                    "/api/movies/now-playing",
                    "/api/ratings/movie/*/average",
                    "/api/ratings/movie/*/distribution",
                    "/api/genre-tags",
                    "/api/tmdb-popularity",
                    "/api/movies/filter",
                    "/api/person/actor/*",
                    "/api/person/director/*",
                    "/api/person/recommended-actor",
                    "/api/person/recommended-director",
                    "/api/person/refresh-recommended-actor",
                    "/api/person/refresh-recommended-director",
                    "/api/person/actor/*/like-status",
                    "/api/person/director/*/like-status",
                    "/api/reviews/movie/*",
                    "/api/reviews/movie/*/content-only",
                    "/api/reviews/*/liked-users",
                    "/api/comments/review/*",
                    "/api/comments/review/*/all",
                    "/api/comments/review/*/flat",
                    "/api/search",
                    "/api/popular-keywords/**",
                    "/api/forbidden-words/filter",
                    "/api/forbidden-words/check",
                    "/api/profile/images/**",
                    "/uploads/**",
                    "/data/**",
                    "/api/mcp/tools/**",
                    "/swagger-ui/**",
                    "/api-docs/**",
                    "/v3/api-docs/**",
                    // 기존 회원가입/로그인/이메일 등 공개 API도 유지
                    "/api/login",
                    "/api/current-user",
                    "/api/logout",
                    "/api/users/join",
                    "/api/users/check-login-id",
                    "/api/users/check-email",
                    "/api/users/check-nickname",
                    "/api/users/recommend-nickname",
                    "/api/social-join-complete",
                    "/api/social-password-join",
                    "/api/mail/send-verification",
                    "/api/mail/verify-code",
                    "/api/mail/**",
                    "/api/find-id",
                    "/api/forgot-password",
                    "/api/reset-password/validate-token",
                    "/api/reset-password",
                    "/api/search-history/popular"
                ).permitAll()
                .requestMatchers("/api/user-login").permitAll()
                .requestMatchers("/api/user-ratings/movie/*/average").permitAll()
                .requestMatchers("/api/user-ratings/movie/*/distribution").permitAll()
                .requestMatchers("/api/ratings/movie/*/distribution").permitAll()
                .requestMatchers("/api/ratings/movie/*/average").permitAll()
                .requestMatchers("/api/movies/*/like").authenticated()
                .requestMatchers("/api/person/*/like").authenticated()
                .requestMatchers("/api/person/*/like-status").permitAll()
                .requestMatchers("/api/movies/**").hasRole("ADMIN")  // 나머지 영화 관리 기능은 ADMIN만
                .requestMatchers("/api/reviews/movie/*").permitAll()  // 리뷰 목록 조회는 누구나
                .requestMatchers("/api/reviews/movie/*/content-only").permitAll()  // 댓글만 조회도 누구나
                .requestMatchers("/api/reviews/*/liked-users").permitAll()  // 좋아요한 유저 목록 조회는 누구나
                .requestMatchers("/api/reviews/**").authenticated()  // 나머지 리뷰 관련 기능은 인증 필요
                .requestMatchers("/api/tmdb/**").permitAll()  // TMDB 매핑 API는 모든 사용자 접근 가능
                .requestMatchers("/api/comments/review/*").permitAll()  // 댓글 조회는 누구나
                .requestMatchers("/api/comments/review/*/all").permitAll()  // 전체 댓글 조회는 누구나
                .requestMatchers("/api/comments/review/*/flat").permitAll()  // 평탄화 댓글 조회는 누구나
                .requestMatchers("/api/comments/**").authenticated()  // 나머지 댓글 관련 기능은 인증 필요
                .requestMatchers("/api/search-history").authenticated()
                .requestMatchers("/api/users/*/liked-movies").permitAll()
                .requestMatchers("/api/users/*/liked-actors").permitAll()
                .requestMatchers("/api/users/*/liked-directors").permitAll()
                .requestMatchers("/api/movie-detail-dto/search").permitAll()  // 영화 검색은 누구나 접근 가능
                .requestMatchers("/api/search-person").permitAll()  // 인물 검색은 누구나 접근 가능
                .requestMatchers("/api/users/search").permitAll()  // 사용자 검색은 누구나 접근 가능
                .anyRequest().authenticated()
            .and()
            .formLogin().disable()
            .httpBasic().disable()
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login.html")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .oauth2Login(oauth2 -> oauth2
                .successHandler(customAuthenticationSuccessHandler())
                .failureHandler(customAuthenticationFailureHandler())
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(customOAuth2UserService)
                )
            )
            .exceptionHandling(exceptionHandling -> exceptionHandling
                .authenticationEntryPoint((request, response, authException) -> {
                    // API 요청에 대해서는 JSON 응답 반환
                    if (request.getRequestURI().startsWith("/api/")) {
                        response.setContentType("application/json;charset=UTF-8");
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.getWriter().write("{\"success\":false,\"message\":\"로그인이 필요합니다.\"}");
                    } else {
                        // 일반 페이지 요청은 로그인 페이지로 리다이렉트
                        response.sendRedirect(frontendUrl + "/login");
                    }
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    // API 요청에 대해서는 JSON 응답 반환
                    if (request.getRequestURI().startsWith("/api/")) {
                        response.setContentType("application/json;charset=UTF-8");
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        response.getWriter().write("{\"success\":false,\"message\":\"접근 권한이 없습니다.\"}");
                    } else {
                        // 일반 페이지 요청은 에러 페이지로 리다이렉트
                        response.sendRedirect(frontendUrl + "/error");
                    }
                })
            );

        return http.build();
    }
} 

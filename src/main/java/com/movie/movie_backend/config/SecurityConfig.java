package com.movie.movie_backend.config;

import com.movie.movie_backend.service.USRUserDetailServiceImpl;
import com.movie.movie_backend.service.CustomOAuth2UserService;
import com.movie.movie_backend.handler.CustomAuthenticationSuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
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
                    getRedirectStrategy().sendRedirect(request, response, "http://localhost:3000/login?error=true&message=" + errorMessage);
                } else {
                    getRedirectStrategy().sendRedirect(request, response, "http://localhost:3000/login?error=true&message=" + errorMessage + "&social=" + provider);
                }
            }
        };
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:3000"));
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
                    "/api/search-history/popular",
                    "/api/popular-keywords/**",
                    "/reset-password",
                    "/static/**",
                    "/resources/static/**",
                    "/data/**",
                    "/api/users/search",
                    "/swagger-ui/**",
                    "/api-docs/**",
                    "/v3/api-docs/**",
                    "/api/person/actor/*",
                    "/api/person/recommended-actor",
                    "/api/person/refresh-recommended-actor",
                    "/api/person/director/*",
                    "/api/person/recommended-director",
                    "/api/person/refresh-recommended-director",
                    "/api/person/actor/*/like-status",
                    "/api/person/director/*/like-status",
                    "/api/users/*/follow",
                    "/api/users/*/unfollow",
                    "/api/users/*/followers",
                    "/api/users/*/following",
                    "/api/users/*/liked-directors"
                ).permitAll()
                .requestMatchers("/api/mcp/tools/**").permitAll()
                .requestMatchers("/api/user-login").permitAll()
                .requestMatchers("/api/user-ratings/movie/*/average").permitAll()
                .requestMatchers("/api/user-ratings/movie/*/distribution").permitAll()
                .requestMatchers("/api/ratings/movie/*/distribution").permitAll()
                .requestMatchers("/api/ratings/movie/*/average").permitAll()
                .requestMatchers("/api/movies/*/like").authenticated()
                .requestMatchers("/api/person/*/like").authenticated()
                .requestMatchers("/api/person/*/like-status").permitAll()
                .requestMatchers("/api/movies/**").hasRole("ADMIN")
                .requestMatchers("/api/reviews/movie/*").permitAll()  // 리뷰 목록 조회는 누구나
                .requestMatchers("/api/reviews/movie/*/content-only").permitAll()  // 댓글만 조회도 누구나
                .requestMatchers("/api/reviews/**").authenticated()  // 나머지 리뷰 관련 기능은 인증 필요
                .requestMatchers("/api/search-history").authenticated()
                .requestMatchers("/api/users/*/liked-movies").permitAll()
                .requestMatchers("/api/users/*/liked-actors").permitAll()
                .requestMatchers("/api/users/*/liked-directors").permitAll()
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
                        response.sendRedirect("http://localhost:3000/login");
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
                        response.sendRedirect("http://localhost:3000/error");
                    }
                })
            );

        return http.build();
    }
} 

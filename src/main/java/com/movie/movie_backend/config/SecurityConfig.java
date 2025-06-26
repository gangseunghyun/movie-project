package com.movie.movie_backend.config;

import com.movie.movie_backend.service.UserDetailServiceImpl;
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
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler() {
        return new CustomAuthenticationSuccessHandler();
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
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                        .allowedOrigins("http://localhost:3000")
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowCredentials(true);
            }
        };
    }

    @Bean
    public SecurityFilterChain filterChain(
        HttpSecurity http,
        UserDetailServiceImpl userDetailService,
        CustomOAuth2UserService customOAuth2UserService
    ) throws Exception {
        AuthenticationManagerBuilder authBuilder = http.getSharedObject(AuthenticationManagerBuilder.class);
        authBuilder.userDetailsService(userDetailService).passwordEncoder(passwordEncoder());
        AuthenticationManager authenticationManager = authBuilder.build();

        http
            .csrf().disable()
            .cors().and()
            .authenticationManager(authenticationManager)
            .authorizeHttpRequests()
                .requestMatchers(
                    "/api/login",
                    "/api/users/join",
                    "/api/users/check-login-id",
                    "/api/users/check-email",
                    "/api/users/check-nickname",
                    "/api/users/recommend-nickname",
                    "/api/mail/**",
                    "/reset-password",
                    "/static/**",
                    "/resources/static/**"
                ).permitAll()
                .requestMatchers("/api/search-history/**").authenticated()
                .anyRequest().authenticated()
            .and()
            .formLogin().disable()
            .httpBasic().disable()
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login.html")
                .permitAll()
            )
            .oauth2Login(oauth2 -> oauth2
                .loginPage("http://localhost:3000/login")
                .successHandler(customAuthenticationSuccessHandler())
                .failureHandler(customAuthenticationFailureHandler())
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(customOAuth2UserService)
                )
            );

        return http.build();
    }
} 
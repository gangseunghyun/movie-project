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
                    getRedirectStrategy().sendRedirect(request, response, "/login?error=true&message=" + errorMessage);
                } else {
                    getRedirectStrategy().sendRedirect(request, response, "/login?error=true&message=" + errorMessage + "&social=" + provider);
                }
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
            .csrf(csrf -> csrf.disable())
            .authenticationManager(authenticationManager)
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/", "/join", "/login", "/css/**", "/js/**", "/images/**").permitAll()
                .requestMatchers("/api/users/**").permitAll()
                .requestMatchers("/api/mail/**").permitAll()
                .requestMatchers("/terms/**").permitAll()
                .requestMatchers("/forgot-password").permitAll()
                .requestMatchers("/reset-password").permitAll()
                .requestMatchers("/find-id").permitAll()
                .requestMatchers("/social-join").permitAll()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/", true)
                .failureUrl("/login?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login")
                .permitAll()
            )
            .oauth2Login(oauth2 -> oauth2
                .loginPage("/login")
                .successHandler(customAuthenticationSuccessHandler())
                .failureHandler(customAuthenticationFailureHandler())
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(customOAuth2UserService)
                )
            );

        return http.build();
    }
} 
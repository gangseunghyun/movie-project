package com.movie.movie_backend.handler;

import com.movie.movie_backend.entity.User;
import com.movie.movie_backend.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import java.io.IOException;

public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {
    @Autowired
    private UserRepository userRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        String provider = null;
        String providerId = null;
        String email = null;
        if (authentication != null && authentication.getPrincipal() instanceof org.springframework.security.oauth2.core.user.DefaultOAuth2User) {
            org.springframework.security.oauth2.core.user.DefaultOAuth2User oauth2User = (org.springframework.security.oauth2.core.user.DefaultOAuth2User) authentication.getPrincipal();
            provider = oauth2User.getAttribute("provider");
            providerId = oauth2User.getAttribute("providerId");
            if (providerId == null) providerId = oauth2User.getAttribute("sub");
            email = oauth2User.getAttribute("email");
        }
        // DB에서 회원 여부만 판단
        User user = null;
        if (provider != null && providerId != null) {
            user = userRepository.findByProviderAndProviderId(provider, providerId).orElse(null);
        } else if (email != null) {
            user = userRepository.findByEmail(email).orElse(null);
        }
        if (user == null || user.getNickname() == null || !user.isSocialJoinCompleted()) {
            response.sendRedirect("/social-join");
        } else {
            response.sendRedirect("/");
        }
    }
} 
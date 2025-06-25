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
        if (user == null) {
            System.out.println("[DEBUG] SuccessHandler: user==null, provider=" + provider + ", providerId=" + providerId + ", email=" + email);
        }
        if (user == null || user.getNickname() == null || !user.isSocialJoinCompleted()) {
            // 세션 값 확인
            HttpSession session = request.getSession(false);
            if (session == null) {
                System.out.println("[DEBUG] SuccessHandler: 세션 없음");
            } else {
                Object emailAttr = session.getAttribute("SOCIAL_EMAIL");
                Object providerAttr = session.getAttribute("SOCIAL_PROVIDER");
                Object providerIdAttr = session.getAttribute("SOCIAL_PROVIDER_ID");
                System.out.println("[DEBUG] SuccessHandler 세션: email=" + emailAttr + ", provider=" + providerAttr + ", providerId=" + providerIdAttr);
                if (emailAttr == null || providerAttr == null || providerIdAttr == null) {
                    System.out.println("[DEBUG] SuccessHandler: 소셜 세션 정보 없음");
                }
            }
            response.sendRedirect("http://localhost:3000/social-join");
        } else {
            // 닉네임을 쿼리 파라미터로 포함하여 리다이렉트
            String nickname = user.getNickname() != null ? java.net.URLEncoder.encode(user.getNickname(), "UTF-8") : "";
            response.sendRedirect("http://localhost:3000/?social=success&nickname=" + nickname);
        }
    }
} 
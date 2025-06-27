package com.movie.movie_backend.handler;

import com.movie.movie_backend.entity.User;
import com.movie.movie_backend.repository.USRUserRepository;
import com.movie.movie_backend.constant.Provider;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import java.io.IOException;
import java.util.Map;

public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {
    @Autowired
    private USRUserRepository userRepository;

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
            
            // 카카오의 경우 email이 kakao_account 안에 있을 수 있음
            if (email == null && "KAKAO".equals(provider)) {
                @SuppressWarnings("unchecked")
                Map<String, Object> kakaoAccount = (Map<String, Object>) oauth2User.getAttribute("kakao_account");
                if (kakaoAccount != null) {
                    email = (String) kakaoAccount.get("email");
                }
            }
        }
        
        // DB에서 회원 여부만 판단
        User user = null;
        if (provider != null && providerId != null) {
            Provider providerEnum = Provider.valueOf(provider.toUpperCase());
            user = userRepository.findByProviderAndProviderId(providerEnum, providerId).orElse(null);
        } else if (email != null) {
            user = userRepository.findByEmail(email).orElse(null);
        }
        
        if (user == null) {
            System.out.println("[DEBUG] SuccessHandler: user==null, provider=" + provider + ", providerId=" + providerId + ", email=" + email);
        }
        
        if (user == null || user.getNickname() == null || !user.isSocialJoinCompleted()) {
            response.sendRedirect("http://localhost:3000/social-join");
        } else {
            // 닉네임을 쿼리 파라미터로 포함하여 리다이렉트
            String nickname = user.getNickname() != null ? java.net.URLEncoder.encode(user.getNickname(), "UTF-8") : "";
            response.sendRedirect("http://localhost:3000/?social=success&nickname=" + nickname);
        }
    }
} 

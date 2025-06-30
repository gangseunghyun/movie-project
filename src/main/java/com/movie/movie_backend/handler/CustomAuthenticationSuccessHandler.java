package com.movie.movie_backend.handler;

import com.movie.movie_backend.entity.User;
import com.movie.movie_backend.repository.USRUserRepository;
import com.movie.movie_backend.constant.Provider;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.util.Map;

@Component
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
        }
        
        if (user == null) {
            System.out.println("[DEBUG] SuccessHandler: user==null, provider=" + provider + ", providerId=" + providerId + ", email=" + email);
        } else {
            System.out.println("[DEBUG] SuccessHandler: user found, nickname=" + user.getNickname() + ", socialJoinCompleted=" + user.isSocialJoinCompleted());
            
            // 소셜 로그인 성공 시 세션 업데이트
            HttpSession session = request.getSession(true);
            // 소셜 사용자의 경우 loginId가 null일 수 있으므로 email을 사용
            String loginId = user.getLoginId() != null ? user.getLoginId() : user.getEmail();
            session.setAttribute("USER_LOGIN_ID", loginId);
            session.setAttribute("SOCIAL_USER_ID", String.valueOf(user.getId()));
            session.setAttribute("SOCIAL_PROVIDER", provider);
            session.setAttribute("SOCIAL_PROVIDER_ID", providerId);
            // 프론트엔드에서 로그인 상태를 인식할 수 있도록 user 객체도 저장
            session.setAttribute("user", user);
            session.setMaxInactiveInterval(3600); // 1시간
            
            System.out.println("[DEBUG] SuccessHandler: 세션 업데이트 완료 - USER_LOGIN_ID: " + loginId);
        }
        
        if (user == null || user.getNickname() == null || !user.isSocialJoinCompleted()) {
            System.out.println("[DEBUG] SuccessHandler: Redirecting to social-join page");
            response.sendRedirect("http://localhost:3000/social-join");
        } else {
            System.out.println("[DEBUG] SuccessHandler: Redirecting to main page with nickname=" + user.getNickname());
            // 닉네임을 쿼리 파라미터로 포함하여 리다이렉트
            String nickname = user.getNickname() != null ? java.net.URLEncoder.encode(user.getNickname(), "UTF-8") : "";
            response.sendRedirect("http://localhost:3000/?social=success&nickname=" + nickname);
        }
    }
} 

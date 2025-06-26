package com.movie.service;

import com.movie.entity.User;
import com.movie.repository.USRUserRepository;
import com.movie.constant.Provider;
import com.movie.constant.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.Collections;
import java.util.HashMap;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import org.springframework.security.core.AuthenticationException;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    private final USRUserRepository userRepository;
    private final @Lazy PasswordEncoder passwordEncoder;
    @Autowired private HttpServletRequest request;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        String registrationId = userRequest.getClientRegistration().getRegistrationId(); // google, naver, kakao
        Map<String, Object> attributes = oAuth2User.getAttributes();

        Provider provider = getProviderFromRegistrationId(registrationId);
        String providerId = null;
        String email = null;
        String name = null;

        if ("google".equals(registrationId)) {
            providerId = (String) attributes.get("sub");
            email = (String) attributes.get("email");
            name = (String) attributes.get("name");
        }
        if ("kakao".equals(registrationId)) {
            providerId = String.valueOf(attributes.get("id"));
            Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
            if (kakaoAccount != null) {
                email = (String) kakaoAccount.get("email");
                Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
                if (profile != null) {
                    name = (String) profile.get("nickname");
                }
            }
        }
        if ("naver".equals(registrationId)) {
            Map<String, Object> response = (Map<String, Object>) attributes.get("response");
            if (response != null) {
                providerId = (String) response.get("id");
                email = (String) response.get("email");
                name = (String) response.get("nickname");
                if (name == null) name = (String) response.get("name");
                attributes = response; // 네이버는 response 맵을 attributes로 사용
            }
        }

        // null 체크 (닉네임은 name, providerId, email 모두)
        if (email == null || providerId == null || name == null || name.isBlank()) {
            throw new AuthenticationException("소셜 로그인에서 필수 정보(email, id, nickname)를 받아오지 못했습니다. 소셜 제공 동의 항목을 확인해 주세요.") {};
        }

        String nickname = (name != null && !name.isBlank()) ? name : (email != null ? email.split("@")[0] : "user");

        final String finalProviderId = providerId;
        final String finalEmail = email;
        final String finalNickname = nickname;

        // 이미 가입된 사용자인지 확인
        User user = userRepository.findByProviderAndProviderId(provider, providerId)
                .orElseGet(() -> {
                    // 이메일로 이미 가입된 사용자가 있는지 추가로 확인
                    Optional<User> existingByEmail = userRepository.findByEmail(finalEmail);
                    if (existingByEmail.isPresent()) {
                        Provider existProvider = existingByEmail.get().getProvider();
                        if (existProvider == Provider.LOCAL) {
                            throw new AuthenticationException("PROVIDER:local|이 이메일은 일반 계정으로 가입되어 있습니다. 아이디/비밀번호로 로그인해 주세요.") {};
                        } else {
                            throw new AuthenticationException("PROVIDER:" + existProvider.name() + "|이 이메일은 '" + existProvider.getDisplayName() + "' 소셜 계정입니다. 해당 소셜 로그인 버튼을 이용해 주세요.") {};
                        }
                    }
                    // 신규 회원이면 생성
                    return User.builder()
                            .loginId(finalEmail)
                            .password(passwordEncoder.encode(provider.name() + finalProviderId)) // 소셜 로그인은 임의 비번
                            .email(finalEmail)
                            .nickname(null) // 최초 로그인 시 닉네임 없음
                            .role(UserRole.USER)
                            .provider(provider)
                            .providerId(finalProviderId)
                            .emailVerified(true)
                            .socialJoinCompleted(false)
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .build();
                });

        // 닉네임이 없으면 세션에 소셜 사용자 정보 저장
        if (user.getNickname() == null || user.getNickname().isBlank()) {
            System.out.println("[DEBUG] 세션 저장: provider=" + provider + ", providerId=" + providerId + ", email=" + email);
            HttpSession session = request.getSession();
            session.setAttribute("SOCIAL_EMAIL", email);
            session.setAttribute("SOCIAL_PROVIDER", provider.name());
            session.setAttribute("SOCIAL_PROVIDER_ID", providerId);
            // SuccessHandler에서 /social-join으로 리다이렉트
        }

        // 이미 가입된 경우에도 정보 갱신 (nickname은 덮어쓰지 않음)
        user.setEmail(email);
        user.setProvider(provider);
        user.setProviderId(providerId);
        user.setEmailVerified(true);
        user.setUpdatedAt(LocalDateTime.now());
        try {
            System.out.println("[DEBUG] 소셜 유저 저장 시도: " + user.getEmail());
            userRepository.save(user);
            System.out.println("[DEBUG] 소셜 유저 저장 성공: " + user.getEmail());
        } catch (Exception e) {
            System.out.println("[ERROR] 소셜 유저 저장 실패: " + e.getMessage());
            e.printStackTrace();
        }

        // attributes에 DB의 닉네임, provider, providerId, sub를 반드시 포함시켜서 반환
        Map<String, Object> customAttributes = new HashMap<>(attributes);
        customAttributes.put("nickname", user.getNickname());
        customAttributes.put("provider", provider.name());
        customAttributes.put("providerId", providerId);
        // sub는 providerId로 통일
        if ("naver".equals(registrationId)) {
            customAttributes.put("sub", providerId);
        } else {
            customAttributes.put("sub", providerId); // google, kakao도 동일하게
        }

        return new org.springframework.security.oauth2.core.user.DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")),
                customAttributes,
                "id".equals(registrationId) ? "id" : "sub" // 네이버는 "id", 그 외는 "sub"
        );
    }

    private Provider getProviderFromRegistrationId(String registrationId) {
        switch (registrationId.toLowerCase()) {
            case "google":
                return Provider.GOOGLE;
            case "kakao":
                return Provider.KAKAO;
            case "naver":
                return Provider.NAVER;
            case "facebook":
                return Provider.FACEBOOK;
            default:
                return Provider.LOCAL;
        }
    }
} 
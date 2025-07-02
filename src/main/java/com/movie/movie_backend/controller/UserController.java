package com.movie.movie_backend.controller;

import com.movie.movie_backend.dto.UserJoinRequestDto;
import com.movie.movie_backend.dto.MovieDetailDto;
import com.movie.movie_backend.entity.PasswordResetToken;
import com.movie.movie_backend.entity.User;
import com.movie.movie_backend.entity.Tag;
import com.movie.movie_backend.entity.MovieDetail;
import com.movie.movie_backend.entity.Like;
import com.movie.movie_backend.entity.PersonLike;
import com.movie.movie_backend.constant.PersonType;
import com.movie.movie_backend.entity.Actor;
import com.movie.movie_backend.entity.Director;
import com.movie.movie_backend.entity.Review;
import com.movie.movie_backend.mapper.MovieMapper;
import com.movie.movie_backend.mapper.MovieDetailMapper;
import com.movie.movie_backend.repository.PasswordResetTokenRepository;
import com.movie.movie_backend.repository.USRUserRepository;
import com.movie.movie_backend.repository.PRDTagRepository;
import com.movie.movie_backend.repository.PRDMovieRepository;
import com.movie.movie_backend.repository.REVLikeRepository;
import com.movie.movie_backend.repository.PersonLikeRepository;
import com.movie.movie_backend.repository.ReviewLikeRepository;
import com.movie.movie_backend.repository.REVReviewRepository;
import com.movie.movie_backend.service.MailService;
import com.movie.movie_backend.service.USRUserService;
import com.movie.movie_backend.constant.Provider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.Collections;

@RestController
@RequiredArgsConstructor
@Slf4j
public class UserController {
    
    private final USRUserService userService;
    private final USRUserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final MailService mailService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final PRDTagRepository tagRepository;
    private final MovieMapper movieMapper;
    private final PRDMovieRepository movieRepository;
    private final REVLikeRepository likeRepository;
    private final MovieDetailMapper movieDetailMapper;
    private final PersonLikeRepository personLikeRepository;
    private final ReviewLikeRepository reviewLikeRepository;
    private final REVReviewRepository reviewRepository;
    
    // REST API - 회원가입
    @PostMapping("/api/users/join")
    public ResponseEntity<Map<String, Object>> joinApi(@Valid @RequestBody UserJoinRequestDto requestDto) {
        Map<String, Object> response = new HashMap<>();
        try {
            log.info("=== 회원가입 요청 시작 ===");
            log.info("요청 DTO: {}", requestDto);
            log.info("아이디: {}", requestDto.getLoginId());
            log.info("이메일: {}", requestDto.getEmail());
            log.info("닉네임: {}", requestDto.getNickname());
            
            userService.join(requestDto);
            
            log.info("회원가입 성공: {}", requestDto.getLoginId());
            response.put("success", true);
            response.put("message", "회원가입이 완료되었습니다.");
            response.put("nickname", requestDto.getNickname());
            response.put("loginId", requestDto.getLoginId());
            response.put("email", requestDto.getEmail());
            response.put("redirect", "/login"); // 로그인 페이지로 리다이렉트 안내
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("회원가입 실패: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            log.error("회원가입 중 예상치 못한 오류: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "회원가입 중 오류가 발생했습니다.");
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    // REST API - 아이디 중복 확인
    @GetMapping("/api/users/check-login-id")
    public ResponseEntity<Map<String, Object>> checkLoginId(@RequestParam String loginId) {
        Map<String, Object> response = new HashMap<>();
        boolean isDuplicate = userService.checkLoginIdDuplicate(loginId);
        response.put("duplicate", isDuplicate);
        response.put("available", !isDuplicate);
        response.put("message", isDuplicate ? "이미 사용 중인 아이디입니다." : "사용 가능한 아이디입니다.");
        return ResponseEntity.ok(response);
    }
    
    // REST API - 이메일 중복 확인
    @GetMapping("/api/users/check-email")
    public ResponseEntity<Map<String, Object>> checkEmail(@RequestParam String email) {
        Map<String, Object> response = new HashMap<>();
        boolean isDuplicate = userService.checkEmailDuplicate(email);
        response.put("duplicate", isDuplicate);
        response.put("available", !isDuplicate);
        response.put("message", isDuplicate ? "이미 사용 중인 이메일입니다." : "사용 가능한 이메일입니다.");
        return ResponseEntity.ok(response);
    }
    
    // REST API - 닉네임 중복 확인
    @GetMapping("/api/users/check-nickname")
    public ResponseEntity<Map<String, Object>> checkNickname(@RequestParam String nickname) {
        Map<String, Object> response = new HashMap<>();
        boolean isDuplicate = userService.checkNicknameDuplicate(nickname);
        response.put("duplicate", isDuplicate);
        response.put("available", !isDuplicate);
        response.put("message", isDuplicate ? "이미 사용 중인 닉네임입니다." : "사용 가능한 닉네임입니다.");
        return ResponseEntity.ok(response);
    }
    
    // REST API - 닉네임 추천
    @GetMapping("/api/users/recommend-nickname")
    public ResponseEntity<Map<String, Object>> recommendNickname() {
        Map<String, Object> response = new HashMap<>();
        response.put("nicknames", userService.recommendNicknames());
        return ResponseEntity.ok(response);
    }

    // REST API - 아이디 찾기
    @PostMapping("/api/find-id")
    public ResponseEntity<Map<String, Object>> findIdApi(@RequestBody Map<String, String> req) {
        Map<String, Object> response = new HashMap<>();
        String email = req.get("email");
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            response.put("success", false);
            response.put("message", "가입된 이메일이 아닙니다.");
            return ResponseEntity.ok(response);
        }
        if (user.getProvider() != null && !user.getProvider().name().equals("LOCAL")) {
            response.put("success", false);
            response.put("message", "이 이메일은 '" + user.getProvider().getDisplayName() + "' 소셜 계정입니다. 해당 소셜 로그인 버튼을 이용해 주세요.");
            return ResponseEntity.ok(response);
        }
        String maskedLoginId = maskLoginId(user.getLoginId());
        response.put("success", true);
        response.put("maskedLoginId", maskedLoginId);
        response.put("message", "아이디를 찾았습니다.");
        return ResponseEntity.ok(response);
    }

    // 로그인 ID 마스킹 유틸
    private String maskLoginId(String loginId) {
        if (loginId == null || loginId.length() <= 2) return loginId;
        return loginId.substring(0, 2) + "***" + loginId.substring(loginId.length() - 1);
    }

    // REST API: 비밀번호 찾기(소셜/자체 분기)
    @PostMapping("/api/forgot-password")
    public ResponseEntity<Map<String, Object>> forgotPasswordApi(@RequestBody Map<String, String> req) {
        Map<String, Object> response = new HashMap<>();
        String email = req.get("email");
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            response.put("type", "NOT_FOUND");
            response.put("message", "가입된 이메일이 아닙니다.");
            return ResponseEntity.ok(response);
        }
        if (user.getProvider() != null && user.getProvider().name().equals("LOCAL") == false) {
            response.put("type", "SOCIAL_ONLY");
            response.put("provider", user.getProvider().name());
            response.put("email", user.getEmail());
            response.put("nickname", user.getNickname());
            response.put("message", user.getProvider().getDisplayName() + " 소셜 로그인 전용 계정입니다. 자체 로그인(비밀번호)도 사용하시겠습니까?");
            return ResponseEntity.ok(response);
        }
        PasswordResetToken token = userService.createPasswordResetToken(email);
        String resetLink = "http://localhost:3000/reset-password?token=" + token.getToken();
        mailService.sendResetPasswordEmail(email, resetLink);
        response.put("type", "NORMAL");
        response.put("message", "비밀번호 재설정 링크가 이메일로 발송되었습니다.");
        return ResponseEntity.ok(response);
    }

    // REST API - 비밀번호 재설정 토큰 검증
    @PostMapping("/api/reset-password/validate-token")
    public ResponseEntity<Map<String, Object>> validateResetToken(@RequestParam String token) {
        Map<String, Object> response = new HashMap<>();
        try {
            userService.validatePasswordResetToken(token);
            response.put("success", true);
            response.put("message", "유효한 토큰입니다.");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // REST API - 비밀번호 재설정
    @PostMapping("/api/reset-password")
    public ResponseEntity<Map<String, Object>> resetPasswordApi(@RequestParam String token, 
                                                               @RequestParam String newPassword, 
                                                               @RequestParam String newPasswordConfirm) {
        Map<String, Object> response = new HashMap<>();
        
        if (!newPassword.equals(newPasswordConfirm)) {
            response.put("success", false);
            response.put("message", "비밀번호가 일치하지 않습니다.");
            return ResponseEntity.badRequest().body(response);
        }
        
        if (!isValidPassword(newPassword)) {
            response.put("success", false);
            response.put("message", "비밀번호는 8자 이상, 영문/숫자/특수문자를 모두 포함해야 합니다.");
            return ResponseEntity.badRequest().body(response);
        }
        
        try {
            userService.resetPassword(token, newPassword);
            response.put("success", true);
            response.put("message", "비밀번호가 성공적으로 변경되었습니다.");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // 비밀번호 유효성 검사
    private boolean isValidPassword(String password) {
        if (password == null || password.length() < 8) return false;
        boolean hasLetter = password.matches(".*[a-zA-Z].*");
        boolean hasDigit = password.matches(".*\\d.*");
        boolean hasSpecial = password.matches(".*[!@#$%^&*(),.?\":{}|<>].*");
        return hasLetter && hasDigit && hasSpecial;
    }

    // REST API - 소셜 회원가입 추가 정보(닉네임, 약관동의)
    @PostMapping("/api/social-join-complete")
    public ResponseEntity<Map<String, Object>> socialJoinComplete(@RequestBody Map<String, Object> req, HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        String nickname = (String) req.get("nickname");
        Boolean agree = (Boolean) req.get("agree");
        if (nickname == null || nickname.isBlank()) {
            response.put("success", false);
            response.put("message", "닉네임을 입력해 주세요.");
            return ResponseEntity.ok(response);
        }
        if (agree == null || !agree) {
            response.put("success", false);
            response.put("message", "약관에 동의해야 가입이 완료됩니다.");
            return ResponseEntity.ok(response);
        }
        
        // Spring Security Authentication에서 소셜 사용자 정보 추출
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || 
            !(authentication.getPrincipal() instanceof org.springframework.security.oauth2.core.user.DefaultOAuth2User)) {
            response.put("success", false);
            response.put("message", "소셜 로그인이 필요합니다.");
            return ResponseEntity.ok(response);
        }
        
        org.springframework.security.oauth2.core.user.DefaultOAuth2User oauth2User = 
            (org.springframework.security.oauth2.core.user.DefaultOAuth2User) authentication.getPrincipal();
        
        String email = oauth2User.getAttribute("email");
        String provider = oauth2User.getAttribute("provider");
        String providerId = oauth2User.getAttribute("providerId");
        
        // 카카오의 경우 email이 kakao_account 안에 있을 수 있음
        if (email == null && "KAKAO".equals(provider)) {
            @SuppressWarnings("unchecked")
            Map<String, Object> kakaoAccount = (Map<String, Object>) oauth2User.getAttribute("kakao_account");
            if (kakaoAccount != null) {
                email = (String) kakaoAccount.get("email");
            }
        }
        
        if (email == null || provider == null || providerId == null) {
            response.put("success", false);
            response.put("message", "소셜 로그인 정보가 올바르지 않습니다.");
            return ResponseEntity.ok(response);
        }
        
        // 해당 유저 찾기
        Provider providerEnum = Provider.valueOf(provider.toUpperCase());
        User user = userRepository.findByProviderAndProviderId(providerEnum, providerId).orElse(null);
        if (user == null) {
            response.put("success", false);
            response.put("message", "사용자를 찾을 수 없습니다. 다시 로그인해 주세요.");
            return ResponseEntity.ok(response);
        }
        
        // 닉네임 중복 체크
        if (userRepository.existsByNickname(nickname)) {
            response.put("success", false);
            response.put("message", "이미 사용 중인 닉네임입니다.");
            return ResponseEntity.ok(response);
        }
        
        user.setNickname(nickname);
        user.setSocialJoinCompleted(true);
        userRepository.save(user);
        
        // 소셜 회원가입 완료 후 세션에 user 객체 저장
        HttpSession session = request.getSession(true);
        session.setAttribute("user", user);
        session.setMaxInactiveInterval(3600); // 1시간
        
        response.put("success", true);
        response.put("message", "소셜 회원가입이 완료되었습니다. 이제 로그인하세요.");
        return ResponseEntity.ok(response);
    }

    // REST API - 로그아웃
    @PostMapping("/api/logout")
    public ResponseEntity<Map<String, Object>> logoutApi(HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        // 세션에서 소셜 로그인 정보 정리
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.removeAttribute("USER_LOGIN_ID");
            session.removeAttribute("SOCIAL_USER_ID");
            session.removeAttribute("SOCIAL_PROVIDER");
            session.removeAttribute("SOCIAL_PROVIDER_ID");
            session.removeAttribute("SPRING_SECURITY_CONTEXT");
            session.removeAttribute("user"); // user 세션도 제거
            log.info("세션 정보 정리 완료");
        }
        
        // Spring Security 컨텍스트 클리어
        SecurityContextHolder.clearContext();
        
        response.put("success", true);
        response.put("message", "로그아웃 성공");
        return ResponseEntity.ok(response);
    }

    // REST API - 닉네임 변경
    @PostMapping("/api/update-nickname")
    public ResponseEntity<Map<String, Object>> updateNickname(@RequestBody Map<String, String> req) {
        Map<String, Object> response = new HashMap<>();
        String newNickname = req.get("nickname");
        
        if (newNickname == null || newNickname.isBlank()) {
            response.put("success", false);
            response.put("message", "닉네임을 입력해 주세요.");
            return ResponseEntity.ok(response);
        }
        
        // Spring Security Authentication에서 사용자 정보 추출
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || 
            "anonymousUser".equals(authentication.getName())) {
            response.put("success", false);
            response.put("message", "로그인이 필요합니다.");
            return ResponseEntity.ok(response);
        }
        
        User currentUser = null;
        
        // OAuth2 사용자인 경우
        if (authentication.getPrincipal() instanceof org.springframework.security.oauth2.core.user.DefaultOAuth2User) {
            org.springframework.security.oauth2.core.user.DefaultOAuth2User oauth2User = 
                (org.springframework.security.oauth2.core.user.DefaultOAuth2User) authentication.getPrincipal();
            
            String email = oauth2User.getAttribute("email");
            String provider = oauth2User.getAttribute("provider");
            String providerId = oauth2User.getAttribute("providerId");
            
            // 카카오의 경우 email이 kakao_account 안에 있을 수 있음
            if (email == null && "KAKAO".equals(provider)) {
                @SuppressWarnings("unchecked")
                Map<String, Object> kakaoAccount = (Map<String, Object>) oauth2User.getAttribute("kakao_account");
                if (kakaoAccount != null) {
                    email = (String) kakaoAccount.get("email");
                }
            }
            
            if (email != null && provider != null && providerId != null) {
                try {
                    Provider providerEnum = Provider.valueOf(provider.toUpperCase());
                    currentUser = userRepository.findByProviderAndProviderId(providerEnum, providerId).orElse(null);
                } catch (Exception e) {
                    log.error("OAuth2 사용자 조회 실패", e);
                }
            }
        }
        // Spring Security로 로그인한 사용자인 경우
        else if (authentication.getPrincipal() instanceof User) {
            currentUser = (User) authentication.getPrincipal();
        }
        // 기타 경우 (loginId로 조회)
        else {
            String loginId = authentication.getName();
            currentUser = userRepository.findByLoginId(loginId).orElse(null);
        }
        
        if (currentUser == null) {
            response.put("success", false);
            response.put("message", "사용자를 찾을 수 없습니다.");
            return ResponseEntity.ok(response);
        }
        
        // 닉네임 중복 체크 (자신의 기존 닉네임은 제외)
        if (!newNickname.equals(currentUser.getNickname()) && userRepository.existsByNickname(newNickname)) {
            response.put("success", false);
            response.put("message", "이미 사용 중인 닉네임입니다.");
            return ResponseEntity.ok(response);
        }
        
        // 닉네임 변경
        currentUser.setNickname(newNickname);
        userRepository.save(currentUser);
        
        response.put("success", true);
        response.put("message", "닉네임이 성공적으로 변경되었습니다.");
        response.put("nickname", newNickname);
        return ResponseEntity.ok(response);
    }

    /**
     * 현재 로그인한 사용자 정보 조회
     */
    @GetMapping("/api/current-user")
    public ResponseEntity<Map<String, Object>> getCurrentUser(HttpServletRequest request) {
        log.info("=== /api/current-user 호출됨 ===");
        
        try {
            // 세션에서 직접 사용자 정보 확인
            HttpSession session = request.getSession(false);
            if (session != null) {
                Object sessionLoginIdObj = session.getAttribute("USER_LOGIN_ID");
                String sessionLoginId = null;
                if (sessionLoginIdObj != null) {
                    sessionLoginId = String.valueOf(sessionLoginIdObj);
                }
                Object socialUserIdObj = session.getAttribute("SOCIAL_USER_ID");
                String socialUserId = null;
                if (socialUserIdObj != null) {
                    socialUserId = String.valueOf(socialUserIdObj);
                }
                String socialProvider = (String) session.getAttribute("SOCIAL_PROVIDER");
                String socialProviderId = (String) session.getAttribute("SOCIAL_PROVIDER_ID");
                
                log.info("세션에서 USER_LOGIN_ID: {}", sessionLoginId);
                log.info("세션에서 SOCIAL_USER_ID: {}", socialUserId);
                log.info("세션에서 SOCIAL_PROVIDER: {}", socialProvider);
                log.info("세션에서 SOCIAL_PROVIDER_ID: {}", socialProviderId);
                
                // 소셜 로그인 사용자인 경우
                if (socialUserId != null && socialProvider != null && socialProviderId != null) {
                    try {
                        Provider providerEnum = Provider.valueOf(socialProvider.toUpperCase());
                        User socialUser = userRepository.findByProviderAndProviderId(providerEnum, socialProviderId).orElse(null);
                        if (socialUser != null) {
                            log.info("소셜 사용자 조회 성공: {}", socialUser.getEmail());
                            return ResponseEntity.ok()
                                .header("Cache-Control", "no-cache, no-store, must-revalidate")
                                .header("Pragma", "no-cache")
                                .header("Expires", "0")
                                .body(Map.of(
                                    "success", true,
                                    "user", Map.of(
                                        "id", socialUser.getId(),
                                        "loginId", socialUser.getLoginId() != null ? socialUser.getLoginId() : "",
                                        "email", socialUser.getEmail(),
                                        "nickname", socialUser.getNickname(),
                                        "role", socialUser.getRole().name(),
                                        "isAdmin", socialUser.isAdmin(),
                                        "isUser", socialUser.isUser()
                                    )
                                ));
                        }
                    } catch (Exception e) {
                        log.error("소셜 사용자 조회 실패", e);
                    }
                }
                
                // 일반 로그인 사용자인 경우
                if (sessionLoginId != null) {
                    User sessionUser = userRepository.findByLoginId(sessionLoginId).orElse(null);
                    if (sessionUser != null) {
                        log.info("세션에서 사용자 조회 성공: {}", sessionUser.getLoginId());
                        return ResponseEntity.ok()
                            .header("Cache-Control", "no-cache, no-store, must-revalidate")
                            .header("Pragma", "no-cache")
                            .header("Expires", "0")
                            .body(Map.of(
                                "success", true,
                                "user", Map.of(
                                    "id", sessionUser.getId(),
                                    "loginId", sessionUser.getLoginId(),
                                    "email", sessionUser.getEmail(),
                                    "nickname", sessionUser.getNickname(),
                                    "role", sessionUser.getRole().name(),
                                    "isAdmin", sessionUser.isAdmin(),
                                    "isUser", sessionUser.isUser()
                                )
                            ));
                    }
                }
            }
            
            // Spring Security Authentication에서 사용자 정보 가져오기
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            log.info("Authentication: {}", authentication);
            log.info("Authentication Principal: {}", authentication.getPrincipal());
            log.info("Authentication Principal Type: {}", authentication.getPrincipal().getClass().getName());
            log.info("Authentication Name: {}", authentication.getName());
            log.info("Authentication isAuthenticated: {}", authentication.isAuthenticated());
            
            User currentUser = null;
            
            if (authentication != null && authentication.isAuthenticated() && 
                !"anonymousUser".equals(authentication.getName())) {
                
                // OAuth2 사용자인 경우
                if (authentication.getPrincipal() instanceof org.springframework.security.oauth2.core.user.DefaultOAuth2User) {
                    org.springframework.security.oauth2.core.user.DefaultOAuth2User oauth2User = 
                        (org.springframework.security.oauth2.core.user.DefaultOAuth2User) authentication.getPrincipal();
                    
                    String email = oauth2User.getAttribute("email");
                    String provider = oauth2User.getAttribute("provider");
                    String providerId = oauth2User.getAttribute("providerId");
                    
                    log.info("OAuth2 사용자 정보 - email: {}, provider: {}, providerId: {}", email, provider, providerId);
                    
                    // 카카오의 경우 email이 kakao_account 안에 있을 수 있음
                    if (email == null && "KAKAO".equals(provider)) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> kakaoAccount = (Map<String, Object>) oauth2User.getAttribute("kakao_account");
                        if (kakaoAccount != null) {
                            email = (String) kakaoAccount.get("email");
                        }
                    }
                    
                    if (email != null && provider != null && providerId != null) {
                        try {
                            Provider providerEnum = Provider.valueOf(provider.toUpperCase());
                            currentUser = userRepository.findByProviderAndProviderId(providerEnum, providerId).orElse(null);
                            log.info("OAuth2 사용자 조회 결과: {}", currentUser);
                        } catch (Exception e) {
                            log.error("OAuth2 사용자 조회 실패", e);
                        }
                    }
                }
                // Spring Security로 로그인한 사용자인 경우 (User 엔티티가 Principal)
                else if (authentication.getPrincipal() instanceof User) {
                    currentUser = (User) authentication.getPrincipal();
                    log.info("Spring Security 사용자 조회: {}", currentUser);
                }
                // 기타 경우 (loginId로 조회) - Spring Security의 UserDetails 구현체
                else {
                    String loginId = authentication.getName();
                    log.info("loginId로 사용자 조회 시도: {}", loginId);
                    currentUser = userRepository.findByLoginId(loginId).orElse(null);
                    log.info("loginId로 사용자 조회 결과: {}", currentUser);
                }
            }
            
            if (currentUser == null) {
                log.warn("인증된 사용자 정보가 없음 - Authentication: {}", authentication);
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("user", null);
                result.put("message", "비로그인 상태입니다.");
                return ResponseEntity.ok(result);
            }
            
            log.info("사용자 정보 조회 성공: {}", currentUser.getLoginId());
            log.info("사용자 역할: {}", currentUser.getRole());
            log.info("관리자 여부: {}", currentUser.isAdmin());
            
            return ResponseEntity.ok()
                .header("Cache-Control", "no-cache, no-store, must-revalidate")
                .header("Pragma", "no-cache")
                .header("Expires", "0")
                .body(Map.of(
                    "success", true,
                    "user", Map.of(
                        "id", currentUser.getId(),
                        "loginId", currentUser.getLoginId() != null ? currentUser.getLoginId() : "",
                        "email", currentUser.getEmail() != null ? currentUser.getEmail() : "",
                        "nickname", currentUser.getNickname() != null ? currentUser.getNickname() : "",
                        "role", currentUser.getRole().name(),
                        "isAdmin", currentUser.isAdmin(),
                        "isUser", currentUser.isUser()
                    )
                ));
        } catch (Exception e) {
            log.error("현재 사용자 정보 조회 실패", e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "사용자 정보 조회에 실패했습니다: " + e.getMessage());
            return ResponseEntity.badRequest()
                .header("Cache-Control", "no-cache, no-store, must-revalidate")
                .header("Pragma", "no-cache")
                .header("Expires", "0")
                .body(result);
        }
    }

    // REST API - 자체 로그인
    @PostMapping("/api/user-login")
    public ResponseEntity<Map<String, Object>> loginApi(@RequestBody Map<String, String> loginRequest, HttpServletRequest request) {
        log.info("=== /api/user-login 호출됨 ===");
        
        Map<String, Object> response = new HashMap<>();
        String loginId = loginRequest.get("loginId");
        String password = loginRequest.get("password");
        
        log.info("로그인 시도: {}", loginId);
        
        try {
            // Spring Security AuthenticationManager를 사용한 인증
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginId, password)
            );
            
            log.info("인증 성공: {}", authentication);
            log.info("인증 Principal: {}", authentication.getPrincipal());
            log.info("인증 Principal Type: {}", authentication.getPrincipal().getClass().getName());
            
            // 인증 성공 시 SecurityContext에 설정
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            // 세션에 인증 정보 저장
            HttpSession session = request.getSession(true);
            session.setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());
            
            // 세션을 즉시 저장
            session.setMaxInactiveInterval(3600); // 1시간
            session.setAttribute("USER_LOGIN_ID", loginId);
            
            log.info("세션 ID: {}", session.getId());
            log.info("세션에 SPRING_SECURITY_CONTEXT 저장됨");
            log.info("세션에 USER_LOGIN_ID 저장됨: {}", loginId);
            
            // Authentication에서 User 정보 가져오기
            User user = (User) authentication.getPrincipal();
            
            if (user != null) {
                log.info("로그인 성공: {}", user.getLoginId());
                log.info("사용자 역할: {}", user.getRole());
                log.info("관리자 여부: {}", user.isAdmin());
                
                response.put("success", true);
                response.put("message", "로그인 성공");
                response.put("user", Map.of(
                    "id", user.getId(),
                    "loginId", user.getLoginId(),
                    "nickname", user.getNickname(),
                    "email", user.getEmail(),
                    "role", user.getRole().name(),
                    "isAdmin", user.isAdmin()
                ));
                return ResponseEntity.ok(response);
            } else {
                log.warn("사용자 정보를 찾을 수 없음: {}", loginId);
                response.put("success", false);
                response.put("message", "사용자 정보를 찾을 수 없습니다.");
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            log.error("로그인 실패: {}", e.getMessage());
            response.put("success", false);
            response.put("message", "아이디 또는 비밀번호가 올바르지 않습니다.");
            return ResponseEntity.badRequest().body(response);
        }
    }

    // 유저 닉네임 포함 검색 API
    @GetMapping("/api/users/search")
    public ResponseEntity<?> searchUsersByNickname(@RequestParam String nickname) {
        var users = userRepository.findByNicknameContainingIgnoreCase(nickname);
        // 닉네임만 리스트로 반환
        return ResponseEntity.ok(users.stream().map(User::getNickname).toList());
    }

    // 유저 닉네임 단일 조회 API (마이페이지)
    @GetMapping("/api/users/nickname/{nickname}")
    public ResponseEntity<?> getUserByNickname(@PathVariable String nickname) {
        var userOpt = userRepository.findOneByNickname(nickname);
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        var user = userOpt.get();
        // 마이페이지: 닉네임, 이메일, ID 반환
        Map<String, Object> result = new HashMap<>();
        result.put("id", user.getId());
        result.put("nickname", user.getNickname());
        result.put("email", user.getEmail());
        return ResponseEntity.ok(result);
    }

    // [1] 장르 태그 전체 조회
    @GetMapping("/api/genre-tags")
    public ResponseEntity<List<String>> getGenreTags() {
        List<Tag> tags = tagRepository.findGenreTags();
        List<String> tagNames = tags.stream()
                .map(Tag::getName)
                .collect(Collectors.toList());
        return ResponseEntity.ok(tagNames);
    }

    // [2-1] 사용자 선호 장르 태그 조회
    @GetMapping("/api/users/{userId}/preferred-genres")
    public ResponseEntity<List<String>> getUserPreferredGenres(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.getPreferredGenres(userId));
    }

    // [2-2] 사용자 선호 장르 태그 저장/수정 (전체 교체)
    @PutMapping("/api/users/{userId}/preferred-genres")
    public ResponseEntity<?> setUserPreferredGenres(@PathVariable Long userId, @RequestBody List<String> genreTagNames) {
        userService.setPreferredGenres(userId, genreTagNames);
        return ResponseEntity.ok().build();
    }

    // [2-3] 사용자 선호 태그 조회 (모든 카테고리)
    @GetMapping("/api/users/{userId}/preferred-tags")
    public ResponseEntity<List<String>> getUserPreferredTags(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.getPreferredTags(userId));
    }

    // [2-4] 사용자 선호 태그 저장/수정 (모든 카테고리)
    @PutMapping("/api/users/{userId}/preferred-tags")
    public ResponseEntity<?> setUserPreferredTags(@PathVariable Long userId, @RequestBody List<String> tagNames) {
        userService.setPreferredTags(userId, tagNames);
        return ResponseEntity.ok().build();
    }

    // [2-5] 사용자 특징 태그 제거 (장르 태그만 남김)
    @DeleteMapping("/api/users/{userId}/feature-tags")
    public ResponseEntity<?> removeUserFeatureTags(@PathVariable Long userId) {
        userService.removeFeatureTags(userId);
        return ResponseEntity.ok().build();
    }

    // [3] 사용자 선호 태그 기반 영화 추천 (태그별 그룹화)
    @GetMapping("/api/users/{userId}/recommended-movies")
    public ResponseEntity<Map<String, List<MovieDetailDto>>> getRecommendedMovies(@PathVariable Long userId) {
        // 사용자의 선호 태그 가져오기
        User user = userRepository.findById(userId).orElse(null);
        if (user == null || user.getPreferredTags().isEmpty()) {
            // 선호 태그가 없으면 빈 Map 반환 (마이페이지에서 설정하라고 안내)
            return ResponseEntity.ok(new HashMap<>());
        }

        Map<String, List<MovieDetailDto>> groupedMovies = new HashMap<>();
        for (Tag tag : user.getPreferredTags()) {
            // 1. 해당 태그에 매칭되는 영화 모두 가져오기
            List<MovieDetail> tagMovies = movieRepository.findMoviesByTags(List.of(tag));
            Collections.shuffle(tagMovies); // 랜덤 섞기
            // 2. 20개만 반환 (20개 이하면 있는대로만)
            List<MovieDetailDto> dtos = tagMovies.stream().limit(20).map(movieMapper::toDto).collect(Collectors.toList());
            groupedMovies.put(tag.getName(), dtos);
        }
        return ResponseEntity.ok(groupedMovies);
    }

    // [4] 사용자가 찜한 영화 목록 조회
    @GetMapping("/api/users/{userId}/liked-movies")
    public ResponseEntity<Map<String, Object>> getLikedMovies(@PathVariable Long userId) {
        try {
            log.info("사용자 찜한 영화 목록 조회: {}", userId);
            
            // 사용자가 찜한 영화 목록 조회
            List<Like> likedMovies = likeRepository.findByUserIdOrderByCreatedAtDesc(userId);
            
            // MovieDetailDto로 변환
            List<MovieDetailDto> movieDtos = likedMovies.stream()
                .map(like -> {
                    MovieDetail movie = like.getMovieDetail();
                    int likeCount = likeRepository.countByMovieDetail(movie);
                    boolean likedByMe = true; // 이미 찜한 영화이므로 true
                    return movieDetailMapper.toDto(movie, likeCount, likedByMe);
                })
                .collect(Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", movieDtos);
            response.put("count", movieDtos.size());
            response.put("message", "찜한 영화 목록을 성공적으로 조회했습니다.");
            
            log.info("찜한 영화 목록 조회 성공: {}개", movieDtos.size());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("찜한 영화 목록 조회 실패: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "찜한 영화 목록 조회에 실패했습니다: " + e.getMessage()
            ));
        }
    }

    // [5] 사용자가 좋아요한 배우 목록 조회
    @GetMapping("/api/users/{userId}/liked-actors")
    public ResponseEntity<Map<String, Object>> getLikedActors(@PathVariable Long userId) {
        try {
            log.info("사용자 좋아요한 배우 목록 조회: {}", userId);
            
            // 사용자가 좋아요한 배우 목록 조회
            List<PersonLike> likedActors = personLikeRepository.findByUserIdAndPersonTypeOrderByCreatedAtDesc(userId, PersonType.ACTOR);
            
            // Actor 정보로 변환
            List<Map<String, Object>> actorDtos = likedActors.stream()
                .map(personLike -> {
                    Actor actor = personLike.getActor();
                    Map<String, Object> actorDto = new HashMap<>();
                    actorDto.put("id", actor.getId());
                    actorDto.put("name", actor.getName());
                    actorDto.put("photoUrl", actor.getPhotoUrl());
                    actorDto.put("likeCount", personLikeRepository.countByActorAndPersonType(actor, PersonType.ACTOR));
                    actorDto.put("likedByMe", true); // 이미 좋아요한 배우이므로 true
                    return actorDto;
                })
                .collect(Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", actorDtos);
            response.put("count", actorDtos.size());
            response.put("message", "좋아요한 배우 목록을 성공적으로 조회했습니다.");
            
            log.info("좋아요한 배우 목록 조회 성공: {}개", actorDtos.size());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("좋아요한 배우 목록 조회 실패: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "좋아요한 배우 목록 조회에 실패했습니다: " + e.getMessage()
            ));
        }
    }

    // [6] 사용자가 좋아요한 감독 목록 조회
    @GetMapping("/api/users/{userId}/liked-directors")
    public ResponseEntity<Map<String, Object>> getLikedDirectors(@PathVariable Long userId) {
        try {
            log.info("사용자 좋아요한 감독 목록 조회: {}", userId);
            
            // 사용자가 좋아요한 감독 목록 조회
            List<PersonLike> likedDirectors = personLikeRepository.findByUserIdAndPersonTypeOrderByCreatedAtDesc(userId, PersonType.DIRECTOR);
            
            // Director 정보로 변환
            List<Map<String, Object>> directorDtos = likedDirectors.stream()
                .map(personLike -> {
                    Director director = personLike.getDirector();
                    Map<String, Object> directorDto = new HashMap<>();
                    directorDto.put("id", director.getId());
                    directorDto.put("name", director.getName());
                    directorDto.put("photoUrl", director.getPhotoUrl());
                    directorDto.put("likeCount", personLikeRepository.countByDirectorAndPersonType(director, PersonType.DIRECTOR));
                    directorDto.put("likedByMe", true); // 이미 좋아요한 감독이므로 true
                    return directorDto;
                })
                .collect(Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", directorDtos);
            response.put("count", directorDtos.size());
            response.put("message", "좋아요한 감독 목록을 성공적으로 조회했습니다.");
            
            log.info("좋아요한 감독 목록 조회 성공: {}개", directorDtos.size());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("좋아요한 감독 목록 조회 실패: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "좋아요한 감독 목록 조회에 실패했습니다: " + e.getMessage()
            ));
        }
    }

    // [7] 사용자가 작성한 코멘트(리뷰) 목록 조회
    @GetMapping("/api/users/{userId}/my-comments")
    public ResponseEntity<Map<String, Object>> getMyComments(@PathVariable Long userId) {
        try {
            log.info("사용자 작성 코멘트 목록 조회: {}", userId);
            
            // 사용자가 작성한 리뷰 목록 조회
            List<Review> myReviews = reviewRepository.findByUserIdOrderByCreatedAtDesc(userId);
            
            // 리뷰 정보로 변환
            List<Map<String, Object>> reviewDtos = myReviews.stream()
                .map(review -> {
                    Map<String, Object> reviewDto = new HashMap<>();
                    reviewDto.put("id", review.getId());
                    reviewDto.put("content", review.getContent());
                    reviewDto.put("rating", review.getRating());
                    reviewDto.put("createdAt", review.getCreatedAt());
                    reviewDto.put("updatedAt", review.getUpdatedAt());
                    
                    // 영화 정보 추가
                    MovieDetail movie = review.getMovieDetail();
                    if (movie != null) {
                        reviewDto.put("movieCd", movie.getMovieCd());
                        reviewDto.put("movieNm", movie.getMovieNm());
                        reviewDto.put("posterUrl", movie.getMovieList() != null ? movie.getMovieList().getPosterUrl() : null);
                    }
                    
                    // 좋아요 수 추가
                    int likeCount = reviewLikeRepository.countByReviewId(review.getId());
                    reviewDto.put("likeCount", likeCount);
                    
                    return reviewDto;
                })
                .collect(Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", reviewDtos);
            response.put("count", reviewDtos.size());
            response.put("message", "작성한 코멘트 목록을 성공적으로 조회했습니다.");
            
            log.info("작성한 코멘트 목록 조회 성공: {}개", reviewDtos.size());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("작성한 코멘트 목록 조회 실패: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "작성한 코멘트 목록 조회에 실패했습니다: " + e.getMessage()
            ));
        }
    }
} 

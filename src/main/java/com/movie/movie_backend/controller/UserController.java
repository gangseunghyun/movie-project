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
import com.movie.movie_backend.entity.Comment;
import com.movie.movie_backend.entity.CommentLike;
import com.movie.movie_backend.entity.ReviewLike;
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
import com.movie.movie_backend.repository.CommentLikeRepository;
import com.movie.movie_backend.repository.REVCommentRepository;
import com.movie.movie_backend.service.MailService;
import com.movie.movie_backend.service.USRUserService;
import com.movie.movie_backend.constant.Provider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
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
import com.movie.movie_backend.dto.ReservationReceiptDto;
import com.movie.movie_backend.dto.PaymentDto;
import com.movie.movie_backend.dto.ScreeningDto;
import com.movie.movie_backend.dto.CinemaDto;
import com.movie.movie_backend.dto.TheaterDto;
import com.movie.movie_backend.dto.ScreeningSeatDto;
import com.movie.movie_backend.entity.Reservation;
import com.movie.movie_backend.entity.Payment;
import com.movie.movie_backend.entity.ScreeningSeat;
import com.movie.movie_backend.entity.Screening;
import com.movie.movie_backend.entity.Cinema;
import com.movie.movie_backend.entity.Theater;
import com.movie.movie_backend.repository.ReservationRepository;
import com.movie.movie_backend.repository.ScreeningSeatRepository;
import com.movie.movie_backend.repository.PaymentRepository;
import java.util.Set;

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
    private final CommentLikeRepository commentLikeRepository;
    private final REVCommentRepository commentRepository;
    private final ReservationRepository reservationRepository;
    private final ScreeningSeatRepository screeningSeatRepository;
    private final PaymentRepository paymentRepository;
    
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
                                        "isUser", socialUser.isUser(),
                                        "profileImageUrl", socialUser.getProfileImageUrl()
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
                                    "isUser", sessionUser.isUser(),
                                    "profileImageUrl", sessionUser.getProfileImageUrl()
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
                        "isUser", currentUser.isUser(),
                        "profileImageUrl", currentUser.getProfileImageUrl()
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
        result.put("profileImageUrl", user.getProfileImageUrl()); // 추가
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

    // [2-6] 추천 캐시 완전 삭제 (디버깅용)
    @PostMapping("/api/users/{userId}/clear-recommendation-cache")
    public ResponseEntity<?> clearRecommendationCache(@PathVariable Long userId) {
        userService.clearRecommendationCache(userId);
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
            List<Review> myReviews = reviewRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, Review.ReviewStatus.ACTIVE);
            
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
                    
                    // 댓글 수 추가
                    Long commentCount = commentRepository.getCommentCountByReviewId(review.getId());
                    reviewDto.put("commentCount", commentCount != null ? commentCount.intValue() : 0);
                    
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

    // [8] 사용자가 좋아요한 리뷰 목록 조회 (내가 쓴 리뷰 제외)
    @GetMapping("/api/users/{userId}/liked-reviews")
    public ResponseEntity<Map<String, Object>> getLikedReviews(@PathVariable Long userId) {
        try {
            log.info("사용자 좋아요한 리뷰 목록 조회: {}", userId);

            List<ReviewLike> likedReviews = reviewLikeRepository.findLikedReviewsByUserIdExcludingOwn(userId);

            List<Map<String, Object>> reviewDtos = likedReviews.stream()
                .map(reviewLike -> {
                    Review review = reviewLike.getReview();
                    Map<String, Object> dto = new HashMap<>();
                    dto.put("id", review.getId());
                    dto.put("content", review.getContent());
                    dto.put("rating", review.getRating());
                    dto.put("createdAt", review.getCreatedAt());
                    dto.put("updatedAt", review.getUpdatedAt());
                    // 작성자 정보
                    User reviewUser = review.getUser();
                    dto.put("authorNickname", reviewUser.getNickname());
                    dto.put("authorId", reviewUser.getId());
                    // 영화 정보
                    MovieDetail md = review.getMovieDetail();
                    dto.put("movieCd", md.getMovieCd());
                    dto.put("movieNm", md.getMovieNm());
                    dto.put("posterUrl", md.getMovieList() != null ? md.getMovieList().getPosterUrl() : null);
                    dto.put("genreNm", md.getGenreNm());
                    dto.put("openDt", md.getOpenDt());
                    // 좋아요 수 추가
                    int likeCount = reviewLikeRepository.countByReviewId(review.getId());
                    dto.put("likeCount", likeCount);
                    
                    // 댓글 수 추가
                    Long commentCount = commentRepository.getCommentCountByReviewId(review.getId());
                    dto.put("commentCount", commentCount != null ? commentCount.intValue() : 0);
                    return dto;
                })
                .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", reviewDtos);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("좋아요한 리뷰 목록 조회 실패: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "좋아요한 리뷰 목록 조회에 실패했습니다: " + e.getMessage()
            ));
        }
    }

    // [9] 사용자가 좋아요한 모든 코멘트 목록 조회 (디버깅용)
    @GetMapping("/api/users/{userId}/all-liked-comments")
    public ResponseEntity<Map<String, Object>> getAllLikedComments(@PathVariable Long userId) {
        try {
            log.info("사용자 좋아요한 모든 코멘트 목록 조회 (디버깅): {}", userId);
            
            // 사용자가 좋아요한 모든 코멘트 목록 조회
            List<CommentLike> allLikedComments = commentLikeRepository.findAllLikedCommentsByUserId(userId);
            log.info("조회된 모든 좋아요한 코멘트 수: {}", allLikedComments.size());
            
            // 코멘트 정보로 변환
            List<Map<String, Object>> commentDtos = allLikedComments.stream()
                .map(commentLike -> {
                    Comment comment = commentLike.getComment();
                    Review review = comment.getReview();
                    Map<String, Object> commentDto = new HashMap<>();
                    commentDto.put("id", comment.getId());
                    commentDto.put("content", comment.getContent());
                    commentDto.put("createdAt", comment.getCreatedAt());
                    commentDto.put("updatedAt", comment.getUpdatedAt());
                    
                    // 작성자 정보 추가
                    User commentUser = comment.getUser();
                    if (commentUser != null) {
                        commentDto.put("authorId", commentUser.getId());
                        commentDto.put("authorNickname", commentUser.getNickname());
                        commentDto.put("isMyComment", commentUser.getId().equals(userId));
                    }
                    
                    // 리뷰 정보 추가 (평점 포함)
                    if (review != null) {
                        commentDto.put("rating", review.getRating());
                        
                        // 영화 정보 추가
                        MovieDetail movie = review.getMovieDetail();
                        if (movie != null) {
                            commentDto.put("movieCd", movie.getMovieCd());
                            commentDto.put("movieNm", movie.getMovieNm());
                            commentDto.put("posterUrl", movie.getMovieList() != null ? movie.getMovieList().getPosterUrl() : null);
                            commentDto.put("genreNm", movie.getGenreNm());
                            commentDto.put("openDt", movie.getOpenDt());
                        }
                    }
                    
                    // 좋아요 수 추가
                    int likeCount = commentLikeRepository.countByCommentId(comment.getId());
                    commentDto.put("likeCount", likeCount);
                    commentDto.put("likedByMe", true);
                    
                    return commentDto;
                })
                .collect(Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", commentDtos);
            response.put("count", commentDtos.size());
            response.put("message", "좋아요한 모든 코멘트 목록을 성공적으로 조회했습니다.");
            
            log.info("좋아요한 모든 코멘트 목록 조회 성공: {}개", commentDtos.size());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("좋아요한 모든 코멘트 목록 조회 실패: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "좋아요한 모든 코멘트 목록 조회에 실패했습니다: " + e.getMessage()
            ));
        }
    }

    // 내 예매내역 리스트 조회
    @GetMapping("/api/users/{userId}/reservations")
    public ResponseEntity<List<ReservationReceiptDto>> getMyReservations(@PathVariable Long userId) {
        List<Reservation> reservations = reservationRepository.findByUserId(userId);
        List<ReservationReceiptDto> result = reservations.stream().map(this::toReceiptDto).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    // 단일 예매영수증(상세) 조회
    @GetMapping("/api/users/{userId}/reservations/{reservationId}")
    public ResponseEntity<ReservationReceiptDto> getMyReservationDetail(@PathVariable Long userId, @PathVariable Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId).orElse(null);
        if (reservation == null || !reservation.getUser().getId().equals(userId)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(toReceiptDto(reservation));
    }

    // Reservation -> ReservationReceiptDto 변환
    private ReservationReceiptDto toReceiptDto(Reservation reservation) {
        Screening screening = reservation.getScreening();
        Cinema cinema = screening.getCinema();
        Theater theater = screening.getTheater();
        List<ScreeningSeatDto> seatDtos = reservation.getReservedSeats() != null ?
            reservation.getReservedSeats().stream().map(ScreeningSeatDto::fromEntity).collect(Collectors.toList()) :
            Collections.emptyList();
        List<PaymentDto> paymentDtos = reservation.getPayments() != null ?
            reservation.getPayments().stream().map(this::toPaymentDto).collect(Collectors.toList()) :
            Collections.emptyList();
        return ReservationReceiptDto.builder()
            .reservationId(reservation.getId())
            .reservedAt(reservation.getReservedAt() != null ? reservation.getReservedAt().toString() : null)
            .status(reservation.getStatus() != null ? reservation.getStatus().name() : null)
            .totalAmount(reservation.getTotalAmount() != null ? reservation.getTotalAmount().intValue() : 0)
            .screening(ScreeningDto.fromEntity(screening))
            .cinema(cinema != null ? new CinemaDto(cinema.getId(), cinema.getName(), cinema.getAddress(), cinema.getPhoneNumber(), null) : null)
            .theater(theater != null ? new TheaterDto(theater.getId(), theater.getName(), theater.getTotalSeats(), theater.getCinema() != null ? theater.getCinema().getId() : null) : null)
            .seats(seatDtos)
            .payments(paymentDtos)
            .build();
    }

    // Payment -> PaymentDto 변환
    private PaymentDto toPaymentDto(Payment payment) {
        return PaymentDto.builder()
            .id(payment.getId())
            .amount(payment.getAmount() != null ? payment.getAmount().intValue() : 0)
            .method(payment.getMethod() != null ? payment.getMethod().name() : null)
            .status(payment.getStatus() != null ? payment.getStatus().name() : null)
            .paidAt(payment.getPaidAt() != null ? payment.getPaidAt().toString() : null)
            .receiptUrl(payment.getReceiptUrl())
            .cancelled(payment.isCancelled())
            .cancelReason(payment.getCancelReason())
            .cancelledAt(payment.getCancelledAt() != null ? payment.getCancelledAt().toString() : null)
            .impUid(payment.getImpUid())
            .merchantUid(payment.getMerchantUid())
            .receiptNumber(payment.getReceiptNumber())
            .cardName(payment.getCardName())
            .cardNumberSuffix(payment.getCardNumberSuffix())
            .approvalNumber(payment.getApprovalNumber())
            .userName(payment.getUser() != null ? payment.getUser().getDisplayName() : null)
            .pgResponseCode(payment.getPgResponseCode())
            .pgResponseMessage(payment.getPgResponseMessage())
            .build();
    }

    // ===== 소셜/팔로우 관련 API =====
    @PostMapping("/api/users/{userId}/follow")
    public ResponseEntity<?> followUser(@PathVariable Long userId) {
        Long currentUserId = getCurrentUserId();
        userService.followUser(currentUserId, userId);
        var followers = userService.getFollowers(userId);
        var following = userService.getFollowing(userId);
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "팔로우 성공",
            "followers", followers,
            "following", following
        ));
    }

    @DeleteMapping("/api/users/{userId}/unfollow")
    public ResponseEntity<?> unfollowUser(@PathVariable Long userId) {
        Long currentUserId = getCurrentUserId();
        userService.unfollowUser(currentUserId, userId);
        var followers = userService.getFollowers(userId);
        var following = userService.getFollowing(userId);
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "언팔로우 성공",
            "followers", followers,
            "following", following
        ));
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            throw new RuntimeException("로그인이 필요합니다.");
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof com.movie.movie_backend.entity.User user) {
            return user.getId();
        }
        if (principal instanceof org.springframework.security.oauth2.core.user.DefaultOAuth2User oAuth2User) {
            String email = (String) oAuth2User.getAttribute("email");
            if (email != null) {
                User userEntity = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("유저 정보를 찾을 수 없습니다."));
                return userEntity.getId();
            }
        }
        throw new RuntimeException("유저 정보를 찾을 수 없습니다.");
    }

    public static class UserSimpleDto {
        private Long id;
        private String nickname;
        public UserSimpleDto(User user) {
            this.id = user.getId();
            this.nickname = user.getNickname();
        }
        public Long getId() { return id; }
        public String getNickname() { return nickname; }
    }

    @GetMapping("/api/users/{userId}/followers")
    public ResponseEntity<?> getFollowers(@PathVariable Long userId) {
        var followers = userService.getFollowers(userId)
            .stream()
            .map(UserSimpleDto::new)
            .collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(java.util.Map.of("success", true, "data", followers));
    }

    @GetMapping("/api/users/{userId}/following")
    public ResponseEntity<?> getFollowing(@PathVariable Long userId) {
        var following = userService.getFollowing(userId)
            .stream()
            .map(UserSimpleDto::new)
            .collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(java.util.Map.of("success", true, "data", following));
    }

    @GetMapping("/api/users/{userId}/daily-social-recommendation")
    public ResponseEntity<?> getDailySocialRecommendation(@PathVariable Long userId) {
        return getSocialRecommendationInternal(userId);
    }

    @GetMapping("/api/users/{userId}/main-recommendation")
    public ResponseEntity<?> getMainSocialRecommendation(@PathVariable Long userId) {
        return getSocialRecommendationInternal(userId);
    }

    private ResponseEntity<?> getSocialRecommendationInternal(Long userId) {
        var following = userService.getFollowing(userId);
        if (following.isEmpty()) {
            return ResponseEntity.ok(java.util.Map.of(
                "success", true,
                "message", "팔로잉한 유저가 없습니다.",
                "recommender", null,
                "movies", java.util.Collections.emptyList()
            ));
        }
        // 팔로잉 유저를 반드시 DB에서 새로 조회해서 최신 상태 사용
        var candidates = following.stream()
            .map(f -> userRepository.findById(f.getId()).orElse(null))
            .filter(java.util.Objects::nonNull)
            .filter(f -> {
                boolean hasLiked = f.getLikes() != null && !f.getLikes().isEmpty();
                boolean hasHighRating = f.getRatings() != null && f.getRatings().stream().anyMatch(r -> r.getScore() >= 4.0);
                return hasLiked || hasHighRating;
            }).toList();
        if (candidates.isEmpty()) {
            return ResponseEntity.ok(java.util.Map.of(
                "success", true,
                "message", "추천할 만한 팔로잉 유저가 없습니다.",
                "recommender", null,
                "movies", java.util.Collections.emptyList()
            ));
        }
        java.time.format.DateTimeFormatter minuteFormatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        String minuteKey = java.time.LocalDateTime.now().format(minuteFormatter);
        int seed = (userId + minuteKey).hashCode();
        java.util.Random random = new java.util.Random(seed);
        var recommender = candidates.get(random.nextInt(candidates.size()));
        List<MovieDetail> likedMovies = recommender.getLikes() == null ? java.util.Collections.emptyList() : recommender.getLikes().stream().map(like -> like.getMovieDetail()).toList();
        List<MovieDetail> highRatedMovies = recommender.getRatings() == null ? java.util.Collections.emptyList() : recommender.getRatings().stream().filter(r -> r.getScore() >= 4.0).map(r -> r.getMovieDetail()).toList();
        java.util.Set<MovieDetail> movieSet = new java.util.LinkedHashSet<>(likedMovies);
        movieSet.addAll(highRatedMovies);
        List<java.util.Map<String, Object>> movies = movieSet.stream()
            .map(md -> {
                java.util.Map<String, Object> m = new java.util.HashMap<>();
                m.put("movieCd", md.getMovieCd());
                m.put("movieNm", md.getMovieNm());
                m.put("posterUrl", (md.getMovieList() != null ? md.getMovieList().getPosterUrl() : null));
                m.put("genreNm", md.getGenreNm());
                m.put("openDt", md.getOpenDt());
                return m;
            })
            .collect(java.util.stream.Collectors.toList());
        var recommenderDto = java.util.Map.of(
            "id", recommender.getId(),
            "nickname", recommender.getNickname()
        );
        return ResponseEntity.ok(java.util.Map.of(
            "success", true,
            "recommender", recommenderDto,
            "movies", movies
        ));
    }

    /**
     * 이런 영화 어때요? (새로운 장르 추천)
     * - 사용자가 경험하지 않은(평점/찜/리뷰하지 않은) 장르별 대표 영화를 추천
     * - 각 장르별로 내가 이미 본 영화(평점/찜/리뷰한 영화)는 추천에서 제외
     */
    @GetMapping("/api/users/{userId}/new-genre-recommendation")
    public ResponseEntity<?> getNewGenreRecommendation(
            @PathVariable Long userId,
            @RequestParam(value = "sort", defaultValue = "rating") String sort
    ) {
        // 캐시 무효화 헤더 설정
        HttpHeaders headers = new HttpHeaders();
        headers.setCacheControl("no-cache, no-store, must-revalidate");
        headers.setPragma("no-cache");
        headers.setExpires(0);
        // 1. 전체 장르 목록
        List<Tag> allGenres = tagRepository.findGenreTags();
        Set<String> allGenreNames = allGenres.stream().map(Tag::getName).collect(java.util.stream.Collectors.toSet());

        // 2. 사용자의 선호 태그(장르)
        User user = userRepository.findById(userId).orElse(null);
        Set<String> preferredGenres = user != null && user.getPreferredTags() != null
                ? user.getPreferredTags().stream().map(Tag::getName).collect(java.util.stream.Collectors.toSet())
                : java.util.Collections.emptySet();
        
        // 선호태그 확인을 위한 추가 로그
        System.out.println("[새로운 장르 추천] 사용자 조회: " + (user != null ? "성공" : "실패"));
        if (user != null) {
            System.out.println("[새로운 장르 추천] 사용자 ID: " + user.getId());
            System.out.println("[새로운 장르 추천] 사용자 선호태그 엔티티: " + user.getPreferredTags());
        }

        // 3. 사용자가 평점 남긴 영화의 ID
        Set<Long> ratedMovieIds = new java.util.HashSet<>();
        if (user != null) {
            List<Review> ratedReviews = reviewRepository.findByUserIdAndRatingIsNotNullAndStatusOrderByCreatedAtDesc(userId, Review.ReviewStatus.ACTIVE);
            ratedMovieIds = ratedReviews.stream()
                .filter(r -> r.getMovieDetail() != null)
                .map(r -> r.getMovieDetail().getId())
                .collect(java.util.stream.Collectors.toSet());
        }
        // 4. 사용자가 찜한 영화의 ID
        Set<Long> likedMovieIds = user != null && user.getLikes() != null
                ? user.getLikes().stream()
                    .filter(l -> l.getMovieDetail() != null)
                    .map(l -> l.getMovieDetail().getId())
                    .collect(java.util.stream.Collectors.toSet())
                : java.util.Collections.emptySet();
        // 5. 사용자가 리뷰한 영화의 ID (평점이 없는 리뷰도 포함)
        Set<Long> reviewedMovieIds = new java.util.HashSet<>();
        if (user != null) {
            List<Review> allReviews = reviewRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, Review.ReviewStatus.ACTIVE);
            reviewedMovieIds = allReviews.stream()
                .filter(r -> r.getMovieDetail() != null)
                .map(r -> r.getMovieDetail().getId())
                .collect(java.util.stream.Collectors.toSet());
        }
        // 6. 본 적 있는 영화 ID = 평점 + 찜 + 리뷰
        Set<Long> experiencedMovieIds = new java.util.HashSet<>();
        experiencedMovieIds.addAll(ratedMovieIds);
        experiencedMovieIds.addAll(likedMovieIds);
        experiencedMovieIds.addAll(reviewedMovieIds);

        // 7. 각 미경험 장르별 대표 영화 20개 추천
        java.util.List<java.util.Map<String, Object>> genreResults = new java.util.ArrayList<>();
        for (String genre : allGenreNames) {
            // 해당 장르 영화 전체 조회
            List<MovieDetail> movies = new ArrayList<>(movieRepository.findByGenreNmContaining(genre));
            // 내가 본 영화 제외
            movies = movies.stream()
                .filter(md -> md.getId() != null && !experiencedMovieIds.contains(md.getId()))
                .toList();
            // 정렬
            if ("latest".equalsIgnoreCase(sort)) {
                movies = new ArrayList<>(movies); // 가변 리스트로 변환
                movies.sort((a, b) -> {
                    if (a.getOpenDt() == null && b.getOpenDt() == null) return 0;
                    if (a.getOpenDt() == null) return 1;
                    if (b.getOpenDt() == null) return -1;
                    return b.getOpenDt().compareTo(a.getOpenDt());
                });
            } else if ("random".equalsIgnoreCase(sort)) {
                movies = new ArrayList<>(movies); // 가변 리스트로 변환
                java.util.Collections.shuffle(movies);
            } else { // 평점순(기본)
                movies = new ArrayList<>(movies); // 가변 리스트로 변환
                movies.sort((a, b) -> {
                    Double ar = a.getAverageRating() != null ? a.getAverageRating() : 0.0;
                    Double br = b.getAverageRating() != null ? b.getAverageRating() : 0.0;
                    return Double.compare(br, ar);
                });
            }
            // 최대 20개만
            List<MovieDetail> topMovies = movies.stream().limit(20).toList();
            // DTO 변환
            List<java.util.Map<String, Object>> movieDtos = topMovies.stream().map(md -> {
                java.util.Map<String, Object> m = new java.util.HashMap<>();
                m.put("movieCd", md.getMovieCd());
                m.put("movieNm", md.getMovieNm());
                m.put("posterUrl", md.getMovieList() != null ? md.getMovieList().getPosterUrl() : null);
                m.put("genreNm", md.getGenreNm());
                m.put("openDt", md.getOpenDt());
                m.put("averageRating", md.getAverageRating());
                return m;
            }).toList();
            genreResults.add(java.util.Map.of(
                "genre", genre,
                "movies", movieDtos
            ));
        }
        return ResponseEntity.ok()
            .headers(headers)
            .body(java.util.Map.of(
                "success", true,
                "genres", genreResults
            ));
    }
} 

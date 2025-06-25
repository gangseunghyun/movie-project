package com.movie.movie_backend.controller;

import com.movie.movie_backend.dto.UserJoinRequestDto;
import com.movie.movie_backend.service.UserService;
import com.movie.movie_backend.repository.UserRepository;
import com.movie.movie_backend.entity.User;
import com.movie.movie_backend.entity.PasswordResetToken;
import com.movie.movie_backend.repository.PasswordResetTokenRepository;
import com.movie.movie_backend.service.MailService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class UserController {
    
    private final UserService userService;
    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final MailService mailService;
    private final PasswordEncoder passwordEncoder;
    
    // REST API - 회원가입
    @PostMapping("/api/users/join")
    public ResponseEntity<Map<String, Object>> joinApi(@Valid @RequestBody UserJoinRequestDto requestDto) {
        Map<String, Object> response = new HashMap<>();
        try {
            userService.join(requestDto);
            response.put("success", true);
            response.put("message", "회원가입이 완료되었습니다.");
            response.put("nickname", requestDto.getNickname());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
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
        if (user.getProvider() != null && !user.getProvider().isBlank() && !user.getProvider().equals("local")) {
            response.put("success", false);
            response.put("message", "이 이메일은 '" + user.getProvider() + "' 소셜 계정입니다. 해당 소셜 로그인 버튼을 이용해 주세요.");
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
        if (user.getProvider() != null && !user.getProvider().isBlank()) {
            response.put("type", "SOCIAL_ONLY");
            response.put("provider", user.getProvider());
            response.put("email", user.getEmail());
            response.put("nickname", user.getNickname());
            response.put("message", user.getProvider() + " 소셜 로그인 전용 계정입니다. 자체 로그인(비밀번호)도 사용하시겠습니까?");
            return ResponseEntity.ok(response);
        }
        PasswordResetToken token = userService.createPasswordResetToken(email);
        String resetLink = "http://localhost:8080/reset-password?token=" + token.getToken();
        mailService.sendResetPasswordEmail(email, resetLink);
        response.put("type", "NORMAL");
        response.put("message", "비밀번호 재설정 링크가 이메일로 발송되었습니다.");
        return ResponseEntity.ok(response);
    }

    // REST API: 소셜 전용 계정 → 자체 로그인 통합(비밀번호 설정)
    @PostMapping("/api/social-password-join")
    public ResponseEntity<Map<String, Object>> socialPasswordJoin(@RequestBody Map<String, String> req) {
        Map<String, Object> response = new HashMap<>();
        String email = req.get("email");
        String nickname = req.get("nickname");
        String password = req.get("password");
        String passwordConfirm = req.get("passwordConfirm");
        if (!password.equals(passwordConfirm)) {
            response.put("success", false);
            response.put("message", "비밀번호가 일치하지 않습니다.");
            return ResponseEntity.ok(response);
        }
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            response.put("success", false);
            response.put("message", "가입된 이메일이 아닙니다.");
            return ResponseEntity.ok(response);
        }
        if (user.getProvider() == null || user.getProvider().isBlank()) {
            response.put("success", false);
            response.put("message", "이미 자체 로그인 계정입니다.");
            return ResponseEntity.ok(response);
        }
        user.setPassword(passwordEncoder.encode(password));
        userRepository.save(user);
        response.put("success", true);
        response.put("message", "비밀번호가 설정되었습니다. 이제 자체 로그인도 가능합니다.");
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
    public ResponseEntity<Map<String, Object>> socialJoinComplete(@RequestBody Map<String, Object> req, HttpSession session) {
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
        // 세션에서 소셜 사용자 정보 추출
        String email = (String) session.getAttribute("SOCIAL_EMAIL");
        String provider = (String) session.getAttribute("SOCIAL_PROVIDER");
        String providerId = (String) session.getAttribute("SOCIAL_PROVIDER_ID");
        if (email == null || provider == null || providerId == null) {
            response.put("success", false);
            response.put("message", "소셜 로그인 세션이 만료되었습니다. 다시 로그인해 주세요.");
            return ResponseEntity.ok(response);
        }
        // 해당 유저 찾기
        User user = userRepository.findByProviderAndProviderId(provider, providerId).orElse(null);
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
        response.put("success", true);
        response.put("message", "소셜 회원가입이 완료되었습니다. 이제 로그인하세요.");
        return ResponseEntity.ok(response);
    }
} 
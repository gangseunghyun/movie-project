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
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class UserController {
    
    private final UserService userService;
    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final MailService mailService;
    private final PasswordEncoder passwordEncoder;
    
    // 로그인 성공 시 보여줄 홈 페이지
    @GetMapping("/")
    public String home(Model model, org.springframework.security.core.Authentication authentication) {
        String nickname = null;
        if (authentication != null && authentication.getPrincipal() != null) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof com.movie.movie_backend.entity.User) {
                // 자체 회원
                nickname = ((com.movie.movie_backend.entity.User) principal).getUsername();
            } else if (principal instanceof org.springframework.security.oauth2.core.user.DefaultOAuth2User) {
                // 소셜 회원
                org.springframework.security.oauth2.core.user.DefaultOAuth2User oauth2User = (org.springframework.security.oauth2.core.user.DefaultOAuth2User) principal;
                Object attr = oauth2User.getAttribute("nickname");
                if (attr != null && !attr.toString().isBlank()) {
                    nickname = attr.toString();
                } else {
                    // attributes에 nickname이 없으면 DB에서 직접 조회
                    String provider = oauth2User.getAttribute("provider");
                    String providerId = oauth2User.getAttribute("providerId");
                    if (providerId == null) providerId = oauth2User.getAttribute("sub");
                    if (provider != null && providerId != null) {
                        User user = userRepository.findByProviderAndProviderId(provider, providerId).orElse(null);
                        if (user != null && user.getNickname() != null) {
                            nickname = user.getNickname();
                        }
                    }
                }
            }
        }
        model.addAttribute("nickname", nickname);
        return "index";
    }
    
    // 회원가입 페이지 (테스트용)
    @GetMapping("/join")
    public String joinPage() {
        return "join";
    }
    
    // 회원가입 처리 (테스트용)
    @PostMapping("/join")
    public String join(@Valid UserJoinRequestDto requestDto, 
                      BindingResult bindingResult, 
                      Model model) {
        if (requestDto.getNickname() == null || requestDto.getNickname().trim().isEmpty()) {
            model.addAttribute("error", "닉네임을 입력해주세요.");
            model.addAttribute("requestDto", requestDto); // 기존 입력값 유지
            return "join";
        }
        if (bindingResult.hasErrors()) {
            model.addAttribute("errors", bindingResult.getAllErrors());
            model.addAttribute("requestDto", requestDto);
            return "join";
        }
        try {
            userService.join(requestDto);
            model.addAttribute("message", "회원가입이 완료되었습니다!");
            return "redirect:/login";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("requestDto", requestDto);
            return "join";
        }
    }
    
    // 로그인 페이지
    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }
    
    // REST API - 회원가입
    @PostMapping("/api/users/join")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> joinApi(@Valid @RequestBody UserJoinRequestDto requestDto) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            userService.join(requestDto);
            response.put("success", true);
            response.put("message", "회원가입이 완료되었습니다.");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    // REST API - 아이디 중복 확인
    @GetMapping("/api/users/check-login-id")
    @ResponseBody
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
    @ResponseBody
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
    @ResponseBody
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
    @ResponseBody
    public ResponseEntity<Map<String, Object>> recommendNickname() {
        Map<String, Object> response = new HashMap<>();
        response.put("nicknames", userService.recommendNicknames());
        return ResponseEntity.ok(response);
    }

    // 소셜 로그인 추가 정보 입력 폼
    @GetMapping("/social-join")
    public String socialJoinPage() {
        return "social-join";
    }

    // 소셜 로그인 추가 정보 저장
    @PostMapping("/social-join")
    public String socialJoinSubmit(@RequestParam String nickname,
                                   @RequestParam(required = false) boolean termsService,
                                   @RequestParam(required = false) boolean termsPrivacy,
                                   Model model,
                                   HttpSession session) {
        // 약관 동의 체크
        if (!termsService || !termsPrivacy) {
            model.addAttribute("error", "필수 약관에 모두 동의해야 회원가입이 가능합니다.");
            return "social-join";
        }
        if (nickname == null || nickname.trim().isEmpty()) {
            model.addAttribute("error", "닉네임을 입력해주세요.");
            return "social-join";
        }
        // provider/providerId로 사용자 식별
        String provider = (String) session.getAttribute("SOCIAL_PROVIDER");
        String providerId = (String) session.getAttribute("SOCIAL_PROVIDER_ID");
        System.out.println("[DEBUG] provider: " + provider + ", providerId: " + providerId + ", nickname: " + nickname);
        if (provider == null || providerId == null) {
            model.addAttribute("error", "소셜 로그인 정보가 유실되었습니다. 다시 로그인해주세요.");
            return "social-join";
        }
        // 닉네임 저장
        try {
            userService.updateNicknameByProviderAndProviderId(provider, providerId, nickname);
            // 소셜 회원가입 완료 후 세션 정보 삭제
            session.removeAttribute("SOCIAL_PROVIDER");
            session.removeAttribute("SOCIAL_PROVIDER_ID");
            session.removeAttribute("SOCIAL_EMAIL");
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", "사용자를 찾을 수 없습니다. 다시 로그인해주세요.");
            return "social-join";
        }
        // TODO: 자동 로그인 처리 (Spring Security 인증 세션 갱신)
        return "redirect:/";
    }

    // 소셜 로그인 추가 정보 입력 예외 핸들러
    @ExceptionHandler(RuntimeException.class)
    public String handleSocialJoinRequired(RuntimeException ex, RedirectAttributes redirectAttributes) {
        if ("SOCIAL_JOIN_REQUIRED".equals(ex.getMessage())) {
            // 필요시 세션/쿠키에 소셜 사용자 정보 저장 가능
            return "redirect:/social-join";
        }
        throw ex;
    }

    // 비밀번호 찾기 폼
    @GetMapping("/forgot-password")
    public String forgotPasswordPage() {
        return "forgot-password";
    }

    // 비밀번호 찾기 요청 처리
    @PostMapping("/forgot-password")
    public String forgotPasswordSubmit(@RequestParam String loginId, @RequestParam String email, Model model) {
        // 아이디로 사용자 찾기
        User user = userRepository.findByLoginId(loginId).orElse(null);
        
        if (user == null) {
            model.addAttribute("passwordError", "가입된 아이디가 아닙니다.");
            model.addAttribute("success", false);
            return "find-id";
        }
        
        // 이메일이 일치하는지 확인
        if (!user.getEmail().equals(email)) {
            model.addAttribute("passwordError", "아이디와 이메일이 일치하지 않습니다.");
            model.addAttribute("success", false);
            return "find-id";
        }
        
        PasswordResetToken token = userService.createPasswordResetToken(user.getEmail());
        String resetLink = "http://localhost:8080/reset-password?token=" + token.getToken();
        mailService.sendResetPasswordEmail(user.getEmail(), resetLink);
        model.addAttribute("passwordMessage", "비밀번호 재설정 링크가 이메일로 발송되었습니다.");
        model.addAttribute("success", false);
        return "find-id";
    }

    // 비밀번호 재설정 폼
    @GetMapping("/reset-password")
    public String resetPasswordPage(@RequestParam String token, Model model) {
        try {
            userService.validatePasswordResetToken(token);
            model.addAttribute("token", token);
            return "reset-password";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "reset-password";
        }
    }

    // 비밀번호 재설정 처리
    @PostMapping("/reset-password")
    public String resetPasswordSubmit(@RequestParam String token, @RequestParam String newPassword, @RequestParam String newPasswordConfirm, Model model) {
        if (!newPassword.equals(newPasswordConfirm)) {
            model.addAttribute("error", "비밀번호가 일치하지 않습니다.");
            model.addAttribute("token", token);
            return "reset-password";
        }
        if (!isValidPassword(newPassword)) {
            model.addAttribute("error", "비밀번호는 8자 이상, 영문/숫자/특수문자를 모두 포함해야 합니다.");
            model.addAttribute("token", token);
            return "reset-password";
        }
        try {
            userService.resetPassword(token, newPassword);
            model.addAttribute("message", "비밀번호가 성공적으로 변경되었습니다. 로그인해 주세요.");
            return "login";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "reset-password";
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

    // REST API: 비밀번호 찾기(소셜/자체 분기)
    @PostMapping("/api/forgot-password")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> forgotPasswordApi(@RequestParam String email) {
        Map<String, Object> response = new HashMap<>();
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            response.put("type", "NOT_FOUND");
            response.put("message", "가입된 이메일이 아닙니다.");
            return ResponseEntity.ok(response);
        }
        if (user.getProvider() != null && !user.getProvider().isBlank()) {
            // 소셜 전용 계정
            response.put("type", "SOCIAL_ONLY");
            response.put("provider", user.getProvider());
            response.put("email", user.getEmail());
            response.put("nickname", user.getNickname());
            response.put("message", user.getProvider() + " 소셜 로그인 전용 계정입니다. 자체 로그인(비밀번호)도 사용하시겠습니까?");
            return ResponseEntity.ok(response);
        }
        // 자체 회원
        PasswordResetToken token = userService.createPasswordResetToken(email);
        String resetLink = "http://localhost:8080/reset-password?token=" + token.getToken();
        mailService.sendResetPasswordEmail(email, resetLink);
        response.put("type", "NORMAL");
        response.put("message", "비밀번호 재설정 링크가 이메일로 발송되었습니다.");
        return ResponseEntity.ok(response);
    }

    // REST API: 소셜 전용 계정 → 자체 로그인 통합(비밀번호 설정)
    @PostMapping("/api/social-password-join")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> socialPasswordJoin(@RequestParam String email,
                                                                 @RequestParam String nickname,
                                                                 @RequestParam String password,
                                                                 @RequestParam String passwordConfirm) {
        Map<String, Object> response = new HashMap<>();
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

    // 아이디 찾기 폼
    @GetMapping("/find-id")
    public String findIdPage(Model model) {
        model.addAttribute("success", false);
        return "find-id";
    }

    // 아이디 찾기 처리
    @PostMapping("/find-id")
    public String findIdSubmit(@RequestParam String email, Model model) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            model.addAttribute("error", "가입된 이메일이 아닙니다.");
            model.addAttribute("success", false);
            return "find-id";
        }
        // 소셜 계정 분기
        if (user.getProvider() != null && !user.getProvider().isBlank() && !user.getProvider().equals("local")) {
            model.addAttribute("error", "이 이메일은 '" + user.getProvider() + "' 소셜 계정입니다. 해당 소셜 로그인 버튼을 이용해 주세요.");
            model.addAttribute("success", false);
            return "find-id";
        }
        // 로그인 ID 마스킹 처리
        String maskedLoginId = maskLoginId(user.getLoginId());
        model.addAttribute("maskedLoginId", maskedLoginId);
        model.addAttribute("success", true);
        return "find-id";
    }

    // 로그인 ID 마스킹 유틸
    private String maskLoginId(String loginId) {
        if (loginId == null || loginId.length() <= 2) return loginId;
        return loginId.substring(0, 2) + "***" + loginId.substring(loginId.length() - 1);
    }
} 
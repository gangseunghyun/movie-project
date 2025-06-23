package com.movie.movie_backend.controller;

import com.movie.movie_backend.dto.UserJoinRequestDto;
import com.movie.movie_backend.service.UserService;
import com.movie.movie_backend.repository.UserRepository;
import com.movie.movie_backend.entity.User;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
        
        if (bindingResult.hasErrors()) {
            model.addAttribute("errors", bindingResult.getAllErrors());
            return "join";
        }
        
        try {
            userService.join(requestDto);
            model.addAttribute("message", "회원가입이 완료되었습니다!");
            return "redirect:/login";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
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
} 
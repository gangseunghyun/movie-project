package com.movie.movie_backend.controller;

import com.movie.movie_backend.dto.EmailRequestDto;
import com.movie.movie_backend.dto.EmailVerificationRequestDto;
import com.movie.movie_backend.service.MailService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/mail")
@Slf4j
public class MailController {
    
    private final MailService mailService;
    
    // 이메일 인증 코드 발송
    @PostMapping("/send-verification")
    public ResponseEntity<Map<String, Object>> sendVerificationEmail(@Valid @RequestBody EmailRequestDto requestDto) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            log.info("이메일 인증 요청 받음: {}", requestDto.getEmail());
            mailService.sendVerificationEmail(requestDto.getEmail());
            response.put("success", true);
            response.put("message", "인증 코드가 이메일로 발송되었습니다.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("이메일 발송 실패: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "이메일 발송에 실패했습니다: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    // 이메일 인증 코드 확인
    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyEmail(@Valid @RequestBody EmailVerificationRequestDto requestDto) {
        Map<String, Object> response = new HashMap<>();
        
        boolean isCodeValid = mailService.verifyCode(requestDto.getEmail(), requestDto.getVerificationCode());
        
        if (isCodeValid) {
            response.put("success", true);
            response.put("message", "이메일 인증이 완료되었습니다.");
            return ResponseEntity.ok(response);
        } else {
            response.put("success", false);
            response.put("message", "인증 코드가 올바르지 않습니다.");
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    // 테스트용 간단한 이메일 발송
    @GetMapping("/test")
    public ResponseEntity<Map<String, Object>> testEmail(@RequestParam String email) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            log.info("테스트 이메일 발송 요청: {}", email);
            mailService.sendVerificationEmail(email);
            response.put("success", true);
            response.put("message", "테스트 이메일이 발송되었습니다.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("테스트 이메일 발송 실패: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "테스트 이메일 발송 실패: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
} 
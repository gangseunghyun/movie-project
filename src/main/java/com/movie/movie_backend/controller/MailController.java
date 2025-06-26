package com.movie.movie_backend.controller;

import com.movie.movie_backend.dto.EmailRequestDto;
import com.movie.movie_backend.dto.EmailVerificationRequestDto;
import com.movie.movie_backend.service.MailService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
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
            log.info("=== 이메일 인증 요청 시작 ===");
            log.info("요청 DTO: {}", requestDto);
            log.info("이메일 값: '{}'", requestDto.getEmail());
            log.info("이메일 길이: {}", requestDto.getEmail() != null ? requestDto.getEmail().length() : "null");
            log.info("이메일이 비어있는지: {}", requestDto.getEmail() != null ? requestDto.getEmail().isEmpty() : "null");
            log.info("이메일이 공백인지: {}", requestDto.getEmail() != null ? requestDto.getEmail().trim().isEmpty() : "null");
            log.info("MailService 호출 시작...");
            
            // MailService를 사용하여 인증 코드 생성 및 이메일 발송
            String verificationCode = mailService.sendVerificationEmail(requestDto.getEmail());
            
            log.info("MailService 호출 완료");
            response.put("success", true);
            response.put("message", "인증 코드가 이메일로 발송되었습니다.");
            response.put("email", requestDto.getEmail());
            response.put("code", verificationCode); // 개발용으로만 임시 추가
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("이메일 발송 실패: {}", e.getMessage(), e);
            log.error("예외 타입: {}", e.getClass().getName());
            if (e.getCause() != null) {
                log.error("원인 예외: {}", e.getCause().getClass().getName());
                log.error("원인 메시지: {}", e.getCause().getMessage());
            }
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
            
            // MailService를 사용하여 테스트 이메일 발송
            String verificationCode = mailService.sendVerificationEmail(email);
            
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

    // 검증 오류 처리
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, Object> response = new HashMap<>();
        BindingResult bindingResult = ex.getBindingResult();
        
        log.error("검증 오류 발생: {}", ex.getMessage());
        
        StringBuilder errorMessage = new StringBuilder();
        for (FieldError fieldError : bindingResult.getFieldErrors()) {
            log.error("필드 오류: {} - {}", fieldError.getField(), fieldError.getDefaultMessage());
            errorMessage.append(fieldError.getDefaultMessage()).append("; ");
        }
        
        response.put("success", false);
        response.put("message", "입력 데이터 검증 실패: " + errorMessage.toString());
        response.put("errors", bindingResult.getFieldErrors());
        
        return ResponseEntity.badRequest().body(response);
    }
} 

package com.movie.movie_backend.controller;

import com.movie.movie_backend.dto.EmailRequestDto;
import com.movie.movie_backend.dto.EmailVerificationRequestDto;
import com.movie.movie_backend.service.MailService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/mail")
@RequiredArgsConstructor
public class MailController {

    private final MailService mailService;

    @PostMapping("/send-code")
    public ResponseEntity<String> sendVerificationCode(@Valid @RequestBody EmailRequestDto requestDto) {
        try {
            mailService.sendVerificationEmail(requestDto.getEmail());
            return ResponseEntity.ok("인증 코드가 성공적으로 발송되었습니다.");
        } catch (Exception e) {
            // 실제 운영 환경에서는 로그를 남기고, 사용자에게는 더 일반적인 에러 메시지를 보여주는 것이 좋습니다.
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("인증 코드 발송에 실패했습니다.");
        }
    }

    @PostMapping("/verify-code")
    public ResponseEntity<String> verifyCode(@RequestBody EmailVerificationRequestDto requestDto) {
        try {
            boolean isValid = mailService.verifyCode(requestDto.getEmail(), requestDto.getVerificationCode());
            if (isValid) {
                return ResponseEntity.ok("이메일 인증이 완료되었습니다.");
            } else {
                return ResponseEntity.badRequest().body("인증 코드가 올바르지 않습니다.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("인증 코드 검증에 실패했습니다.");
        }
    }
} 
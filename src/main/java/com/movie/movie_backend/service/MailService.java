package com.movie.movie_backend.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender javaMailSender;
    private final Map<String, String> verificationCodes = new HashMap<>(); // 인증 코드를 임시로 저장할 Map
    private final Set<String> verifiedEmails = new HashSet<>(); // 인증 완료된 이메일을 저장할 Set

    // 6자리 랜덤 인증 코드 생성
    private String createVerificationCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000); // 100000 ~ 999999
        return String.valueOf(code);
    }

    // 이메일 발송 메소드
    public void sendVerificationEmail(String email) throws MessagingException {
        log.info("이메일 발송 시작: {}", email);
        
        String verificationCode = createVerificationCode();
        log.info("생성된 인증 코드: {}", verificationCode);

        try {
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");

            String htmlContent = "<h1>[Movie-Project] 이메일 인증</h1>" +
                                 "<p>아래 인증 코드를 입력하여 이메일 주소를 인증해 주세요.</p>" +
                                 "<h2>" + verificationCode + "</h2>";

            helper.setTo(email);
            helper.setSubject("[Movie-Project] 이메일 인증 코드입니다.");
            helper.setText(htmlContent, true); // true는 HTML 형식으로 보내겠다는 의미

            log.info("메일 메시지 생성 완료, 발송 시도...");
            javaMailSender.send(mimeMessage);
            log.info("이메일 발송 성공: {}", email);

            // 이메일과 인증 코드 저장 (나중에 검증을 위해)
            verificationCodes.put(email, verificationCode);
            
        } catch (Exception e) {
            log.error("이메일 발송 실패: {}", e.getMessage(), e);
            throw e;
        }
    }

    // 인증 코드 검증 메소드
    public boolean verifyCode(String email, String inputCode) {
        String storedCode = verificationCodes.get(email);
        
        if (storedCode == null) {
            log.warn("이메일 {}에 대한 인증 코드가 존재하지 않습니다.", email);
            return false;
        }
        
        boolean isValid = storedCode.equals(inputCode);
        
        if (isValid) {
            // 인증 성공 시 저장된 코드 삭제 (보안상 한 번만 사용 가능)
            verificationCodes.remove(email);
            // 인증 완료된 이메일 목록에 추가
            verifiedEmails.add(email);
            log.info("이메일 {} 인증 성공", email);
        } else {
            log.warn("이메일 {} 인증 실패 - 입력된 코드: {}, 저장된 코드: {}", email, inputCode, storedCode);
        }
        
        return isValid;
    }

    // 이메일 인증 완료 여부 확인 메소드
    public boolean isEmailVerified(String email) {
        return verifiedEmails.contains(email);
    }
} 
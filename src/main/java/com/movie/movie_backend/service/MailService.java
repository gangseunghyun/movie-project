package com.movie.movie_backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class MailService {
    
    private final JavaMailSender mailSender;
    private final CacheManager cacheManager;

    // 이메일을 보내고, 생성된 코드를 'verificationCodes' 캐시에 저장합니다.
    public String sendVerificationEmail(String email) {
        try {
            log.info("이메일 인증 코드 발송 시작: {}", email);
            String verificationCode = generateVerificationCode();
            log.info("생성된 인증 코드: {}", verificationCode);
            
            // 캐시에 인증 코드 저장
            Cache cache = cacheManager.getCache("verificationCodes");
            if (cache != null) {
                cache.put(email, verificationCode);
                log.info("인증 코드를 캐시에 저장: email={}, code={}", email, verificationCode);
            } else {
                log.error("캐시 'verificationCodes'를 찾을 수 없습니다.");
            }
            
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject("[영화 추천 서비스] 이메일 인증 코드");
            message.setText("안녕하세요!\n\n" +
                    "영화 추천 서비스 회원가입을 위한 이메일 인증 코드입니다.\n\n" +
                    "인증 코드: " + verificationCode + "\n\n" +
                    "이 코드는 3분간 유효합니다.\n" +
                    "본인이 요청하지 않은 경우 이 메일을 무시하세요.\n\n" +
                    "감사합니다.");
            
            log.info("이메일 메시지 생성 완료, 발송 시도...");
            mailSender.send(message);
            log.info("이메일 발송 완료: {}", email);

            return verificationCode;
            
        } catch (Exception e) {
            log.error("이메일 발송 실패: {}", email, e);
            throw new RuntimeException("이메일 발송에 실패했습니다: " + e.getMessage(), e);
        }
    }

    // 'verificationCodes' 캐시에서 이메일 키에 해당하는 코드를 조회합니다.
    public String getVerificationCode(String email) {
        Cache cache = cacheManager.getCache("verificationCodes");
        if (cache != null) {
            Cache.ValueWrapper wrapper = cache.get(email);
            if (wrapper != null) {
                String code = (String) wrapper.get();
                log.info("캐시에서 인증 코드 조회: email={}, code={}", email, code);
                return code;
            } else {
                log.warn("캐시에 없는 인증 코드 조회 시도: {}", email);
                return null;
            }
        } else {
            log.error("캐시 'verificationCodes'를 찾을 수 없습니다.");
            return null;
        }
    }

    // 'verificationCodes' 캐시에서 이메일 키에 해당하는 코드를 삭제합니다.
    public void removeVerificationCode(String email) {
        Cache cache = cacheManager.getCache("verificationCodes");
        if (cache != null) {
            cache.evict(email);
            log.info("인증 코드 캐시에서 삭제: {}", email);
        } else {
            log.error("캐시 'verificationCodes'를 찾을 수 없습니다.");
        }
    }

    public boolean verifyCode(String email, String code) {
        String savedCode = getVerificationCode(email);
        log.info("인증 코드 확인: email={}, 입력코드={}, 저장된코드={}", email, code, savedCode);
        return savedCode != null && savedCode.equals(code);
    }

    public boolean verifyAndRemoveCode(String email, String code) {
        boolean isValid = verifyCode(email, code);
        if (isValid) {
            removeVerificationCode(email);
        }
        return isValid;
    }
    
    private String generateVerificationCode() {
        Random random = new Random();
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            code.append(random.nextInt(10));
        }
        return code.toString();
    }

    // 비밀번호 재설정 메일 발송 (HTML 형식)
    public void sendResetPasswordEmail(String email, String resetLink) {
        try {
            log.info("비밀번호 재설정 메일 발송 시작: {}", email);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setTo(email);
            helper.setSubject("[영화 추천 서비스] 비밀번호 재설정 안내");
            
            String htmlContent = "<html><body>" +
                    "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;'>" +
                    "<h2 style='color: #333;'>비밀번호 재설정 안내</h2>" +
                    "<p>안녕하세요!</p>" +
                    "<p>비밀번호 재설정을 위한 링크입니다. 아래 버튼을 클릭하여 새 비밀번호를 설정해 주세요.</p>" +
                    "<div style='text-align: center; margin: 30px 0;'>" +
                    "<a href='" + resetLink + "' style='background-color: #667eea; color: white; padding: 12px 30px; text-decoration: none; border-radius: 5px; display: inline-block;'>비밀번호 재설정하기</a>" +
                    "</div>" +
                    "<p style='color: #666; font-size: 14px;'>위 버튼이 작동하지 않는 경우, 아래 링크를 복사하여 브라우저에 붙여넣기 해주세요:</p>" +
                    "<p style='word-break: break-all; color: #667eea;'>" + resetLink + "</p>" +
                    "<p style='color: #666; font-size: 14px;'>이 링크는 30분간만 유효합니다.</p>" +
                    "<p style='color: #666; font-size: 14px;'>본인이 요청하지 않은 경우 이 메일을 무시하세요.</p>" +
                    "<p>감사합니다.</p>" +
                    "</div></body></html>";
            
            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("비밀번호 재설정 메일 발송 완료: {}", email);
        } catch (MessagingException e) {
            log.error("비밀번호 재설정 메일 발송 실패: {}", email, e);
            throw new RuntimeException("비밀번호 재설정 메일 발송에 실패했습니다: " + e.getMessage(), e);
        }
    }
} 
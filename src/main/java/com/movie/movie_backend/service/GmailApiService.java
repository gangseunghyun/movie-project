package com.movie.movie_backend.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.io.ByteArrayOutputStream;
import java.util.Properties;
import java.util.Optional;

@Service
@Slf4j
public class GmailApiService {
    
    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String clientId;
    
    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String clientSecret;
    
    @Value("${spring.mail.username}")
    private String senderEmail;
    
    private final OAuth2TokenService oAuth2TokenService;
    
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String APPLICATION_NAME = "Movie Recommendation Service";
    
    public GmailApiService(OAuth2TokenService oAuth2TokenService) {
        this.oAuth2TokenService = oAuth2TokenService;
    }
    
    public void sendEmail(String to, String subject, String body) {
        try {
            log.info("Gmail API를 사용한 이메일 발송 시작: {}", to);
            
            // Gmail API 서비스 생성
            NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            
            // 액세스 토큰 가져오기
            String accessToken = getAccessToken();
            if (accessToken == null) {
                throw new RuntimeException("Gmail API 액세스 토큰을 찾을 수 없습니다.");
            }
            
            log.info("Gmail API 액세스 토큰 획득 완료");
            
            // OAuth2 인증 설정
            GoogleCredential credential = new GoogleCredential.Builder()
                    .setTransport(httpTransport)
                    .setJsonFactory(JSON_FACTORY)
                    .build()
                    .setAccessToken(accessToken);
            
            // Gmail API 서비스 생성
            Gmail service = new Gmail.Builder(httpTransport, JSON_FACTORY, credential)
                    .setApplicationName(APPLICATION_NAME)
                    .build();
            
            // 이메일 메시지 생성
            MimeMessage mimeMessage = createEmail(to, subject, body);
            Message message = createMessageWithEmail(mimeMessage);
            
            // 이메일 발송
            service.users().messages().send("me", message).execute();
            
            log.info("Gmail API를 사용한 이메일 발송 완료: {}", to);
            
        } catch (Exception e) {
            log.error("Gmail API를 사용한 이메일 발송 실패: {}", to, e);
            throw new RuntimeException("이메일 발송에 실패했습니다: " + e.getMessage(), e);
        }
    }
    
    private String getAccessToken() {
        try {
            // 1. 먼저 현재 로그인된 사용자의 토큰 확인
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof OAuth2User) {
                OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
                String email = oauth2User.getAttribute("email");
                if (email != null) {
                    Optional<String> accessTokenOpt = oAuth2TokenService.getAccessToken(email, "GOOGLE");
                    if (accessTokenOpt.isPresent()) {
                        log.info("로그인된 사용자의 OAuth2 토큰 사용: email={}", email);
                        return accessTokenOpt.get();
                    }
                }
            }
            
            // 2. 로그인된 사용자가 없으면 기본 Gmail 계정 사용
            log.info("로그인된 사용자가 없으므로 기본 Gmail 계정 사용");
            return getDefaultGmailAccessToken();
            
        } catch (Exception e) {
            log.error("액세스 토큰 획득 실패", e);
            return null;
        }
    }
    
    private String getDefaultGmailAccessToken() {
        try {
            log.info("기본 Gmail 계정 액세스 토큰 획득 시도");
            
            // 기본 Gmail 계정의 액세스 토큰을 가져오기
            Optional<String> accessTokenOpt = oAuth2TokenService.getAccessToken(senderEmail, "GOOGLE");
            if (accessTokenOpt.isPresent()) {
                log.info("기본 Gmail 계정 액세스 토큰 획득 완료");
                return accessTokenOpt.get();
            } else {
                log.warn("기본 Gmail 계정의 액세스 토큰이 없습니다. Gmail API 사용을 위해서는 먼저 Google 로그인이 필요합니다.");
                return null;
            }
            
        } catch (Exception e) {
            log.error("기본 Gmail 계정 액세스 토큰 획득 실패", e);
            return null;
        }
    }
    
    private MimeMessage createEmail(String to, String subject, String body) throws Exception {
        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);
        
        MimeMessage email = new MimeMessage(session);
        email.setFrom(new InternetAddress(senderEmail));
        email.addRecipient(jakarta.mail.Message.RecipientType.TO, new InternetAddress(to));
        email.setSubject(subject);
        email.setText(body);
        
        return email;
    }
    
    private Message createMessageWithEmail(MimeMessage email) throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        email.writeTo(buffer);
        byte[] bytes = buffer.toByteArray();
        String encodedEmail = java.util.Base64.getUrlEncoder().encodeToString(bytes);
        
        Message message = new Message();
        message.setRaw(encodedEmail);
        return message;
    }
} 
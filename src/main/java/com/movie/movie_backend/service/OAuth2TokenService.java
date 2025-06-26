package com.movie.movie_backend.service;

import com.movie.movie_backend.entity.OAuth2Token;
import com.movie.movie_backend.repository.OAuth2TokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OAuth2TokenService {
    
    private final OAuth2TokenRepository oAuth2TokenRepository;
    
    public void saveToken(String email, String provider, String accessToken, String refreshToken, int expiresInSeconds) {
        try {
            // 기존 토큰 삭제
            oAuth2TokenRepository.deleteByEmailAndProvider(email, provider);
            
            // 새 토큰 저장
            OAuth2Token token = OAuth2Token.builder()
                    .email(email)
                    .provider(provider)
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .expiresAt(LocalDateTime.now().plusSeconds(expiresInSeconds))
                    .build();
            
            oAuth2TokenRepository.save(token);
            log.info("OAuth2 토큰 저장 완료: email={}, provider={}", email, provider);
            
        } catch (Exception e) {
            log.error("OAuth2 토큰 저장 실패: email={}, provider={}", email, provider, e);
            throw new RuntimeException("토큰 저장에 실패했습니다: " + e.getMessage(), e);
        }
    }
    
    public Optional<String> getAccessToken(String email, String provider) {
        try {
            Optional<OAuth2Token> tokenOpt = oAuth2TokenRepository.findByEmailAndProvider(email, provider);
            
            if (tokenOpt.isPresent()) {
                OAuth2Token token = tokenOpt.get();
                
                if (token.isExpired()) {
                    log.warn("토큰이 만료되었습니다: email={}, provider={}", email, provider);
                    return Optional.empty();
                }
                
                log.info("액세스 토큰 조회 성공: email={}, provider={}", email, provider);
                return Optional.of(token.getAccessToken());
            } else {
                log.warn("토큰을 찾을 수 없습니다: email={}, provider={}", email, provider);
                return Optional.empty();
            }
            
        } catch (Exception e) {
            log.error("액세스 토큰 조회 실패: email={}, provider={}", email, provider, e);
            return Optional.empty();
        }
    }
    
    public void deleteToken(String email, String provider) {
        try {
            oAuth2TokenRepository.deleteByEmailAndProvider(email, provider);
            log.info("OAuth2 토큰 삭제 완료: email={}, provider={}", email, provider);
        } catch (Exception e) {
            log.error("OAuth2 토큰 삭제 실패: email={}, provider={}", email, provider, e);
        }
    }
} 
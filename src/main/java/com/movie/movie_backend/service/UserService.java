package com.movie.movie_backend.service;

import com.movie.movie_backend.dto.UserJoinRequestDto;
import com.movie.movie_backend.entity.User;
import com.movie.movie_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;
    
    public void join(UserJoinRequestDto requestDto) {
        // 이메일 인증 확인
        boolean isCodeValid = mailService.verifyAndRemoveCode(requestDto.getEmail(), requestDto.getVerificationCode());
        if (!isCodeValid) {
            throw new IllegalArgumentException("이메일 인증 코드가 올바르지 않습니다.");
        }

        // 비밀번호 확인 검증
        if (!requestDto.getPassword().equals(requestDto.getPasswordConfirm())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }
        
        // 아이디 중복 검사
        if (userRepository.existsByLoginId(requestDto.getLoginId())) {
            throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
        }
        
        // 이메일 중복 검사
        if (userRepository.existsByEmail(requestDto.getEmail())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }
        
        // 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(requestDto.getPassword());
        
        // User 엔티티 생성 (이메일 인증 완료 상태로)
        User user = User.builder()
                .loginId(requestDto.getLoginId())
                .password(encodedPassword)
                .email(requestDto.getEmail())
                .role("USER")
                .emailVerified(true) // 최종 가입 시 인증 상태를 true로 설정
                .build();
        
        // 저장
        userRepository.save(user);
    }
    
    @Transactional(readOnly = true)
    public boolean checkLoginIdDuplicate(String loginId) {
        return userRepository.existsByLoginId(loginId);
    }
    
    @Transactional(readOnly = true)
    public boolean checkEmailDuplicate(String email) {
        return userRepository.existsByEmail(email);
    }
} 
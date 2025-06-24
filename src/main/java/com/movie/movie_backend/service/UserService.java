package com.movie.movie_backend.service;

import com.movie.movie_backend.dto.UserJoinRequestDto;
import com.movie.movie_backend.entity.User;
import com.movie.movie_backend.entity.PasswordResetToken;
import com.movie.movie_backend.repository.UserRepository;
import com.movie.movie_backend.repository.PasswordResetTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    
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
                .nickname(requestDto.getNickname())
                .role("USER")
                .emailVerified(true) // 최종 가입 시 인증 상태를 true로 설정
                .socialJoinCompleted(true) // 반드시 true로!
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

    // 닉네임 중복 확인
    @Transactional(readOnly = true)
    public boolean checkNicknameDuplicate(String nickname) {
        return userRepository.existsByNickname(nickname);
    }

    // 닉네임 추천 (영화 관련 + 유명 영화 캐릭터)
    public List<String> recommendNicknames() {
        String[] movieAdjectives = {
            "시네마", "액션", "로맨스", "스릴러", "판타지", "애니메이션", "SF", "느와르", "뮤지컬", "명작", "클래식", "마블", "DC", "흥행", "레전드", "감독님", "배우", "평론가", "주인공", "히어로", "악당", "명탐정", "마법사", "OST", "시사회", "엔딩크레딧", "필름", "팝콘"
        };
        String[] movieCharacters = {
            "해리포터", "반지의제왕", "프로도", "간달프", "아라곤", "토르", "아이언맨", "캡틴아메리카", "배트맨", "조커", "슈퍼맨", "스파이더맨", "블랙위도우", "헐크", "닥터스트레인지", "엘사", "안나", "올라프", "인디아나존스", "터미네이터", "로키", "다스베이더", "요다", "루크", "한솔로", "에이리언", "고질라", "킹콩", "007", "제임스본드", "에단헌트", "존윅", "네모", "도리", "알라딘", "자스민", "심바", "무파사", "나탈리", "아멜리에"
        };
        String[] movieNouns = {"덕후", "매니아", "광", "러버", "마스터", "헌터", "킹", "여신", "짱", "고수", "초보", "감상러", "수집가", "추천러", "리뷰어", "팬", "주인", "감독", "배우", "평론가"};
        List<String> nicknames = new ArrayList<>();
        Random random = new Random();
        int tryCount = 0;
        while (nicknames.size() < 3 && tryCount < 30) {
            String first = random.nextBoolean() ? movieAdjectives[random.nextInt(movieAdjectives.length)] : movieCharacters[random.nextInt(movieCharacters.length)];
            String noun = movieNouns[random.nextInt(movieNouns.length)];
            String number = random.nextInt(10) < 3 ? String.valueOf(100 + random.nextInt(900)) : "";
            String nickname = first + noun + number;
            if (!checkNicknameDuplicate(nickname) && !nicknames.contains(nickname)) {
                nicknames.add(nickname);
            }
            tryCount++;
        }
        return nicknames;
    }

    @Transactional
    public void updateNicknameByEmail(String email, String nickname) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        user.setNickname(nickname);
        user.setSocialJoinCompleted(true);
        userRepository.save(user);
    }

    @Transactional
    public void updateNicknameByProviderAndProviderId(String provider, String providerId, String nickname) {
        System.out.println("[DEBUG] updateNicknameByProviderAndProviderId - provider: " + provider + ", providerId: " + providerId + ", nickname: " + nickname);
        User user = userRepository.findByProviderAndProviderId(provider, providerId)
                .orElse(null);
        if (user == null) {
            System.out.println("[DEBUG] User not found for provider: " + provider + ", providerId: " + providerId);
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다.");
        }
        System.out.println("[DEBUG] User found: id=" + user.getId() + ", email=" + user.getEmail());
        user.setNickname(nickname);
        user.setSocialJoinCompleted(true);
        userRepository.save(user);
        System.out.println("[DEBUG] Nickname updated successfully.");
    }

    // 비밀번호 재설정 토큰 생성 및 저장
    public PasswordResetToken createPasswordResetToken(String email) {
        // 기존 토큰 삭제(1인 1토큰 정책)
        passwordResetTokenRepository.deleteByEmail(email);
        String token = java.util.UUID.randomUUID().toString();
        PasswordResetToken resetToken = PasswordResetToken.create(token, email, 30); // 30분 유효
        return passwordResetTokenRepository.save(resetToken);
    }

    // 토큰 검증
    public PasswordResetToken validatePasswordResetToken(String token) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 토큰입니다."));
        if (resetToken.isExpired() || resetToken.isUsed()) {
            throw new IllegalArgumentException("만료되었거나 이미 사용된 토큰입니다.");
        }
        return resetToken;
    }

    // 비밀번호 변경 및 토큰 무효화
    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = validatePasswordResetToken(token);
        User user = userRepository.findByEmail(resetToken.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        user.setPassword(passwordEncoder.encode(newPassword));
        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);
        userRepository.save(user);
    }
} 
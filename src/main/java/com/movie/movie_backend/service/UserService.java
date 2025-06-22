package com.movie.movie_backend.service;

import com.movie.movie_backend.dto.UserJoinRequestDto;
import com.movie.movie_backend.entity.User;
import com.movie.movie_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service // 이 클래스가 서비스 계층의 컴포넌트임을 Spring에 알립니다.
@Transactional(readOnly = true) // 기본적으로 모든 메소드에 '읽기 전용' 트랜잭션을 적용합니다. (성능 최적화)
@RequiredArgsConstructor // final이 붙은 필드를 인자로 받는 생성자를 자동으로 만들어줍니다. (의존성 주입)
public class UserService {

    private final UserRepository userRepository; // 생성자를 통해 UserRepository 의존성을 주입받습니다.
    private final PasswordEncoder passwordEncoder; // SecurityConfig에서 Bean으로 등록한 PasswordEncoder를 주입받습니다.
    private final MailService mailService; // 이메일 서비스를 주입받습니다.

    /**
     * 회원가입 로직
     */
    @Transactional // 쓰기 작업이므로, 클래스 레벨의 readOnly 설정을 덮어씁니다.
    public void join(UserJoinRequestDto requestDto) {
        // 1. 아이디 중복 체크
        userRepository.findByLoginId(requestDto.getLoginId()).ifPresent(user -> {
            throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
        });

        // 2. 이메일 중복 체크
        userRepository.findByEmail(requestDto.getEmail()).ifPresent(user -> {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        });

        // 3. 이메일 인증 완료 여부 확인
        if (!mailService.isEmailVerified(requestDto.getEmail())) {
            throw new IllegalArgumentException("이메일 인증이 완료되지 않았습니다.");
        }

        // 4. 비밀번호 암호화
        String encryptedPassword = passwordEncoder.encode(requestDto.getPassword());

        // 5. DTO를 Entity로 변환하여 DB에 저장
        User user = requestDto.toEntity(encryptedPassword);
        userRepository.save(user);
    }

    /**
     * 아이디 중복 확인
     */
    public boolean isLoginIdAvailable(String loginId) {
        // 아이디 유효성 검사
        if (loginId == null || loginId.trim().isEmpty()) {
            throw new IllegalArgumentException("아이디를 입력해주세요.");
        }
        
        if (loginId.length() < 4 || loginId.length() > 20) {
            throw new IllegalArgumentException("아이디는 4자 이상 20자 이하로 입력해주세요.");
        }

        // 중복 확인
        return userRepository.findByLoginId(loginId).isEmpty();
    }

    // 앞으로 이곳에 회원가입, 회원 조회, 정보 수정 등
    // 실제 비즈니스 로직을 담은 메소드들을 만들어 나갈 겁니다.
    //
    // 예를 들면 이런 모습이 될 거예요.
    //
    // @Transactional // 데이터를 저장/수정/삭제하는 메소드에는 '쓰기 가능' 트랜잭션을 별도로 붙여줍니다.
    // public Long join(User user) {
    //     validateDuplicateUser(user); // 중복 회원 검증
    //     userRepository.save(user);
    //     return user.getId();
    // }
    //
    // private void validateDuplicateUser(User user) {
    //     // ... 중복 체크 로직 ...
    // }
} 
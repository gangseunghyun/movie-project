package com.movie.movie_backend.dto;

import com.movie.movie_backend.entity.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserJoinRequestDto {

    @NotBlank(message = "아이디는 필수 입력 항목입니다.")
    @Size(min = 4, max = 20, message = "아이디는 4자 이상 20자 이하로 입력해주세요.")
    private String loginId;

    @NotBlank(message = "비밀번호는 필수 입력 항목입니다.")
    @Size(min = 8, max = 20, message = "비밀번호는 8자 이상 20자 이하로 입력해주세요.")
    private String password;

    @NotBlank(message = "이메일은 필수 입력 항목입니다.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    private String email;

    // DTO를 Entity로 변환하는 메소드.
    // Service 단에서 이 메소드를 호출하여, 전달받은 DTO를 User 엔티티로 변환한 후 DB에 저장합니다.
    public User toEntity(String encryptedPassword) {
        return User.builder()
                .loginId(this.loginId)
                .password(encryptedPassword) // 비밀번호는 암호화해서 저장해야 합니다.
                .email(this.email)
                .role("USER") // 회원가입 시 기본 역할은 "USER"
                .provider("local") // 자체 로그인이므로 "local"
                .build();
    }
} 
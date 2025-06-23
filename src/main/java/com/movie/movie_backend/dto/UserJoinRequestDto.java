package com.movie.movie_backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserJoinRequestDto {
    
    @NotBlank(message = "아이디는 필수입니다.")
    @Size(min = 4, max = 20, message = "아이디는 4~20자 사이여야 합니다.")
    @Pattern(regexp = "^[a-zA-Z0-9]+$", message = "아이디는 영문자와 숫자만 사용 가능합니다.")
    private String loginId;
    
    @NotBlank(message = "비밀번호는 필수입니다.")
    @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*[0-9])(?=.*[!@#$%^&*])[a-zA-Z0-9!@#$%^&*]{8,20}$",
             message = "비밀번호는 8~20자의 영문, 숫자, 특수문자(!@#$%^&*)를 사용해야 합니다.")
    private String password;
    
    @NotBlank(message = "비밀번호 확인은 필수입니다.")
    private String passwordConfirm;
    
    @NotBlank(message = "이메일은 필수입니다.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    private String email;

    @NotBlank(message = "이메일 인증 코드를 입력해주세요.")
    private String verificationCode;

    @NotBlank(message = "닉네임은 필수입니다.")
    @Size(min = 2, max = 20, message = "닉네임은 2~20자 사이여야 합니다.")
    private String nickname;
} 
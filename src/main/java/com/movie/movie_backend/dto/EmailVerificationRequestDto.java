package com.movie.movie_backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EmailVerificationRequestDto {
    
    @NotBlank(message = "인증 코드는 필수입니다.")
    private String email;
    
    @NotBlank(message = "인증 코드는 필수입니다.")
    private String verificationCode;
} 
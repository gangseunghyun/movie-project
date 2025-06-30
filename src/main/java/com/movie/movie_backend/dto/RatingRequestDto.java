package com.movie.movie_backend.dto;

import lombok.*;
import jakarta.validation.constraints.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RatingRequestDto {
    @NotBlank(message = "영화 코드는 필수입니다.")
    private String movieCd;
    
    @NotNull(message = "별점은 필수입니다.")
    @DecimalMin(value = "0.5", message = "별점은 0.5점 이상이어야 합니다.")
    @DecimalMax(value = "5.0", message = "별점은 5점 이하여야 합니다.")
    private Double score;
} 
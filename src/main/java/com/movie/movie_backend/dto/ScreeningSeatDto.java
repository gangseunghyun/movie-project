package com.movie.movie_backend.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ScreeningSeatDto {
    private Long id;
    private Long seatId;
    private String seatNumber;
    private String status;

    public static ScreeningSeatDto fromEntity(com.movie.movie_backend.entity.ScreeningSeat s) {
        return ScreeningSeatDto.builder()
            .id(s.getId())
            .seatId(s.getSeat() != null ? s.getSeat().getId() : null)
            .seatNumber(s.getSeat() != null ? s.getSeat().getSeatNumber() : null)
            .status(s.getStatus() != null ? s.getStatus().name() : null)
            .build();
    }
} 
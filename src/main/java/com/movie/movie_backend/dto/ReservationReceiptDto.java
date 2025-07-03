package com.movie.movie_backend.dto;

import lombok.*;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ReservationReceiptDto {
    private Long reservationId;
    private String reservedAt;
    private String status;
    private int totalAmount;

    private ScreeningDto screening; // 상영정보(영화, 상영관, 시간 등)
    private CinemaDto cinema;       // 영화관 정보
    private TheaterDto theater;     // 상영관 정보
    private List<ScreeningSeatDto> seats; // 예매 좌석 정보

    private List<PaymentDto> payments; // 결제정보(영수증 등)
} 
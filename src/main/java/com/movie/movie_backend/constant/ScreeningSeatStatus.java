package com.movie.movie_backend.constant;

public enum ScreeningSeatStatus {
    AVAILABLE,           // 예매 가능
    RESERVED,            // 예매 완료
    LOCKED,              // 결제 진행 중(임시 홀드)
    CLOSED,              // 예매 마감
    UNAVAILABLE,         // 예매 불가
    COMPLETED            // 상영 완료
} 
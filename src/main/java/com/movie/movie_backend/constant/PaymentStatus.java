package com.movie.movie_backend.constant;

public enum PaymentStatus {
    PENDING("대기중"),
    SUCCESS("성공"),
    FAILED("실패"),
    CANCELLED("취소됨"),
    PAID("결제완료"),
    OTHER("기타");

    private final String displayName;

    PaymentStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
} 

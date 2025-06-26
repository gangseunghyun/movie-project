package com.movie.movie_backend.constant;

public enum ReservationStatus {
    CONFIRMED("예매완료"),
    CANCELLED("취소됨"),
    USED("사용완료"),
    EXPIRED("만료됨");

    private final String displayName;

    ReservationStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
} 

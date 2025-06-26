package com.movie.constant;

public enum ScreeningStatus {
    BOOKING_AVAILABLE("예매가능"),
    BOOKING_CLOSED("예매마감"),
    SCREENING_COMPLETED("상영완료"),
    CANCELLED("취소됨");

    private final String displayName;

    ScreeningStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
} 
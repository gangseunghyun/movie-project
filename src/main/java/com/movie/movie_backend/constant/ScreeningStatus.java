package com.movie.movie_backend.constant;

public enum ScreeningStatus {
    AVAILABLE("예매가능"),
    CLOSED("예매마감"),
    COMPLETED("상영완료"),
    CANCELLED("취소됨");

    private final String displayName;

    ScreeningStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
} 

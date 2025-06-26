package com.movie.constant;

public enum SeatType {
    NORMAL("일반"),
    COUPLE("커플"),
    DISABLED("장애인"),
    VIP("VIP"),
    PREMIUM("프리미엄");

    private final String displayName;

    SeatType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
} 
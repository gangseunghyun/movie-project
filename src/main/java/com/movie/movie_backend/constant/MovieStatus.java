package com.movie.constant;

public enum MovieStatus {
    COMING_SOON("개봉예정"),
    NOW_PLAYING("개봉중"),
    ENDED("상영종료");

    private final String displayName;

    MovieStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
} 
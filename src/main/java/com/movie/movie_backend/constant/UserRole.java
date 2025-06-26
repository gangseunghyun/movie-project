package com.movie.constant;

public enum UserRole {
    USER("일반 사용자"),
    ADMIN("관리자");

    private final String displayName;

    UserRole(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
} 
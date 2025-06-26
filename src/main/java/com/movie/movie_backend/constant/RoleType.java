package com.movie.movie_backend.constant;

public enum RoleType {
    LEAD("주연"),
    SUPPORTING("조연"),
    SPECIAL("특별출연"),
    CAMEO("카메오"),
    EXTRA("단역");

    private final String displayName;

    RoleType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
} 

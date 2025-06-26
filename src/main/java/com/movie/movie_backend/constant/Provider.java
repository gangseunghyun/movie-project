package com.movie.constant;

public enum Provider {
    LOCAL("로컬"),
    GOOGLE("구글"),
    KAKAO("카카오"),
    NAVER("네이버"),
    FACEBOOK("페이스북");

    private final String displayName;

    Provider(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
} 
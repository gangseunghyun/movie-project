package com.movie.movie_backend.constant;

public enum PaymentMethod {
    TOSS("토스"),
    KAKAO("카카오페이"),
    NAVER("네이버페이"),
    CREDIT_CARD("신용카드"),
    POINT("포인트"),
    OTHER("기타");

    private final String displayName;

    PaymentMethod(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
} 

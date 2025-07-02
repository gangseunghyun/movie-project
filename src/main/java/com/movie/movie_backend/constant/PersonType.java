package com.movie.movie_backend.constant;

public enum PersonType {
    ACTOR("배우"),
    DIRECTOR("감독");
    
    private final String displayName;
    
    PersonType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
} 
package com.movie.movie_backend.dto;

public class UserSearchResultDto {
    private Long userId;
    private String userName;
    private String nickname;

    public UserSearchResultDto(Long userId, String userName, String nickname) {
        this.userId = userId;
        this.userName = userName;
        this.nickname = nickname;
    }

    public Long getUserId() { return userId; }
    public String getUserName() { return userName; }
    public String getNickname() { return nickname; }

    public void setUserId(Long userId) { this.userId = userId; }
    public void setUserName(String userName) { this.userName = userName; }
    public void setNickname(String nickname) { this.nickname = nickname; }
} 

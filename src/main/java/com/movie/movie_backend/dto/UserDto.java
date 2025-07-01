package com.movie.movie_backend.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserDto {
    private Long id;
    private String loginId;
    private String email;
    private String nickname;
    private String role;
    private boolean emailVerified;
    private boolean socialJoinCompleted;

    public static UserDto fromEntity(com.movie.movie_backend.entity.User user) {
        return UserDto.builder()
            .id(user.getId())
            .loginId(user.getLoginId())
            .email(user.getEmail())
            .nickname(user.getNickname())
            .role(user.getRole() != null ? user.getRole().name() : null)
            .emailVerified(user.isEmailVerified())
            .socialJoinCompleted(user.isSocialJoinCompleted())
            .build();
    }
} 
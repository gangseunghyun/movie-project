package com.movie.movie_backend.entity;

import lombok.*;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import com.movie.movie_backend.constant.Provider;
import com.movie.movie_backend.constant.UserRole;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Table(name = "users")
public class User implements UserDetails {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 회원 고유 ID

    @Column(unique = true, nullable = false)
    private String loginId; // 로그인 ID (기존 username)
    
    @Column(nullable = true)
    private String password; // 비밀번호(암호화 필요) - 소셜 로그인 시 null 가능
    
    @Column(unique = true, nullable = false)
    private String email;    // 이메일 주소
    
    @Column(unique = true, nullable = true)
    private String nickname; // 닉네임 (표시용 이름)
    
    @Column(nullable = true)
    private String profileImageUrl; // 프로필 이미지 URL
    
    private boolean darkMode = false; // 기본값 false (라이트모드)

    @Enumerated(EnumType.STRING)
    private Provider provider = Provider.LOCAL; // 로그인 제공자 (기본값: 로컬)

    private String providerId; // 소셜 로그인 제공자의 고유 ID (예: 카카오 ID, 구글 ID 등)

    @Enumerated(EnumType.STRING)
    private UserRole role = UserRole.USER; // 사용자 권한 (기본값: 일반 사용자)
    
    @Column(nullable = false)
    private boolean emailVerified = false; // 이메일 인증 여부
    
    @Column(nullable = false)
    private boolean socialJoinCompleted = false; // 소셜 회원가입 완료 여부
    
    @Column(nullable = false)
    private LocalDateTime createdAt; // 생성일시
    
    private LocalDateTime updatedAt; // 수정일시

    // 기존 연관관계들
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Rating> ratings; // 사용자가 남긴 평점 목록

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Review> reviews; // 사용자가 작성한 리뷰 목록

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> comments; // 사용자가 작성한 댓글 목록

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Like> likes; // 사용자가 누른 좋아요 목록

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SearchHistory> searchHistories; // 사용자의 검색 기록

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MovieVisitLog> visitLogs; // 사용자가 클릭한 영화 방문 기록

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Reservation> reservations; // 사용자의 예매 목록

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Payment> payments; // 사용자의 결제 목록

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReviewLike> reviewLikes; // 사용자가 누른 리뷰 좋아요 목록

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CommentLike> commentLikes; // 사용자가 누른 댓글 좋아요 목록

    @ManyToMany
    @JoinTable(name = "user_preferred_tags",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "tag_id"))
    private List<Tag> preferredTags; // 사용자가 선호하는 태그 목록 (마이페이지 태그 설정)

    // JPA 생명주기 메서드
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // UserDetails 구현 (Spring Security)
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }
    
    @Override
    public String getUsername() {
        return nickname != null && !nickname.isBlank() ? nickname : loginId;
    }
    
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }
    
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }
    
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
    
    @Override
    public boolean isEnabled() {
        return emailVerified;
    }
    
    // 편의 메서드들
    public String getDisplayName() {
        return nickname != null && !nickname.isBlank() ? nickname : loginId;
    }
    
    public boolean isSocialUser() {
        return provider != Provider.LOCAL;
    }
    
    public boolean isLocalUser() {
        return provider == Provider.LOCAL;
    }
    
    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }
    
    public boolean isUser() {
        return role == UserRole.USER;
    }
} 

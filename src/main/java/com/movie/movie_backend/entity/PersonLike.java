package com.movie.movie_backend.entity;

import com.movie.movie_backend.constant.PersonType;
import lombok.*;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Table(name = "person_likes")
public class PersonLike {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 인물 좋아요 고유 ID

    private LocalDateTime createdAt; // 좋아요 누른 시간

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user; // 좋아요를 누른 사용자

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private Actor actor; // 좋아요가 달린 배우 (null 가능)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "director_id")
    private Director director; // 좋아요가 달린 감독 (null 가능)

    @Enumerated(EnumType.STRING)
    private PersonType personType; // ACTOR 또는 DIRECTOR

    // 배우 좋아요 생성자
    public static PersonLike createActorLike(User user, Actor actor) {
        return PersonLike.builder()
                .user(user)
                .actor(actor)
                .personType(PersonType.ACTOR)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // 감독 좋아요 생성자
    public static PersonLike createDirectorLike(User user, Director director) {
        return PersonLike.builder()
                .user(user)
                .director(director)
                .personType(PersonType.DIRECTOR)
                .createdAt(LocalDateTime.now())
                .build();
    }
} 
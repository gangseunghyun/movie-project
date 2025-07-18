package com.movie.movie_backend.entity;

import lombok.*;
import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.movie.movie_backend.constant.RoleType;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Table(name = "casts")
public class Cast {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_detail_id")
    @JsonIgnoreProperties("casts")
    private MovieDetail movieDetail;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    @JsonIgnoreProperties("casts")
    private Actor actor;

    @Enumerated(EnumType.STRING)
    private RoleType roleType; // 주연, 조연, 특별출연 등

    private String characterName; // 배우가 연기한 캐릭터 이름
    private Integer orderInCredits; // 크레딧 순서 (1, 2, 3...)
} 

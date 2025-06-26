package com.movie.movie_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(
    indexes = {
        @Index(name = "idx_user_id", columnList = "user_id"),
        @Index(name = "idx_searched_at", columnList = "searchedAt")
    }
    // uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "keyword"}) // 중복 방지 원할 때
)
public class SearchHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100)
    private String keyword; // 사용자가 검색한 키워드

    private LocalDateTime searchedAt; // 검색한 시간

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user; // 누가 검색했는지

    // private Boolean deleted = false; // soft delete 원할 때
} 
package com.movie.movie_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Table(name = "popular_keywords")
public class PopularKeyword {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String keyword;

    @Column(nullable = false)
    private Integer searchCount;

    @Column(nullable = false)
    private LocalDateTime aggregatedAt;

    // getter, setter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
    public Integer getSearchCount() { return searchCount; }
    public void setSearchCount(Integer searchCount) { this.searchCount = searchCount; }
    public LocalDateTime getAggregatedAt() { return aggregatedAt; }
    public void setAggregatedAt(LocalDateTime aggregatedAt) { this.aggregatedAt = aggregatedAt; }
} 
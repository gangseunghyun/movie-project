package com.movie.movie_backend.dto;

import java.time.LocalDateTime;

public class SearchHistoryDto {
    private Long id;
    private String keyword;
    private LocalDateTime searchedAt;

    public SearchHistoryDto(Long id, String keyword, LocalDateTime searchedAt) {
        this.id = id;
        this.keyword = keyword;
        this.searchedAt = searchedAt;
    }

    public Long getId() {
        return id;
    }

    public String getKeyword() {
        return keyword;
    }

    public LocalDateTime getSearchedAt() {
        return searchedAt;
    }
} 
package com.movie.movie_backend.dto;

public class PopularKeywordDto {
    private String keyword;
    private Long searchCount;

    public PopularKeywordDto(String keyword, Long searchCount) {
        this.keyword = keyword;
        this.searchCount = searchCount;
    }

    public String getKeyword() {
        return keyword;
    }

    public Long getSearchCount() {
        return searchCount;
    }
} 
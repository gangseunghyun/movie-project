package com.movie.movie_backend.dto;

public class SearchRequestDto {
    private String query;
    private String type; // "all", "movie", "user", "tag"
    private int page = 0;
    private int size = 10;

    public SearchRequestDto() {}

    public SearchRequestDto(String query, String type, int page, int size) {
        this.query = query;
        this.type = type;
        this.page = page;
        this.size = size;
    }

    public String getQuery() { return query; }
    public String getType() { return type; }
    public int getPage() { return page; }
    public int getSize() { return size; }

    public void setQuery(String query) { this.query = query; }
    public void setType(String type) { this.type = type; }
    public void setPage(int page) { this.page = page; }
    public void setSize(int size) { this.size = size; }
} 

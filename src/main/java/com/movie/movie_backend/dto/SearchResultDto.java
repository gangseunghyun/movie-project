package com.movie.movie_backend.dto;

import java.util.List;

public class SearchResultDto {
    private List<MovieSearchResultDto> movies;
    private List<UserSearchResultDto> users;
    private List<TagSearchResultDto> tags;
    private int totalPages;
    private long totalElements;
    private int currentPage;
    private int pageSize;

    public SearchResultDto(List<MovieSearchResultDto> movies, List<UserSearchResultDto> users, List<TagSearchResultDto> tags) {
        this.movies = movies;
        this.users = users;
        this.tags = tags;
    }

    public SearchResultDto(List<MovieSearchResultDto> movies, List<UserSearchResultDto> users, List<TagSearchResultDto> tags, 
                          int totalPages, long totalElements, int currentPage, int pageSize) {
        this.movies = movies;
        this.users = users;
        this.tags = tags;
        this.totalPages = totalPages;
        this.totalElements = totalElements;
        this.currentPage = currentPage;
        this.pageSize = pageSize;
    }

    public List<MovieSearchResultDto> getMovies() { return movies; }
    public List<UserSearchResultDto> getUsers() { return users; }
    public List<TagSearchResultDto> getTags() { return tags; }
    public int getTotalPages() { return totalPages; }
    public long getTotalElements() { return totalElements; }
    public int getCurrentPage() { return currentPage; }
    public int getPageSize() { return pageSize; }

    public void setMovies(List<MovieSearchResultDto> movies) { this.movies = movies; }
    public void setUsers(List<UserSearchResultDto> users) { this.users = users; }
    public void setTags(List<TagSearchResultDto> tags) { this.tags = tags; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }
    public void setTotalElements(long totalElements) { this.totalElements = totalElements; }
    public void setCurrentPage(int currentPage) { this.currentPage = currentPage; }
    public void setPageSize(int pageSize) { this.pageSize = pageSize; }
} 
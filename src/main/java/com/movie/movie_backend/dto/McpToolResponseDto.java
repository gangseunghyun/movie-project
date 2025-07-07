package com.movie.movie_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McpToolResponseDto {
    private String tool;
    private Object result;
    private boolean success;
    private String error;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MovieSearchResult {
        private List<MovieDetailDto> movies;
        private String genre;
        private int count;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MovieInfoResult {
        private MovieDetailDto movie;
        private String title;
        private String description;
    }
} 
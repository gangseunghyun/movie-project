package com.movie.movie_backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OllamaRequestDto {
    private String model;
    private String prompt;
    
    @JsonProperty("stream")
    private boolean stream = false;
    
    @JsonProperty("options")
    private OllamaOptions options;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OllamaOptions {
        private double temperature = 0.7;
        private int numPredict = 500;
    }
} 
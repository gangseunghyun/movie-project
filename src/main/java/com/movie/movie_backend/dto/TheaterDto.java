package com.movie.movie_backend.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class TheaterDto {
    private Long id;
    private String name;
    private int totalSeats;
    private Long cinemaId;
} 
package com.movie.movie_backend.dto;

import lombok.*;

import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class CinemaDto {
    private Long id;
    private String name;
    private String address;
    private String phoneNumber;
    private List<TheaterDto> theaters;
} 
package com.movie.movie_backend.mapper;

import com.movie.movie_backend.entity.MovieList;
import com.movie.movie_backend.dto.MovieListDto;
import com.movie.movie_backend.constant.MovieStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class MovieListMapper {

    public MovieListDto toDto(MovieList movieList) {
        return MovieListDto.builder()
                .movieCd(movieList.getMovieCd())
                .movieNm(movieList.getMovieNm())
                .movieNmEn(movieList.getMovieNmEn())
                .openDt(movieList.getOpenDt())
                .genreNm(movieList.getGenreNm())
                .nationNm(movieList.getNationNm())
                .watchGradeNm(movieList.getWatchGradeNm())
                .posterUrl(movieList.getPosterUrl())
                .status(movieList.getStatus())
                .build();
    }

    public MovieList toEntity(MovieListDto dto) {
        return MovieList.builder()
                .movieCd(dto.getMovieCd())
                .movieNm(dto.getMovieNm())
                .movieNmEn(dto.getMovieNmEn())
                .openDt(dto.getOpenDt())
                .genreNm(dto.getGenreNm())
                .nationNm(dto.getNationNm())
                .watchGradeNm(dto.getWatchGradeNm())
                .posterUrl(dto.getPosterUrl())
                .status(dto.getStatus())
                .build();
    }

    public List<MovieListDto> toDtoList(List<MovieList> movieLists) {
        return movieLists.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }
} 

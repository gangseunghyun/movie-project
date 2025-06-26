package com.movie.movie_backend.dto;

import java.util.List;

public class MovieSearchResultDto {
    private Long movieId;
    private String title;
    private String directorName;
    private List<String> tags;
    private List<String> actors;
    private String posterUrl;

    public MovieSearchResultDto(Long movieId, String title, String directorName, List<String> tags, List<String> actors, String posterUrl) {
        this.movieId = movieId;
        this.title = title;
        this.directorName = directorName;
        this.tags = tags;
        this.actors = actors;
        this.posterUrl = posterUrl;
    }

    public Long getMovieId() { return movieId; }
    public String getTitle() { return title; }
    public String getDirectorName() { return directorName; }
    public List<String> getTags() { return tags; }
    public List<String> getActors() { return actors; }
    public String getPosterUrl() { return posterUrl; }

    public void setMovieId(Long movieId) { this.movieId = movieId; }
    public void setTitle(String title) { this.title = title; }
    public void setDirectorName(String directorName) { this.directorName = directorName; }
    public void setTags(List<String> tags) { this.tags = tags; }
    public void setActors(List<String> actors) { this.actors = actors; }
    public void setPosterUrl(String posterUrl) { this.posterUrl = posterUrl; }
} 

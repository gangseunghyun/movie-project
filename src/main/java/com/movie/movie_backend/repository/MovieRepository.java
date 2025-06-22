package com.movie.movie_backend.repository;

import com.movie.movie_backend.entity.Movie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MovieRepository extends JpaRepository<Movie, Long> {
    
    List<Movie> findByGenre(String genre);
    
    List<Movie> findByTitleContainingIgnoreCase(String title);
    
    List<Movie> findByDirectorContainingIgnoreCase(String director);
    
    @Query("SELECT m FROM Movie m ORDER BY m.averageRating DESC, m.ratingCount DESC")
    List<Movie> findTopRatedMovies();
    
    @Query("SELECT m FROM Movie m ORDER BY m.viewCount DESC")
    List<Movie> findMostViewedMovies();
    
    @Query("SELECT m FROM Movie m WHERE m.genre = :genre ORDER BY m.averageRating DESC")
    List<Movie> findTopRatedMoviesByGenre(@Param("genre") String genre);
    
    @Query("SELECT DISTINCT m.genre FROM Movie m")
    List<String> findAllGenres();
} 
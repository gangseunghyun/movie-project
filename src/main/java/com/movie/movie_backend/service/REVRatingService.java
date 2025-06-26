package com.movie.movie_backend.service;

import com.movie.movie_backend.entity.Rating;
import com.movie.movie_backend.repository.REVRatingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class REVRatingService {
    private final REVRatingRepository ratingRepository;

    public Rating addRating(Rating rating) {
        return ratingRepository.save(rating);
    }

    public List<Rating> getRatingsByMovieDetail(String movieCd) {
        return ratingRepository.findByMovieDetailMovieCd(movieCd);
    }

    public long getRatingCountByMovieDetail(String movieCd) {
        return ratingRepository.countByMovieDetailMovieCd(movieCd);
    }
} 

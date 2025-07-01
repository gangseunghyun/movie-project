package com.movie.movie_backend.repository;

import com.movie.movie_backend.entity.Screening;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScreeningRepository extends JpaRepository<Screening, Long> {
    List<Screening> findByTheaterId(Long theaterId);
    List<Screening> findByTheaterIdAndMovieDetail_MovieCd(Long theaterId, String movieCd);
    List<Screening> findByTheaterIdAndMovieDetailId(Long theaterId, Long movieDetailId);
} 
package com.movie.movie_backend.repository;

import com.movie.movie_backend.entity.ScreeningSeat;
import com.movie.movie_backend.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ScreeningSeatRepository extends JpaRepository<ScreeningSeat, Long> {
    List<ScreeningSeat> findByScreeningId(Long screeningId);
    Optional<ScreeningSeat> findByScreeningIdAndSeatId(Long screeningId, Long seatId);
    List<ScreeningSeat> findByReservation(Reservation reservation);
    List<ScreeningSeat> findByStatus(com.movie.movie_backend.constant.ScreeningSeatStatus status);
} 
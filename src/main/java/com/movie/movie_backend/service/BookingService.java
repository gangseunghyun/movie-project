package com.movie.movie_backend.service;

import com.movie.movie_backend.entity.Cinema;
import com.movie.movie_backend.entity.Theater;
import com.movie.movie_backend.entity.Screening;
import com.movie.movie_backend.entity.ScreeningSeat;
import com.movie.movie_backend.entity.Seat;
import com.movie.movie_backend.entity.Reservation;
import com.movie.movie_backend.entity.User;
import com.movie.movie_backend.repository.CinemaRepository;
import com.movie.movie_backend.repository.TheaterRepository;
import com.movie.movie_backend.repository.ScreeningRepository;
import com.movie.movie_backend.repository.ScreeningSeatRepository;
import com.movie.movie_backend.repository.SeatRepository;
import com.movie.movie_backend.repository.ReservationRepository;
import com.movie.movie_backend.repository.USRUserRepository;
import com.movie.movie_backend.constant.ScreeningSeatStatus;
import com.movie.movie_backend.constant.ReservationStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class BookingService {

    @Autowired
    private CinemaRepository cinemaRepository;

    @Autowired
    private TheaterRepository theaterRepository;

    @Autowired
    private ScreeningRepository screeningRepository;

    @Autowired
    private ScreeningSeatRepository screeningSeatRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private USRUserRepository userRepository;

    // 모든 영화관 조회
    public List<Cinema> getAllCinemas() {
        return cinemaRepository.findAll();
    }

    // 특정 영화관의 상영관 목록 조회
    public List<Theater> getTheatersByCinema(Long cinemaId) {
        return theaterRepository.findByCinemaId(cinemaId);
    }

    // 특정 상영관의 상영 스케줄 조회 (영화별 필터링)
    public List<Screening> getScreeningsByTheater(Long theaterId, String movieId) {
        if (movieId != null && !movieId.isEmpty()) {
            return screeningRepository.findByTheaterIdAndMovieId(theaterId, movieId);
        } else {
            return screeningRepository.findByTheaterId(theaterId);
        }
    }

    // 특정 상영의 좌석 정보 조회
    public List<ScreeningSeat> getSeatsByScreening(Long screeningId) {
        return screeningSeatRepository.findByScreeningId(screeningId);
    }

    // 예매 처리
    @Transactional
    public boolean createBooking(String movieId, Long screeningId, List<Long> seatIds, Integer totalPrice) {
        try {
            // 현재 로그인한 사용자 정보 가져오기 (세션에서)
            // TODO: 실제 인증된 사용자 정보를 가져오는 로직으로 수정 필요
            Optional<User> currentUser = userRepository.findById(1L); // 임시로 ID 1 사용
            if (currentUser.isEmpty()) {
                return false;
            }

            // 상영 정보 확인
            Optional<Screening> screening = screeningRepository.findById(screeningId);
            if (screening.isEmpty()) {
                return false;
            }

            // 좌석 상태 확인 및 업데이트
            for (Long seatId : seatIds) {
                Optional<ScreeningSeat> screeningSeat = screeningSeatRepository.findByScreeningIdAndSeatId(screeningId, seatId);
                if (screeningSeat.isEmpty() || screeningSeat.get().getStatus() != ScreeningSeatStatus.BOOKING_AVAILABLE) {
                    return false; // 좌석이 이미 예매되었거나 사용할 수 없음
                }
            }

            // 예매 정보 생성
            Reservation reservation = new Reservation();
            reservation.setUser(currentUser.get());
            reservation.setScreening(screening.get());
            reservation.setTotalPrice(totalPrice);
            reservation.setReservationStatus(ReservationStatus.CONFIRMED);
            reservation.setReservationDate(LocalDateTime.now());

            Reservation savedReservation = reservationRepository.save(reservation);

            // 좌석 상태 업데이트
            for (Long seatId : seatIds) {
                Optional<ScreeningSeat> screeningSeat = screeningSeatRepository.findByScreeningIdAndSeatId(screeningId, seatId);
                if (screeningSeat.isPresent()) {
                    ScreeningSeat seat = screeningSeat.get();
                    seat.setStatus(ScreeningSeatStatus.BOOKED);
                    seat.setReservation(savedReservation);
                    screeningSeatRepository.save(seat);
                }
            }

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
} 
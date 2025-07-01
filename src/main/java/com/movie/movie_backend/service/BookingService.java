package com.movie.movie_backend.service;

import com.movie.movie_backend.dto.CinemaDto;
import com.movie.movie_backend.dto.TheaterDto;
import com.movie.movie_backend.dto.ScreeningDto;
import com.movie.movie_backend.dto.ScreeningSeatDto;
import com.movie.movie_backend.entity.Cinema;
import com.movie.movie_backend.entity.Theater;
import com.movie.movie_backend.entity.Screening;
import com.movie.movie_backend.entity.ScreeningSeat;
import com.movie.movie_backend.entity.Seat;
import com.movie.movie_backend.entity.Reservation;
import com.movie.movie_backend.entity.User;
import com.movie.movie_backend.entity.MovieDetail;
import com.movie.movie_backend.repository.CinemaRepository;
import com.movie.movie_backend.repository.TheaterRepository;
import com.movie.movie_backend.repository.ScreeningRepository;
import com.movie.movie_backend.repository.ScreeningSeatRepository;
import com.movie.movie_backend.repository.SeatRepository;
import com.movie.movie_backend.repository.ReservationRepository;
import com.movie.movie_backend.repository.USRUserRepository;
import com.movie.movie_backend.repository.PRDMovieRepository;
import com.movie.movie_backend.constant.ScreeningSeatStatus;
import com.movie.movie_backend.constant.ReservationStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;
import java.util.stream.Collectors;

@Service
public class BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingService.class);

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

    @Autowired
    private PRDMovieRepository prdMovieRepository;

    // 모든 영화관 조회
    public List<CinemaDto> getAllCinemas() {
        List<Cinema> cinemas = cinemaRepository.findAll();
        return cinemas.stream()
                .map(cinema -> {
                    List<TheaterDto> theaterDtos = cinema.getTheaters().stream()
                            .map(theater -> new TheaterDto(
                                    theater.getId(),
                                    theater.getName(),
                                    theater.getTotalSeats(),
                                    theater.getCinema().getId()
                            ))
                            .collect(Collectors.toList());
                    
                    return new CinemaDto(
                            cinema.getId(),
                            cinema.getName(),
                            cinema.getAddress(),
                            cinema.getPhoneNumber(),
                            theaterDtos
                    );
                })
                .collect(Collectors.toList());
    }

    // 특정 영화관의 상영관 목록 조회
    public List<TheaterDto> getTheatersByCinema(Long cinemaId) {
        List<Theater> theaters = theaterRepository.findByCinemaId(cinemaId);
        return theaters.stream()
                .map(theater -> new TheaterDto(
                        theater.getId(),
                        theater.getName(),
                        theater.getTotalSeats(),
                        theater.getCinema().getId()
                ))
                .collect(Collectors.toList());
    }

    // 특정 상영관의 상영 스케줄 조회 (영화별 필터링)
    public List<ScreeningDto> getScreeningsByTheater(Long theaterId, String movieId) {
        List<Screening> screenings;
        if (movieId != null && !movieId.isEmpty()) {
            // movieId는 movieCd임. movie_detail_id로 변환 필요
            Optional<MovieDetail> movieDetailOpt = prdMovieRepository.findByMovieCd(movieId);
            if (movieDetailOpt.isEmpty()) return List.of();
            Long movieDetailId = movieDetailOpt.get().getId();
            screenings = screeningRepository.findByTheaterIdAndMovieDetailId(theaterId, movieDetailId);
        } else {
            screenings = screeningRepository.findByTheaterId(theaterId);
        }
        
        return screenings.stream()
                .map(ScreeningDto::fromEntity)
                .collect(Collectors.toList());
    }

    // 특정 상영의 좌석 정보 조회
    public List<ScreeningSeatDto> getSeatsByScreening(Long screeningId) {
        List<ScreeningSeat> seats = screeningSeatRepository.findByScreeningId(screeningId);
        return seats.stream()
                .map(ScreeningSeatDto::fromEntity)
                .collect(Collectors.toList());
    }

    // 예매 처리
    @Transactional
    public boolean createBooking(String movieId, Long screeningId, List<Long> seatIds, Integer totalPrice) {
        try {
            // 1. 입력값 검증
            if (seatIds == null || seatIds.isEmpty() || seatIds.size() > 2) {
                log.warn("Invalid seatIds: " + seatIds);
                return false;
            }

            // 2. 사용자, 상영 정보 조회
            Optional<User> currentUser = userRepository.findById(1L); // 실제 서비스에서는 인증 정보 사용
            if (currentUser.isEmpty()) {
                log.warn("User not found");
                return false;
            }
            Optional<Screening> screening = screeningRepository.findById(screeningId);
            if (screening.isEmpty()) {
                log.warn("Screening not found: " + screeningId);
                return false;
            }

            // 3. 좌석 상태 확인 및 예약 가능 여부 체크
            List<ScreeningSeat> seatsToReserve = new java.util.ArrayList<>();
            for (Long seatId : seatIds) {
                Optional<ScreeningSeat> screeningSeatOpt = screeningSeatRepository.findByScreeningIdAndSeatId(screeningId, seatId);
                if (screeningSeatOpt.isEmpty()) {
                    log.warn("ScreeningSeat not found: screeningId=" + screeningId + ", seatId=" + seatId);
                    return false;
                }
                ScreeningSeat screeningSeat = screeningSeatOpt.get();
                if (screeningSeat.getStatus() != ScreeningSeatStatus.AVAILABLE &&
                    screeningSeat.getStatus() != ScreeningSeatStatus.LOCKED) {
                    log.warn("Seat not available: screeningId=" + screeningId + ", seatId=" + seatId + ", status=" + screeningSeat.getStatus());
                    return false;
                }
                seatsToReserve.add(screeningSeat);
            }

            // 4. Reservation 생성 및 저장
            Reservation reservation = new Reservation();
            reservation.setUser(currentUser.get());
            reservation.setScreening(screening.get());
            reservation.setTotalAmount(java.math.BigDecimal.valueOf(totalPrice));
            reservation.setStatus(ReservationStatus.CONFIRMED);
            reservation.setReservedAt(java.time.LocalDateTime.now());
            Reservation savedReservation = reservationRepository.save(reservation);

            // 5. 좌석 상태 RESERVED로 변경 및 Reservation 연결
            for (ScreeningSeat seat : seatsToReserve) {
                seat.setStatus(ScreeningSeatStatus.RESERVED);
                seat.setReservation(savedReservation);
                screeningSeatRepository.save(seat);
            }

            // 6. 성공 반환
            return true;
        } catch (Exception e) {
            log.error("Booking failed", e);
            return false;
        }
    }

    // 결제 전 임시 좌석 홀드(LOCKED) 처리
    @Transactional
    public boolean lockSeatsForPayment(Long screeningId, List<Long> seatIds) {
        try {
            List<ScreeningSeat> seatsToLock = new java.util.ArrayList<>();
            for (Long seatId : seatIds) {
                Optional<ScreeningSeat> screeningSeatOpt = screeningSeatRepository.findByScreeningIdAndSeatId(screeningId, seatId);
                if (screeningSeatOpt.isEmpty()) {
                    log.warn("ScreeningSeat not found: screeningId=" + screeningId + ", seatId=" + seatId);
                    return false;
                }
                ScreeningSeat screeningSeat = screeningSeatOpt.get();
                if (screeningSeat.getStatus() != ScreeningSeatStatus.AVAILABLE) {
                    log.warn("Seat not available for lock: screeningId=" + screeningId + ", seatId=" + seatId + ", status=" + screeningSeat.getStatus());
                    return false;
                }
                seatsToLock.add(screeningSeat);
            }
            // 모두 AVAILABLE이면 LOCKED로 변경
            for (ScreeningSeat seat : seatsToLock) {
                seat.setStatus(ScreeningSeatStatus.LOCKED);
                screeningSeatRepository.save(seat);
            }
            return true;
        } catch (Exception e) {
            log.error("Locking seats for payment failed", e);
            return false;
        }
    }

    // 좌석 홀드 취소 (LOCKED -> AVAILABLE)
    @Transactional
    public boolean unlockSeats(Long screeningId, List<Long> seatIds) {
        try {
            for (Long seatId : seatIds) {
                Optional<ScreeningSeat> screeningSeatOpt = screeningSeatRepository.findByScreeningIdAndSeatId(screeningId, seatId);
                if (screeningSeatOpt.isEmpty()) {
                    log.warn("ScreeningSeat not found: screeningId=" + screeningId + ", seatId=" + seatId);
                    return false;
                }
                ScreeningSeat screeningSeat = screeningSeatOpt.get();
                if (screeningSeat.getStatus() == ScreeningSeatStatus.LOCKED) {
                    screeningSeat.setStatus(ScreeningSeatStatus.AVAILABLE);
                    screeningSeatRepository.save(screeningSeat);
                } else {
                    log.warn("Seat is not LOCKED: screeningId=" + screeningId + ", seatId=" + seatId + ", status=" + screeningSeat.getStatus());
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            log.error("Unlocking seats failed", e);
            return false;
        }
    }
} 
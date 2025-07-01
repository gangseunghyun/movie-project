package com.movie.movie_backend.controller;

import com.movie.movie_backend.entity.Cinema;
import com.movie.movie_backend.entity.Theater;
import com.movie.movie_backend.entity.Screening;
import com.movie.movie_backend.entity.ScreeningSeat;
import com.movie.movie_backend.service.BookingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class BookingController {

    @Autowired
    private BookingService bookingService;

    // 영화관 목록 조회
    @GetMapping("/cinemas")
    public ResponseEntity<List<Cinema>> getCinemas() {
        try {
            List<Cinema> cinemas = bookingService.getAllCinemas();
            return ResponseEntity.ok(cinemas);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // 특정 영화관의 상영관 목록 조회
    @GetMapping("/cinemas/{cinemaId}/theaters")
    public ResponseEntity<List<Theater>> getTheatersByCinema(@PathVariable Long cinemaId) {
        try {
            List<Theater> theaters = bookingService.getTheatersByCinema(cinemaId);
            return ResponseEntity.ok(theaters);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // 특정 상영관의 상영 스케줄 조회 (영화별 필터링)
    @GetMapping("/theaters/{theaterId}/screenings")
    public ResponseEntity<List<Screening>> getScreeningsByTheater(
            @PathVariable Long theaterId,
            @RequestParam(required = false) String movieId) {
        try {
            List<Screening> screenings = bookingService.getScreeningsByTheater(theaterId, movieId);
            return ResponseEntity.ok(screenings);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // 특정 상영의 좌석 정보 조회
    @GetMapping("/screenings/{screeningId}/seats")
    public ResponseEntity<List<ScreeningSeat>> getSeatsByScreening(@PathVariable Long screeningId) {
        try {
            List<ScreeningSeat> seats = bookingService.getSeatsByScreening(screeningId);
            return ResponseEntity.ok(seats);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // 예매 처리
    @PostMapping("/bookings")
    public ResponseEntity<Map<String, Object>> createBooking(@RequestBody Map<String, Object> bookingRequest) {
        try {
            String movieId = (String) bookingRequest.get("movieId");
            Long screeningId = Long.valueOf(bookingRequest.get("screeningId").toString());
            @SuppressWarnings("unchecked")
            List<Long> seatIds = (List<Long>) bookingRequest.get("seatIds");
            Integer totalPrice = (Integer) bookingRequest.get("totalPrice");

            boolean success = bookingService.createBooking(movieId, screeningId, seatIds, totalPrice);
            
            if (success) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "예매가 완료되었습니다."
                ));
            } else {
                return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "예매 처리 중 오류가 발생했습니다."
                ));
            }
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", "예매 처리 중 오류가 발생했습니다: " + e.getMessage()
            ));
        }
    }
} 
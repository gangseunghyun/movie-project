package com.movie.movie_backend.controller;

import com.movie.movie_backend.dto.CinemaDto;
import com.movie.movie_backend.dto.TheaterDto;
import com.movie.movie_backend.entity.Screening;
import com.movie.movie_backend.entity.ScreeningSeat;
import com.movie.movie_backend.service.BookingService;
import com.movie.movie_backend.dto.ScreeningDto;
import com.movie.movie_backend.dto.ScreeningSeatDto;
import com.movie.movie_backend.entity.Payment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class BookingController {

    @Autowired
    private BookingService bookingService;

    // 영화관 목록 조회
    @GetMapping("/cinemas")
    public ResponseEntity<List<CinemaDto>> getCinemas() {
        try {
            List<CinemaDto> cinemas = bookingService.getAllCinemas();
            return ResponseEntity.ok(cinemas);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // 특정 영화관의 상영관 목록 조회
    @GetMapping("/cinemas/{cinemaId}/theaters")
    public ResponseEntity<List<TheaterDto>> getTheatersByCinema(@PathVariable Long cinemaId) {
        try {
            List<TheaterDto> theaters = bookingService.getTheatersByCinema(cinemaId);
            return ResponseEntity.ok(theaters);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // 특정 상영관의 상영 스케줄 조회 (영화별 필터링)
    @GetMapping("/theaters/{theaterId}/screenings")
    public ResponseEntity<List<ScreeningDto>> getScreeningsByTheater(
            @PathVariable Long theaterId,
            @RequestParam(required = false) String movieId) {
        try {
            List<ScreeningDto> dtos = bookingService.getScreeningsByTheater(theaterId, movieId);
            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // 특정 상영의 좌석 정보 조회
    @GetMapping("/screenings/{screeningId}/seats")
    public ResponseEntity<List<ScreeningSeatDto>> getSeatsByScreening(@PathVariable Long screeningId) {
        try {
            List<ScreeningSeatDto> dtos = bookingService.getSeatsByScreening(screeningId);
            return ResponseEntity.ok(dtos);
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
            List<Object> seatIdsRaw = (List<Object>) bookingRequest.get("seatIds");
            List<Long> seatIds = seatIdsRaw.stream()
                .map(obj -> Long.valueOf(obj.toString()))
                .collect(java.util.stream.Collectors.toList());
            Integer totalPrice = (Integer) bookingRequest.get("totalPrice");

            boolean success = bookingService.createBooking(movieId, screeningId, seatIds, totalPrice);
            Long reservationId = bookingService.getLastReservationIdForUser(1L); // 실제 서비스에서는 인증 정보 사용
            if (success) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "예매가 완료되었습니다.",
                    "reservationId", reservationId
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

    @PostMapping("/bookings/lock-seats")
    public ResponseEntity<Map<String, Object>> lockSeatsForPayment(@RequestBody Map<String, Object> request) {
        try {
            Long screeningId = Long.valueOf(request.get("screeningId").toString());
            @SuppressWarnings("unchecked")
            List<Object> seatIdsRaw = (List<Object>) request.get("seatIds");
            List<Long> seatIds = seatIdsRaw.stream()
                .map(obj -> Long.valueOf(obj.toString()))
                .collect(java.util.stream.Collectors.toList());
            boolean success = bookingService.lockSeatsForPayment(screeningId, seatIds);
            if (success) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "좌석이 임시 홀드(LOCKED)되었습니다."
                ));
            } else {
                return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "좌석 임시 홀드에 실패했습니다. 이미 예매 중이거나 사용할 수 없는 좌석이 있습니다."
                ));
            }
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", "좌석 임시 홀드 처리 중 오류가 발생했습니다: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/bookings/unlock-seats")
    public ResponseEntity<Map<String, Object>> unlockSeats(@RequestBody Map<String, Object> request) {
        try {
            Long screeningId = Long.valueOf(request.get("screeningId").toString());
            @SuppressWarnings("unchecked")
            List<Object> seatIdsRaw = (List<Object>) request.get("seatIds");
            List<Long> seatIds = seatIdsRaw.stream()
                .map(obj -> Long.valueOf(obj.toString()))
                .collect(java.util.stream.Collectors.toList());
            boolean success = bookingService.unlockSeats(screeningId, seatIds);
            if (success) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "좌석 홀드가 취소되었습니다."
                ));
            } else {
                return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "좌석 홀드 취소에 실패했습니다."
                ));
            }
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", "좌석 홀드 취소 처리 중 오류가 발생했습니다: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/payments/complete")
    public ResponseEntity<?> completePayment(@RequestBody Map<String, Object> req) {
        String impUid = (String) req.get("imp_uid");
        String merchantUid = (String) req.get("merchant_uid");
        Long userId = req.get("userId") != null ? Long.valueOf(req.get("userId").toString()) : null;
        Long reservationId = req.get("reservationId") != null ? Long.valueOf(req.get("reservationId").toString()) : null;
        Payment payment = bookingService.completePayment(impUid, merchantUid, userId, reservationId);
        return ResponseEntity.ok(Map.of("success", true, "paymentId", payment.getId()));
    }

    @PostMapping("/payment/webhook")
    public ResponseEntity<?> handleIamportWebhook(@RequestBody Map<String, Object> payload) {
        String impUid = (String) payload.get("imp_uid");
        String merchantUid = (String) payload.get("merchant_uid");
        bookingService.completePayment(impUid, merchantUid, null, null);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/payments/cancel")
    public ResponseEntity<?> cancelPayment(@RequestBody Map<String, Object> req) {
        String impUid = String.valueOf(req.get("imp_uid"));
        String reason = req.get("reason") != null ? String.valueOf(req.get("reason")) : null;
        boolean result = bookingService.cancelPayment(impUid, reason);
        if (result) {
            return ResponseEntity.ok(Map.of("success", true, "message", "결제취소(환불) 성공"));
        } else {
            return ResponseEntity.ok(Map.of("success", false, "message", "결제취소(환불) 실패"));
        }
    }
} 
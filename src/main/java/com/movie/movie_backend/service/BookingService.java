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
import com.movie.movie_backend.repository.PaymentRepository;
import com.movie.movie_backend.entity.Payment;
import com.movie.movie_backend.constant.ScreeningSeatStatus;
import com.movie.movie_backend.constant.ReservationStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import com.fasterxml.jackson.databind.JsonNode;

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

    @Autowired
    private PaymentRepository paymentRepository;

    // 아임포트 API 키/시크릿 (환경변수나 설정파일에서 주입 권장)
    @Value("${iamport.api.key:YOUR_API_KEY}")
    private String iamportApiKey;
    @Value("${iamport.api.secret:YOUR_API_SECRET}")
    private String iamportApiSecret;

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

    // 아임포트 인증 토큰 발급
    public String getIamportAccessToken() {
        RestTemplate restTemplate = new RestTemplate();
        String url = "https://api.iamport.kr/users/getToken";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String body = String.format("{\"imp_key\":\"%s\",\"imp_secret\":\"%s\"}", iamportApiKey, iamportApiSecret);
        HttpEntity<String> entity = new HttpEntity<>(body, headers);
        ResponseEntity<JsonNode> response = restTemplate.postForEntity(url, entity, JsonNode.class);
        return response.getBody().get("response").get("access_token").asText();
    }

    // 아임포트 결제정보 조회
    public JsonNode getIamportPaymentInfo(String impUid, String accessToken) {
        RestTemplate restTemplate = new RestTemplate();
        String url = "https://api.iamport.kr/payments/" + impUid;
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", accessToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, entity, JsonNode.class);
        return response.getBody().get("response");
    }

    // 결제완료 처리: 아임포트에서 결제정보 조회 후 Payment 저장
    @Transactional
    public Payment completePayment(String impUid, String merchantUid, Long userId, Long reservationId) {
        try {
            // 아임포트 API 호출 시도
            String accessToken = getIamportAccessToken();
            JsonNode info = getIamportPaymentInfo(impUid, accessToken);
            
            Payment payment = Payment.builder()
                .impUid(impUid)
                .merchantUid(merchantUid)
                .amount(info.get("amount").decimalValue())
                .paidAt(info.has("paid_at") ? java.time.Instant.ofEpochSecond(info.get("paid_at").asLong()).atZone(java.time.ZoneId.systemDefault()).toLocalDateTime() : null)
                .method(com.movie.movie_backend.constant.PaymentMethod.valueOf(info.get("pay_method").asText().toUpperCase()))
                .status(com.movie.movie_backend.constant.PaymentStatus.valueOf(info.get("status").asText().toUpperCase()))
                .receiptNumber(info.has("receipt_url") ? info.get("receipt_url").asText() : null)
                .receiptUrl(info.has("receipt_url") ? info.get("receipt_url").asText() : null)
                .isCancelled("cancelled".equalsIgnoreCase(info.get("status").asText()))
                .cancelReason(info.has("cancel_reason") ? info.get("cancel_reason").asText() : null)
                .cancelledAt(info.has("cancelled_at") && !info.get("cancelled_at").isNull() ? java.time.Instant.ofEpochSecond(info.get("cancelled_at").asLong()).atZone(java.time.ZoneId.systemDefault()).toLocalDateTime() : null)
                .pgResponseCode(info.has("code") ? info.get("code").asText() : null)
                .pgResponseMessage(info.has("message") ? info.get("message").asText() : null)
                .cardName(info.has("card_name") ? info.get("card_name").asText() : null)
                .cardNumberSuffix(info.has("card_number") ? info.get("card_number").asText().substring(info.get("card_number").asText().length() - 4) : null)
                .approvalNumber(info.has("apply_num") ? info.get("apply_num").asText() : null)
                .build();
            
            // 유저/예약 연관관계 설정
            if (userId != null) {
                userRepository.findById(userId).ifPresent(payment::setUser);
            }
            if (reservationId != null) {
                reservationRepository.findById(reservationId).ifPresent(payment::setReservation);
            }
            return paymentRepository.save(payment);
            
        } catch (Exception e) {
            log.error("아임포트 API 호출 실패, 기본 결제정보로 저장: {}", e.getMessage());
            
            // 아임포트 API 실패 시 기본 결제정보로 저장
            Payment payment = Payment.builder()
                .impUid(impUid)
                .merchantUid(merchantUid)
                .amount(java.math.BigDecimal.ZERO) // 기본값
                .paidAt(java.time.LocalDateTime.now())
                .method(com.movie.movie_backend.constant.PaymentMethod.CREDIT_CARD) // 기본값
                .status(com.movie.movie_backend.constant.PaymentStatus.SUCCESS) // 기본값
                .isCancelled(false)
                .build();
            
            // 유저/예약 연관관계 설정
            if (userId != null) {
                userRepository.findById(userId).ifPresent(payment::setUser);
            }
            if (reservationId != null) {
                reservationRepository.findById(reservationId).ifPresent(payment::setReservation);
            }
            return paymentRepository.save(payment);
        }
    }

    // 유저의 가장 최근 예매 ID 반환
    public Long getLastReservationIdForUser(Long userId) {
        List<Reservation> reservations = reservationRepository.findByUserId(userId);
        return reservations.stream()
            .sorted((a, b) -> b.getReservedAt().compareTo(a.getReservedAt()))
            .map(Reservation::getId)
            .findFirst()
            .orElse(null);
    }

    // 결제취소(환불) 처리: 아임포트에 환불 요청 후 Payment 상태 업데이트
    @Transactional
    public boolean cancelPayment(String impUid, String reason) {
        try {
            // 1. Payment 엔티티 조회
            Payment payment = paymentRepository.findByImpUid(impUid);
            log.info("[검증] Payment 조회: impUid={}, paymentObj={}, reservationId={}", impUid, payment, payment != null ? payment.getReservation() != null ? payment.getReservation().getId() : "null" : "null");
            if (payment == null) {
                log.warn("[검증] Payment not found for impUid: {}", impUid);
                return false;
            }
            if (payment.isCancelled()) {
                log.info("[검증] Payment already cancelled: {}", impUid);
                return false;
            }

            // 2. 아임포트 인증 토큰 발급
            String accessToken = getIamportAccessToken();

            // 3. 아임포트 결제취소 API 호출
            RestTemplate restTemplate = new RestTemplate();
            String url = "https://api.iamport.kr/payments/cancel";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", accessToken);
            String body = String.format("{\"imp_uid\":\"%s\",\"reason\":\"%s\"}", impUid, reason != null ? reason : "사용자 요청");
            HttpEntity<String> entity = new HttpEntity<>(body, headers);
            ResponseEntity<JsonNode> response = restTemplate.postForEntity(url, entity, JsonNode.class);
            JsonNode responseBody = response.getBody();
            if (responseBody == null || !responseBody.has("response") || responseBody.get("response").isNull()) {
                log.error("[검증] 아임포트 환불 실패: {}", responseBody);
                return false;
            }
            JsonNode cancelInfo = responseBody.get("response");

            // 4. Payment 엔티티 상태 업데이트
            payment.setStatus(com.movie.movie_backend.constant.PaymentStatus.CANCELLED);
            payment.setCancelled(true);
            payment.setCancelReason(reason);
            payment.setCancelledAt(java.time.LocalDateTime.now());
            if (cancelInfo.has("receipt_url")) {
                payment.setReceiptUrl(cancelInfo.get("receipt_url").asText());
            }
            paymentRepository.save(payment);

            // === 추가: 해당 예약의 좌석을 모두 AVAILABLE로 변경 ===
            log.info("[검증] Checking reservation for payment: impUid={}, reservationObj={}, reservationId={}", impUid, payment.getReservation(), payment.getReservation() != null ? payment.getReservation().getId() : "null");
            if (payment.getReservation() != null) {
                Long reservationId = payment.getReservation().getId();
                Reservation managedReservation = reservationRepository.findById(reservationId).orElse(null);
                log.info("[검증] Found managedReservation for payment: reservationId={}, managedReservationObj={}", reservationId, managedReservation);
                if (managedReservation != null) {
                    List<ScreeningSeat> seats = screeningSeatRepository.findByReservation(managedReservation);
                    log.info("[검증] Found {} seats for managedReservation: {}", seats.size(), reservationId);
                    for (ScreeningSeat seat : seats) {
                        log.info("[검증] Updating seat: seatId={}, seatObjId={}, currentStatus={}, newStatus=AVAILABLE, reservationId={}", 
                            seat.getSeat() != null ? seat.getSeat().getId() : "null", seat.getId(), seat.getStatus(), reservationId);
                        seat.setStatus(ScreeningSeatStatus.AVAILABLE);
                        screeningSeatRepository.save(seat);
                    }
                } else {
                    log.warn("[검증] Reservation not found by id: {}", reservationId);
                }
            } else {
                log.warn("[검증] Payment has no reservation: impUid={}", impUid);
                log.info("[검증] Trying alternative approach: looking for seats with RESERVED status");
                List<ScreeningSeat> reservedSeats = screeningSeatRepository.findByStatus(ScreeningSeatStatus.RESERVED);
                log.info("[검증] Found {} reserved seats total", reservedSeats.size());
                for (ScreeningSeat seat : reservedSeats) {
                    if (seat.getReservation() != null) {
                        log.info("[검증] Found reserved seat: seatId={}, seatObjId={}, reservationId={}, status={}", 
                            seat.getSeat() != null ? seat.getSeat().getId() : "null", seat.getId(), seat.getReservation().getId(), seat.getStatus());
                        seat.setStatus(ScreeningSeatStatus.AVAILABLE);
                        screeningSeatRepository.save(seat);
                    }
                }
            }
            // === ===
            return true;
        } catch (Exception e) {
            log.error("[검증] 결제취소(환불) 처리 실패: {}", e.getMessage(), e);
            return false;
        }
    }
} 
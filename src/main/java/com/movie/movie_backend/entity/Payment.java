package com.movie.movie_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import com.movie.movie_backend.constant.PaymentMethod;
import com.movie.movie_backend.constant.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 결제 고유 ID

    private BigDecimal amount; // 결제 금액

    private LocalDateTime paidAt; // 결제 시간

    @Enumerated(EnumType.STRING)
    private PaymentMethod method; // 결제 수단 (토스, 카카오, 네이버, 신용카드)

    @Enumerated(EnumType.STRING)
    private PaymentStatus status; // 결제 상태 (성공, 실패, 대기, 취소)

    private String receiptNumber; // 결제 영수증 번호

    private String receiptUrl; // PG사 영수증 URL

    private String impUid; // 아임포트 결제 고유번호 등

    private String merchantUid; // 가맹점 고유 주문번호

    // ✅ 결제 취소 관련 필드 추가
    private boolean isCancelled = false; // 결제 취소 여부

    private String cancelReason; // 취소 사유

    private LocalDateTime cancelledAt; // 취소 시간

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user; // 결제한 사용자

    @ManyToOne
    @JoinColumn(name = "reservation_id")
    private Reservation reservation; // 이 결제가 연결된 예매 정보

    // --- 추가 추천 필드 (선택적 사용) ---
    private String pgResponseCode; // PG사 응답코드 (실패/취소 상세 사유 등)
    private String pgResponseMessage; // PG사 응답메시지
    private String cardName; // 카드사명 (카드결제시)
    private String cardNumberSuffix; // 카드번호 끝 4자리 (카드결제시)
    private String approvalNumber; // 결제 승인번호 (카드결제시)
}

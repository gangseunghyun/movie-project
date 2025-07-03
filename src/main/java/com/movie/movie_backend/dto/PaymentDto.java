package com.movie.movie_backend.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PaymentDto {
    private Long id;
    private int amount;
    private String method;
    private String status;
    private String paidAt;
    private String receiptUrl;
    private boolean cancelled;
    private String cancelReason;
    private String cancelledAt;
    private String impUid;
    private String merchantUid;
    private String receiptNumber;
    private String cardName;
    private String cardNumberSuffix;
    private String approvalNumber;
    private String userName;
    private String pgResponseCode;
    private String pgResponseMessage;
} 
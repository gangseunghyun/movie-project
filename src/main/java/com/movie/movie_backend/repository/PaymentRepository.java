package com.movie.movie_backend.repository;

import com.movie.movie_backend.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Payment findByImpUid(String impUid);
    Payment findByMerchantUid(String merchantUid);
} 
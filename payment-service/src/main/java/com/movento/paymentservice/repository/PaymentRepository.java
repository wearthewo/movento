package com.movento.paymentservice.repository;

import com.movento.paymentservice.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByPaymentIntentId(String paymentIntentId);
    List<Payment> findByUserId(Long userId);
    boolean existsByPaymentIntentId(String paymentIntentId);
}

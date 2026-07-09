package com.codequests.checkout.payment.repository;

import com.codequests.checkout.payment.domain.Payment;
import com.codequests.checkout.payment.domain.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    @EntityGraph(attributePaths = "order")
    Optional<Payment> findFirstByOrderIdAndStatus(Long orderId, PaymentStatus status);

}


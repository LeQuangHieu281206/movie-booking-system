package com.ttcsn.payment.repository;

import com.ttcsn.payment.entity.Payment;
import com.ttcsn.payment.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findFirstByBookingIdAndStatusOrderByPaymentIdDesc(Long bookingId, PaymentStatus status);

    boolean existsByProviderAndProviderTxnId(String provider, String providerTxnId);

    boolean existsByBookingIdAndStatus(Long bookingId, PaymentStatus status);

    List<Payment> findByBookingIdOrderByPaymentIdDesc(Long bookingId);
}

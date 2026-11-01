package com.ttcsn.payment.repository;

import com.ttcsn.payment.entity.Payment;
import com.ttcsn.payment.entity.PaymentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Payment> findFirstByBookingIdAndStatusOrderByPaymentIdDesc(Long bookingId, PaymentStatus status);

    boolean existsByProviderAndProviderTxnId(String provider, String providerTxnId);

    List<Payment> findByBookingIdOrderByPaymentIdDesc(Long bookingId);

    /**
     * Ghi nhận giao dịch SUCCESS theo kiểu atomic. Nếu webhook trùng provider_txn_id
     * đang được xử lý đồng thời, chỉ một transaction insert được và được phép publish event.
     */
    @Modifying
    @Query(value = """
            INSERT INTO payments
                (booking_id, amount, currency, status, provider, provider_txn_id,
                 reference_code, raw_payload, paid_at, created_at, updated_at)
            VALUES
                (:bookingId, :amount, 'VND', 'SUCCESS', 'SEPAY', :providerTxnId,
                 :referenceCode, CAST(:rawPayload AS jsonb), :paidAt, now(), now())
            ON CONFLICT (provider, provider_txn_id) DO NOTHING
            """, nativeQuery = true)
    int insertSuccessIfAbsent(@Param("bookingId") Long bookingId,
                              @Param("amount") BigDecimal amount,
                              @Param("providerTxnId") String providerTxnId,
                              @Param("referenceCode") String referenceCode,
                              @Param("rawPayload") String rawPayload,
                              @Param("paidAt") OffsetDateTime paidAt);

    @Modifying
    @Query(value = """
            INSERT INTO payments
                (booking_id, amount, currency, status, provider, provider_txn_id,
                 reference_code, raw_payload, failure_reason, created_at, updated_at)
            VALUES
                (:bookingId, :amount, 'VND', 'FAILED', 'SEPAY', :providerTxnId,
                 :referenceCode, CAST(:rawPayload AS jsonb), :failureReason, now(), now())
            ON CONFLICT (provider, provider_txn_id) DO NOTHING
            """, nativeQuery = true)
    int insertFailedIfAbsent(@Param("bookingId") Long bookingId,
                             @Param("amount") BigDecimal amount,
                             @Param("providerTxnId") String providerTxnId,
                             @Param("referenceCode") String referenceCode,
                             @Param("rawPayload") String rawPayload,
                             @Param("failureReason") String failureReason);
}
